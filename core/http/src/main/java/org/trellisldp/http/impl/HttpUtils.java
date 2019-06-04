/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.http.impl;

import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.core.Response.Status.PRECONDITION_FAILED;
import static javax.ws.rs.core.Response.notModified;
import static javax.ws.rs.core.Response.status;
import static org.apache.commons.codec.digest.DigestUtils.md5Hex;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.TrellisUtils.getInstance;
import static org.trellisldp.http.core.HttpConstants.DEFAULT_REPRESENTATION;
import static org.trellisldp.http.core.HttpConstants.PRECONDITION_REQUIRED;
import static org.trellisldp.vocabulary.JSONLD.compacted;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.NotAcceptableException;
import javax.ws.rs.RedirectionException;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.slf4j.Logger;
import org.trellisldp.api.IOService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Http Utility functions.
 *
 * @author acoburn
 */
public final class HttpUtils {

    private static final Logger LOGGER = getLogger(HttpUtils.class);
    private static final RDF rdf = getInstance();
    private static final Set<String> ignoredPreferences;

    static {
        final Set<String> ignore = new HashSet<>();
        ignore.add(Trellis.PreferUserManaged.getIRIString());
        ignore.add(Trellis.PreferServerManaged.getIRIString());
        ignoredPreferences = unmodifiableSet(ignore);
    }

    /**
     * Get all of the LDP resource (super) types for the given LDP interaction model.
     *
     * @param ixnModel the interaction model
     * @return a stream of types
     */
    public static Stream<IRI> ldpResourceTypes(final IRI ixnModel) {
        final Stream.Builder<IRI> supertypes = Stream.builder();
        if (nonNull(ixnModel)) {
            LOGGER.debug("Finding types that subsume {}", ixnModel.getIRIString());
            supertypes.accept(ixnModel);
            final IRI superClass = LDP.getSuperclassOf(ixnModel);
            LOGGER.debug("... including {}", superClass);
            ldpResourceTypes(superClass).forEach(supertypes::accept);
        }
        return supertypes.build();
    }

    /**
     * Build a hash value suitable for generating an ETag.
     * @param identifier the resource identifier
     * @param modified the last modified value
     * @param prefer a prefer header, may be null
     * @return a corresponding hash value
     */
    public static String buildEtagHash(final String identifier, final Instant modified, final Prefer prefer) {
        final String sep = ".";
        final String hash = nonNull(prefer) ? prefer.getInclude().hashCode() + sep + prefer.getOmit().hashCode() : "";
        return md5Hex(modified.toEpochMilli() + sep + modified.getNano() + sep + hash + sep + identifier);
    }

    /**
     * Create a filter based on a Prefer header.
     *
     * @param prefer the Prefer header
     * @param isAcl whether the request is for an ACL resource
     * @return a suitable predicate for filtering a stream of quads
     */
    public static Predicate<Quad> filterWithPrefer(final Prefer prefer, final boolean isAcl) {
        final Set<String> include = new HashSet<>(DEFAULT_REPRESENTATION);
        ofNullable(prefer).ifPresent(p -> {
            if (p.getInclude().contains(LDP.PreferMinimalContainer.getIRIString())) {
                include.remove(LDP.PreferContainment.getIRIString());
                include.remove(LDP.PreferMembership.getIRIString());
            }
            if (p.getOmit().contains(LDP.PreferMinimalContainer.getIRIString())) {
                include.remove(Trellis.PreferUserManaged.getIRIString());
            }
            p.getOmit().forEach(include::remove);
            p.getInclude().stream().filter(iri -> !ignoredPreferences.contains(iri)).forEach(include::add);
        });
        return quad -> quad.getGraphName().filter(IRI.class::isInstance).map(IRI.class::cast)
            .filter(iri -> isAcl || !PreferAccessControl.equals(iri))
            .map(IRI::getIRIString).filter(include::contains).isPresent();
    }

    /**
     * Create a Linked Data Fragments filter.
     *
     * @param subject the LDF subject
     * @param predicate the LDF predicate
     * @param object the LDF object
     * @return a filtering predicate
     */
    public static Predicate<Quad> filterWithLDF(final String subject, final String predicate,
            final String object) {
        return quad -> !(notCompareWithString(quad.getSubject(), subject)
                    || notCompareWithString(quad.getPredicate(), predicate)
                    || notCompareWithString(quad.getObject(), object));
    }

    private static boolean notCompareWithString(final RDFTerm term, final String str) {
        return nonNull(str) && !str.isEmpty() && (term instanceof IRI && !((IRI) term).getIRIString().equals(str)
                    || term instanceof Literal && !((Literal) term).getLexicalForm().equals(str));
    }

    /**
     * Convert triples from a skolemized form to an externa form.
     *
     * @param svc the resourceService
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Triple, Triple> unskolemizeTriples(final ResourceService svc, final String baseUrl) {
        return triple -> rdf.createTriple((BlankNodeOrIRI) svc.toExternal(svc.unskolemize(triple.getSubject()),
                    baseUrl), triple.getPredicate(), svc.toExternal(svc.unskolemize(triple.getObject()), baseUrl));
    }

    /**
     * Convert triples from an external form to a skolemized form.
     *
     * @param svc the resourceService
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Triple, Triple> skolemizeTriples(final ResourceService svc, final String baseUrl) {
        return triple -> rdf.createTriple((BlankNodeOrIRI) svc.toInternal(svc.skolemize(triple.getSubject()), baseUrl),
                triple.getPredicate(), svc.toInternal(svc.skolemize(triple.getObject()), baseUrl));
    }

    /**
     * Convert quads from a skolemized form to an external form.
     *
     * @param svc the resource service
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Quad, Quad> unskolemizeQuads(final ResourceService svc, final String baseUrl) {
        return quad -> rdf.createQuad(quad.getGraphName().orElse(PreferUserManaged),
                    (BlankNodeOrIRI) svc.toExternal(svc.unskolemize(quad.getSubject()), baseUrl),
                    quad.getPredicate(), svc.toExternal(svc.unskolemize(quad.getObject()), baseUrl));
    }

    /**
     * Convert quads from an external form to a skolemized form.
     *
     * @param svc the resource service
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Quad, Quad> skolemizeQuads(final ResourceService svc, final String baseUrl) {
        return quad -> rdf.createQuad(quad.getGraphName().orElse(PreferUserManaged),
                (BlankNodeOrIRI) svc.toInternal(svc.skolemize(quad.getSubject()), baseUrl), quad.getPredicate(),
                svc.toInternal(svc.skolemize(quad.getObject()), baseUrl));
    }

    /**
     * Given a list of acceptable media types, get an RDF syntax.
     *
     * @param ioService the I/O service
     * @param acceptableTypes the types from HTTP headers
     * @param mimeType an additional "default" mimeType to match
     * @return an RDFSyntax or null if there was an error
     */
    public static Optional<RDFSyntax> getSyntax(final IOService ioService, final List<MediaType> acceptableTypes,
            final Optional<String> mimeType) {
        if (acceptableTypes.isEmpty()) {
            return mimeType.isPresent() ? empty() : of(TURTLE);
        }
        final Optional<MediaType> mt = mimeType.map(MediaType::valueOf);
        for (final MediaType type : acceptableTypes) {
            if (mt.filter(type::isCompatible).isPresent()) {
                return empty();
            }
            final Optional<RDFSyntax> syntax = ioService.supportedReadSyntaxes().stream()
                .filter(s -> MediaType.valueOf(s.mediaType()).isCompatible(type)).findFirst();
            if (syntax.isPresent()) {
                return syntax;
            }
        }
        LOGGER.debug("Valid syntax not found among {} or {}", acceptableTypes, mimeType);
        throw new NotAcceptableException();
    }

    /**
     * Close an input stream in an async chain.
     * @param input the input stream
     * @return a bifunction that closes the stream
     */
    public static BiConsumer<Object, Throwable> closeInputStreamAsync(final InputStream input) {
        return (val, err) -> {
            try {
                input.close();
            } catch (final IOException ex) {
                throw new UncheckedIOException("Error closing input stream", ex);
            }
        };
    }

    /**
     * Given a list of acceptable media types and an RDF syntax, get the relevant profile data, if
     * relevant.
     *
     * @param acceptableTypes the types from HTTP headers
     * @param syntax an RDF syntax
     * @return a profile IRI if relevant
     */
    public static IRI getProfile(final List<MediaType> acceptableTypes, final RDFSyntax syntax) {
        for (final MediaType type : acceptableTypes) {
            if (RDFSyntax.byMediaType(type.toString()).filter(isEqual(syntax)).isPresent() &&
                    type.getParameters().containsKey("profile")) {
                return rdf.createIRI(type.getParameters().get("profile").split(" ")[0].trim());
            }
        }
        return null;
    }

    /**
     * Get a default profile IRI from the syntax and/or identifier.
     *
     * @param syntax the RDF syntax
     * @param identifier the resource identifier
     * @param defaultJsonLdProfile a user-supplied default
     * @return a profile IRI usable by the output streamer
     */
    public static IRI getDefaultProfile(final RDFSyntax syntax, final String identifier,
            final String defaultJsonLdProfile) {
        return getDefaultProfile(syntax, rdf.createIRI(identifier), defaultJsonLdProfile);
    }

    /**
     * Get a default profile IRI from the syntax and/or identifier.
     *
     * @param syntax the RDF syntax
     * @param identifier the resource identifier
     * @param defaultJsonLdProfile a user-supplied default
     * @return a profile IRI usable by the output streamer
     */
    public static IRI getDefaultProfile(final RDFSyntax syntax, final IRI identifier,
            final String defaultJsonLdProfile) {
        return RDFA.equals(syntax) ? identifier
            : ofNullable(defaultJsonLdProfile).map(rdf::createIRI).orElse(compacted);
    }

    /**
     * Check whether an LDP type is a sort of container.
     * @param ldpType the LDP type to test
     * @return true if it is a type of LDP container
     */
    public static boolean isContainer(final IRI ldpType) {
        return LDP.Container.equals(ldpType) || LDP.BasicContainer.equals(ldpType)
            || LDP.DirectContainer.equals(ldpType) || LDP.IndirectContainer.equals(ldpType);
    }

    /**
     * Check for a conditional operation.
     * @param method the HTTP method
     * @param ifModifiedSince the If-Modified-Since header
     * @param modified the resource modification date
     */
    public static void checkIfModifiedSince(final String method, final String ifModifiedSince,
            final Instant modified) {
        if (isGetOrHead(method)) {
            parseDate(ifModifiedSince).filter(d -> d.isAfter(modified.truncatedTo(SECONDS))).ifPresent(date -> {
                throw new RedirectionException(notModified().build());
            });
        }
    }

    /**
     * Check for a conditional operation.
     * @param ifUnmodifiedSince the If-Unmodified-Since header
     * @param modified the resource modification date
     */
    public static void checkIfUnmodifiedSince(final String ifUnmodifiedSince, final Instant modified) {
        parseDate(ifUnmodifiedSince).filter(modified.truncatedTo(SECONDS)::isAfter).ifPresent(date -> {
            throw new ClientErrorException(status(PRECONDITION_FAILED).build());
        });
    }

    /**
     * Check for a conditional operation.
     * @param ifMatch the If-Match header
     * @param etag the resource etag
     */
    public static void checkIfMatch(final String ifMatch, final EntityTag etag) {
        if (isNull(ifMatch)) {
            return;
        }
        final Set<String> items = stream(ifMatch.split(",")).map(String::trim).collect(toSet());
        if (items.contains("*")) {
            return;
        }
        try {
            if (etag.isWeak() || items.stream().map(EntityTag::valueOf).noneMatch(isEqual(etag))) {
                    throw new ClientErrorException(status(PRECONDITION_FAILED).build());
            }
        } catch (final IllegalArgumentException ex) {
            throw new BadRequestException(ex);
        }
    }

    /**
     * Check for a conditional operation.
     * @param method the HTTP method
     * @param ifNoneMatch the If-None-Match header
     * @param etag the resource etag
     */
    public static void checkIfNoneMatch(final String method, final String ifNoneMatch, final EntityTag etag) {
        if (isNull(ifNoneMatch)) {
            return;
        }

        final Set<String> items = stream(ifNoneMatch.split(",")).map(String::trim).collect(toSet());
        if (isGetOrHead(method)) {
            if ("*".equals(ifNoneMatch) || items.stream().map(EntityTag::valueOf)
                    .anyMatch(e -> e.equals(etag) || e.equals(new EntityTag(etag.getValue(), !etag.isWeak())))) {
                throw new RedirectionException(notModified().build());
            }
        } else {
            if ("*".equals(ifNoneMatch) || items.stream().map(EntityTag::valueOf).anyMatch(isEqual(etag))) {
                throw new ClientErrorException(status(PRECONDITION_FAILED).build());
            }
        }
     }

    private static boolean isGetOrHead(final String method) {
        return "GET".equals(method) || "HEAD".equals(method);
    }

    private static Optional<Instant> parseDate(final String date) {
        return ofNullable(date).map(String::trim).flatMap(d -> {
            try {
                return of(parse(d, RFC_1123_DATE_TIME));
            } catch (final DateTimeException ex) {
                LOGGER.debug("Ignoring invalid date ({}): {}", date, ex.getMessage());
            }
            return empty();
        }).map(ZonedDateTime::toInstant);
    }

    /**
     * Check whether conditional requests are required.
     * @param required whether conditional requests are required
     * @param ifMatch the If-Match header
     * @param ifUnmodifiedSince the If-Unmodified-Since header
     */
    public static void checkRequiredPreconditions(final boolean required, final String ifMatch,
            final String ifUnmodifiedSince) {
        if (required && isNull(ifMatch) && isNull(ifUnmodifiedSince)) {
            throw new ClientErrorException(status(PRECONDITION_REQUIRED).build());
        }
    }

    private HttpUtils() {
        // prevent instantiation
    }
}
