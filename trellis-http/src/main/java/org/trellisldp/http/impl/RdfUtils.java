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

import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.concat;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA_HTML;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.http.domain.HttpConstants.DEFAULT_REPRESENTATION;
import static org.trellisldp.http.domain.RdfMediaType.MEDIA_TYPES;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.vocabulary.JSONLD.expanded;
import static org.trellisldp.vocabulary.Trellis.DeletedResource;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.NotAcceptableException;
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
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.domain.Prefer;
import org.trellisldp.vocabulary.LDP;

/**
 * RDF Utility functions
 *
 * @author acoburn
 */
public final class RdfUtils {

    private static final Logger LOGGER = getLogger(RdfUtils.class);

    private static final RDF rdf = getInstance();

    /**
     * A mapping of LDP types to their supertype
     */
    public static final Map<IRI, IRI> superClassOf;

    static {
        final Map<IRI, IRI> data = new HashMap<>();
        data.put(LDP.NonRDFSource, LDP.Resource);
        data.put(LDP.RDFSource, LDP.Resource);
        data.put(LDP.Container, LDP.RDFSource);
        data.put(LDP.BasicContainer, LDP.Container);
        data.put(LDP.DirectContainer, LDP.Container);
        data.put(LDP.IndirectContainer, LDP.Container);
        superClassOf = unmodifiableMap(data);
    }

    /**
     * Get all of the LDP resource (super) types for the given LDP interaction model
     * @param interactionModel the interaction model
     * @return a stream of types
     */
    public static Stream<IRI> ldpResourceTypes(final IRI interactionModel) {
        return Stream.of(interactionModel).filter(type -> superClassOf.containsKey(type) || LDP.Resource.equals(type))
            .flatMap(type -> concat(ldpResourceTypes(superClassOf.get(type)), Stream.of(type)));
    }

    /**
     * Create a filter based on a Prefer header
     * @param prefer the Prefer header
     * @return a suitable predicate for filtering a stream of quads
     */
    public static Predicate<Quad> filterWithPrefer(final Prefer prefer) {
        final Set<String> include = new HashSet<>(DEFAULT_REPRESENTATION);
        ofNullable(prefer).ifPresent(p -> {
            p.getOmit().forEach(include::remove);
            p.getInclude().forEach(include::add);
        });
        return quad -> quad.getGraphName().filter(x -> x instanceof IRI).map(x -> (IRI) x)
            .map(IRI::getIRIString).filter(include::contains).isPresent();
    }

    /**
     * Create a Linked Data Fragments filter.
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

    private static Boolean notCompareWithString(final RDFTerm term, final String str) {
        return nonNull(str) && !str.isEmpty() && (term instanceof IRI && !((IRI) term).getIRIString().equals(str)
                    || term instanceof Literal && !((Literal) term).getLexicalForm().equals(str));
    }

    /**
     * Convert triples from a skolemized form to an externa form
     * @param svc the resourceService
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Triple, Triple> unskolemizeTriples(final ResourceService svc, final String baseUrl) {
        return triple -> rdf.createTriple((BlankNodeOrIRI) svc.toExternal(svc.unskolemize(triple.getSubject()),
                    baseUrl), triple.getPredicate(), svc.toExternal(svc.unskolemize(triple.getObject()), baseUrl));
    }

    /**
     * Convert triples from an external form to a skolemized form
     * @param svc the resourceService
     * @param baseUrl the base URL
     * @return a mapping function
     */
    public static Function<Triple, Triple> skolemizeTriples(final ResourceService svc, final String baseUrl) {
        return triple -> rdf.createTriple((BlankNodeOrIRI) svc.toInternal(svc.skolemize(triple.getSubject()), baseUrl),
                triple.getPredicate(), svc.toInternal(svc.skolemize(triple.getObject()), baseUrl));
    }

    /**
     * Convert quads from a skolemized form to an external form
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
     * Convert quads from an external form to a skolemized form
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
     * @param acceptableTypes the types from HTTP headers
     * @param mimeType an additional "default" mimeType to match
     * @return an RDFSyntax
     */
    public static Optional<RDFSyntax> getSyntax(final List<MediaType> acceptableTypes,
            final Optional<String> mimeType) {
        if (acceptableTypes.isEmpty()) {
            // TODO -- JDK9 refactor with Optional::or
            if (mimeType.isPresent()) {
                return empty();
            }
            return of(TURTLE);
        }
        final Optional<MediaType> mt = mimeType.map(MediaType::valueOf);
        for (final MediaType type : acceptableTypes) {
            if (mt.filter(type::isCompatible).isPresent()) {
                return empty();
            }
            final Optional<RDFSyntax> syntax = MEDIA_TYPES.stream().filter(type::isCompatible)
                .findFirst().map(MediaType::toString).flatMap(RDFSyntax::byMediaType);
            if (syntax.isPresent()) {
                return syntax;
            }
        }
        LOGGER.debug("Valid syntax not found among {} or {}", acceptableTypes, mimeType);
        throw new NotAcceptableException();
    }

    /**
     * Given a list of acceptable media types and an RDF syntax, get the relevant profile data, if
     * relevant
     * @param acceptableTypes the types from HTTP headers
     * @param syntax an RDF syntax
     * @return a profile IRI if relevant
     */
    public static IRI getProfile(final List<MediaType> acceptableTypes, final RDFSyntax syntax) {
        for (final MediaType type : acceptableTypes) {
            if (RDFSyntax.byMediaType(type.toString()).filter(syntax::equals).isPresent() &&
                    type.getParameters().containsKey("profile")) {
                return rdf.createIRI(type.getParameters().get("profile").split(" ")[0].trim());
            }
        }
        return null;
    }

    /**
     * Get a default profile IRI from the syntax and/or identifier
     * @param syntax the RDF syntax
     * @param identifier the resource identifier
     * @return a profile IRI usable by the output streamer
     */
    public static IRI getDefaultProfile(final RDFSyntax syntax, final String identifier) {
        return getDefaultProfile(syntax, rdf.createIRI(identifier));
    }

    /**
     * Check if the resource has a deleted mark
     * @param res the resource
     * @return true if the resource has been deleted; false otherwise
     */
    public static Boolean isDeleted(final Resource res) {
        return LDP.Resource.equals(res.getInteractionModel())
            && res.getExtraLinkRelations().anyMatch(e -> e.getValue().equals("type")
                    && e.getKey().equals(DeletedResource.getIRIString()));
    }

    /**
     * Get a default profile IRI from the syntax and/or identifier
     * @param syntax the RDF syntax
     * @param identifier the resource identifier
     * @return a profile IRI usable by the output streamer
     */
    public static IRI getDefaultProfile(final RDFSyntax syntax, final IRI identifier) {
        return RDFA_HTML.equals(syntax) ? identifier : expanded;
    }

    private RdfUtils() {
        // prevent instantiation
    }
}
