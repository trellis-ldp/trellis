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

import static java.lang.String.join;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.Response.Status.FOUND;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.apache.commons.lang3.Range.between;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.domain.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.impl.RdfUtils.getProfile;
import static org.trellisldp.http.impl.RdfUtils.getSyntax;
import static org.trellisldp.vocabulary.JSONLD.expanded;
import static org.trellisldp.vocabulary.LDP.RDFSource;
import static org.trellisldp.vocabulary.LDP.Resource;
import static org.trellisldp.vocabulary.Memento.mementoDatetime;
import static org.trellisldp.vocabulary.Memento.timegate;
import static org.trellisldp.vocabulary.Memento.timemap;
import static org.trellisldp.vocabulary.RDF.type;
import static org.trellisldp.vocabulary.XSD.dateTime;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.Memento;
import org.trellisldp.vocabulary.Time;

/**
 * @author acoburn
 */
public final class MementoResource {

    private static final RDF rdf = getInstance();

    private static final String ORIGINAL = "original";

    private static final String TIMEGATE = "timegate";

    private static final String TIMEMAP = "timemap";

    private static final String MEMENTO = "memento";

    private static final String FROM = "from";

    private static final String UNTIL = "until";

    private static final String DATETIME = "datetime";

    private static final String TIMEMAP_PARAM = "?ext=timemap";

    private static final Function<Link, Stream<Triple>> linkToTriples = link -> {
        final String linkUri = link.getUri().toString();
        final IRI iri = rdf.createIRI(linkUri);
        final List<Triple> buffer = new ArrayList<>();
        final String timeIriPrefix = "http://reference.data.gov.uk/id/gregorian-instant/";

        // TimeMap quads
        if (link.getParams().containsKey(FROM)) {
            buffer.add(rdf.createTriple(iri, type, Memento.TimeMap));
            buffer.add(rdf.createTriple(iri, Time.hasBeginning, rdf.createIRI(timeIriPrefix +
                            parse(link.getParams().get(FROM), RFC_1123_DATE_TIME).toString())));
        }
        if (link.getParams().containsKey(UNTIL)) {
            buffer.add(rdf.createTriple(iri, Time.hasEnd, rdf.createIRI(timeIriPrefix +
                            parse(link.getParams().get(UNTIL), RFC_1123_DATE_TIME).toString())));
        }

        // Quads for Mementos
        if (isMementoLink(link)) {
            final IRI original = rdf.createIRI(linkUri.split("\\?")[0]);
            final IRI timemapUrl = rdf.createIRI(linkUri.split("\\?")[0] + TIMEMAP_PARAM);
            buffer.add(rdf.createTriple(iri, type, Memento.Memento));
            buffer.add(rdf.createTriple(iri, Memento.original, original));
            buffer.add(rdf.createTriple(iri, timegate, original));
            buffer.add(rdf.createTriple(iri, timemap, timemapUrl));
            buffer.add(rdf.createTriple(iri, Time.hasTime, rdf.createIRI(timeIriPrefix +
                            parse(link.getParams().get(DATETIME), RFC_1123_DATE_TIME).toString())));
            buffer.add(rdf.createTriple(iri, mementoDatetime, rdf.createLiteral(parse(
                                link.getParams().get(DATETIME), RFC_1123_DATE_TIME).toString(), dateTime)));
        }
        return buffer.stream();
    };

    private final ServiceBundler trellis;

    /**
     * Wrap a resource in some Memento-specific response builders.
     *
     * @param trellis the Trellis application bundle
     */
    public MementoResource(final ServiceBundler trellis) {
        this.trellis = trellis;
    }

    /**
     * Create a response builder for a TimeMap response.
     *
     * @param mementos the mementos
     * @param baseUrl the base URL
     * @param req the LDP request
     * @return a response builder object
     */
    public ResponseBuilder getTimeMapBuilder(final List<Range<Instant>> mementos, final LdpRequest req,
            final String baseUrl) {

        final List<MediaType> acceptableTypes = req.getHeaders().getAcceptableMediaTypes();
        final String identifier = getBaseUrl(baseUrl, req) + req.getPath();
        final List<Link> links = getMementoLinks(identifier, mementos).collect(toList());

        final ResponseBuilder builder = ok().link(identifier, ORIGINAL + " " + TIMEGATE);
        builder.links(links.toArray(new Link[0])).link(Resource.getIRIString(), "type")
            .link(RDFSource.getIRIString(), "type").header(ALLOW, join(",", GET, HEAD, OPTIONS));

        final Optional<RDFSyntax> syntax;
        syntax = getSyntax(trellis.getIOService(), acceptableTypes, of(APPLICATION_LINK_FORMAT));

        if (syntax.isPresent()) {
            final RDFSyntax rdfSyntax = syntax.get();
            final IRI profile = ofNullable(getProfile(acceptableTypes, syntax.get())).orElse(expanded);

            final List<Triple> extraData = getExtraTriples(identifier);
            for (final Link l : links) {
                if (l.getRels().contains(MEMENTO)) {
                    extraData.add(rdf.createTriple(rdf.createIRI(identifier), Memento.memento,
                                    rdf.createIRI(l.getUri().toString())));
                }
            }

            final StreamingOutput stream = new StreamingOutput() {
                @Override
                public void write(final OutputStream out) throws IOException {
                    trellis.getIOService().write(concat(links.stream().flatMap(linkToTriples), extraData.stream()),
                            out, rdfSyntax, profile);
                }
            };

            return builder.type(syntax.get().mediaType()).entity(stream);
        }

        return builder.type(APPLICATION_LINK_FORMAT)
            .entity(links.stream().map(Link::toString).collect(joining(",\n")) + "\n");
    }

    private static List<Triple> getExtraTriples(final String identifier) {
        final IRI originalResource = rdf.createIRI(identifier);
        final List<Triple> extraData = new ArrayList<>();

        extraData.add(rdf.createTriple(originalResource, type, Memento.OriginalResource));
        extraData.add(rdf.createTriple(originalResource, type, Memento.TimeGate));
        extraData.add(rdf.createTriple(originalResource, timegate, originalResource));
        extraData.add(rdf.createTriple(originalResource, timemap,
                    rdf.createIRI(identifier + TIMEMAP_PARAM)));

        return extraData;
    }

    /**
     * Create a response builder for a TimeGate response.
     *
     * @param mementos the list of memento ranges
     * @param req the LDP request
     * @param baseUrl the base URL
     * @return a response builder object
     */
    public ResponseBuilder getTimeGateBuilder(final List<Range<Instant>> mementos, final LdpRequest req,
            final String baseUrl) {
        final String identifier = getBaseUrl(baseUrl, req) + req.getPath();
        return status(FOUND)
            .location(fromUri(identifier + "?version=" + req.getDatetime().getInstant().toEpochMilli()).build())
            .link(identifier, ORIGINAL + " " + TIMEGATE)
            .links(getMementoLinks(identifier, mementos).toArray(Link[]::new))
            .header(VARY, ACCEPT_DATETIME);
    }

    /**
     * Retrieve all of the Memento-related link headers given a stream of Range objects.
     *
     * @param identifier the public identifier for the resource
     * @param mementos a stream of memento values
     * @return a stream of link headers
     */
    public static Stream<Link> getMementoLinks(final String identifier, final List<Range<Instant>> mementos) {
        return concat(getTimeMap(identifier, mementos.stream()), mementos.stream().map(mementoToLink(identifier)));
    }

    /**
     * Determine whether a link is a well-formed Memento link.
     * @param link the link header
     * @return true if this is a memento link; false otherwise
     */
    public static Boolean isMementoLink(final Link link) {
        return MEMENTO.equals(link.getRel()) && link.getParams().containsKey(DATETIME);
    }

    private String getBaseUrl(final String baseUrl, final LdpRequest req) {
        return ofNullable(baseUrl).orElseGet(req::getBaseUrl);
    }

    private static Stream<Link> getTimeMap(final String identifier, final Stream<Range<Instant>> mementos) {
        return mementos.reduce((acc, x) -> between(acc.getMinimum(), x.getMaximum()))
            .map(x -> Link.fromUri(identifier + TIMEMAP_PARAM).rel(TIMEMAP).type(APPLICATION_LINK_FORMAT)
                .param(FROM, ofInstant(x.getMinimum().minusNanos(1L).plusSeconds(1L), UTC).format(RFC_1123_DATE_TIME))
                .param(UNTIL, ofInstant(x.getMaximum(), UTC).format(RFC_1123_DATE_TIME)).build())
            // TODO use Optional::stream with JDK9
            .map(Stream::of).orElseGet(Stream::empty);
    }

    private static Function<Range<Instant>, Link> mementoToLink(final String identifier) {
        return range ->
            Link.fromUri(identifier + "?version=" + range.getMinimum().toEpochMilli()).rel(MEMENTO)
                .param(DATETIME, ofInstant(range.getMinimum().minusNanos(1L).plusSeconds(1L), UTC)
                        .format(RFC_1123_DATE_TIME)).build();
    }
}
