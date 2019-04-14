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
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.ServiceLoader.load;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.HttpHeaders.VARY;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Response.Status.FOUND;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_DATETIME;
import static org.trellisldp.http.core.HttpConstants.APPLICATION_LINK_FORMAT;
import static org.trellisldp.http.core.HttpConstants.DATETIME;
import static org.trellisldp.http.core.HttpConstants.FROM;
import static org.trellisldp.http.core.HttpConstants.MEMENTO;
import static org.trellisldp.http.core.HttpConstants.ORIGINAL;
import static org.trellisldp.http.core.HttpConstants.TIMEGATE;
import static org.trellisldp.http.core.HttpConstants.TIMEMAP;
import static org.trellisldp.http.core.HttpConstants.UNTIL;
import static org.trellisldp.http.impl.HttpUtils.getProfile;
import static org.trellisldp.http.impl.HttpUtils.getSyntax;
import static org.trellisldp.vocabulary.JSONLD.compacted;
import static org.trellisldp.vocabulary.LDP.RDFSource;
import static org.trellisldp.vocabulary.LDP.Resource;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.SortedSet;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.core.TimemapGenerator;
import org.trellisldp.http.core.TrellisRequest;

/**
 * @author acoburn
 */
public final class MementoResource {

    private static final String TIMEMAP_PARAM = "?ext=timemap";
    private static final TimemapGenerator timemap = of(load(TimemapGenerator.class)).map(ServiceLoader::iterator)
        .filter(Iterator::hasNext).map(Iterator::next).orElseGet(() -> new TimemapGenerator() { });

    private final ServiceBundler trellis;
    private final boolean includeMementoDates;

    /**
     * Wrap a resource in some Memento-specific response builders.
     *
     * @param trellis the Trellis application bundle
     * @param includeMementoDates whether to include memento dates in link headers
     */
    public MementoResource(final ServiceBundler trellis, final boolean includeMementoDates) {
        this.trellis = trellis;
        this.includeMementoDates = includeMementoDates;
    }

    /**
     * Create a response builder for a TimeMap response.
     *
     * @param mementos the mementos
     * @param baseUrl the base URL
     * @param req the LDP request
     * @return a response builder object
     */
    public ResponseBuilder getTimeMapBuilder(final SortedSet<Instant> mementos, final TrellisRequest req,
            final String baseUrl) {

        final List<MediaType> acceptableTypes = req.getAcceptableMediaTypes();
        final String identifier = fromUri(getBaseUrl(baseUrl, req)).path(req.getPath()).build().toString();
        final List<Link> links = getMementoLinks(identifier, mementos).collect(toList());

        final ResponseBuilder builder = ok().link(identifier, ORIGINAL + " " + TIMEGATE);
        builder.links(links.stream().map(this::filterLinkParams).toArray(Link[]::new))
            .link(Resource.getIRIString(), TYPE).link(RDFSource.getIRIString(), TYPE)
            .header(ALLOW, join(",", GET, HEAD, OPTIONS));

        final Optional<RDFSyntax> syntax;
        syntax = getSyntax(trellis.getIOService(), acceptableTypes, of(APPLICATION_LINK_FORMAT));

        if (syntax.isPresent()) {
            final RDFSyntax rdfSyntax = syntax.get();
            final IRI profile = getProfile(acceptableTypes, rdfSyntax);
            final IRI jsonldProfile = nonNull(profile) ? profile : compacted;

            final StreamingOutput stream = new StreamingOutput() {
                @Override
                public void write(final OutputStream out) throws IOException {
                    trellis.getIOService().write(timemap.asRdf(identifier, links), out, rdfSyntax, jsonldProfile);
                }
            };

            return builder.type(syntax.get().mediaType()).entity(stream);
        }

        return builder.type(APPLICATION_LINK_FORMAT)
            .entity(links.stream().map(this::filterLinkParams).map(Link::toString).collect(joining(",\n")) + "\n");
    }

    /**
     * Create a response builder for a TimeGate response.
     *
     * @param mementos the list of memento ranges
     * @param req the LDP request
     * @param baseUrl the base URL
     * @return a response builder object
     */
    public ResponseBuilder getTimeGateBuilder(final SortedSet<Instant> mementos, final TrellisRequest req,
            final String baseUrl) {
        final String identifier = fromUri(getBaseUrl(baseUrl, req)).path(req.getPath()).build().toString();
        return status(FOUND)
            .location(fromUri(identifier + "?version=" + req.getDatetime().getInstant().getEpochSecond()).build())
            .link(identifier, ORIGINAL + " " + TIMEGATE)
            .links(getMementoLinks(identifier, mementos).map(this::filterLinkParams).toArray(Link[]::new))
            .header(VARY, ACCEPT_DATETIME);
    }

    /**
     * Filter link parameters from a provided Link object, if configured to do so.
     * @param link the link
     * @return a Link without Memento parameters, if desired; otherwise, the original link
     */
    public Link filterLinkParams(final Link link) {
        return filterLinkParams(link, !includeMementoDates);
    }

    /**
     * Retrieve all of the Memento-related link headers given a collection of datetimes.
     *
     * @param identifier the public identifier for the resource
     * @param mementos a collection of memento values
     * @return a stream of link headers
     */
    public static Stream<Link> getMementoLinks(final String identifier, final SortedSet<Instant> mementos) {
        if (mementos.isEmpty()) {
            return empty();
        }
        return concat(getTimeMap(identifier, mementos.first(), mementos.last()),
                mementos.stream().map(mementoToLink(identifier)));
    }

    /**
     * Filter link parameters from a provided Link object, if configured to do so.
     * @param link the link
     * @param filter whether to filter the memento parameters
     * @return a Link without Memento parameters, if desired; otherwise, the original link
     */
    public static Link filterLinkParams(final Link link, final boolean filter) {
        // from and until parameters can cause problems with downstream applications because they contain commas. This
        // method makes it possible to filter out those params, if desired. By default, they are not filtered out.
        if (filter) {
            if (TIMEMAP.equals(link.getRel())) {
                return Link.fromUri(link.getUri()).rel(TIMEMAP).type(APPLICATION_LINK_FORMAT).build();
            } else if (MEMENTO.equals(link.getRel())) {
                return Link.fromUri(link.getUri()).rel(MEMENTO).build();
            }
        }
        return link;
    }

    private static String getBaseUrl(final String baseUrl, final TrellisRequest req) {
        return ofNullable(baseUrl).orElseGet(req::getBaseUrl);
    }

    private static Stream<Link> getTimeMap(final String identifier, final Instant from, final Instant until) {
        return Stream.of(Link.fromUri(identifier + TIMEMAP_PARAM).rel(TIMEMAP).type(APPLICATION_LINK_FORMAT)
                .param(FROM, ofInstant(from.truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME))
                .param(UNTIL, ofInstant(until.truncatedTo(SECONDS), UTC).format(RFC_1123_DATE_TIME)).build());
    }

    private static Function<Instant, Link> mementoToLink(final String identifier) {
        return time ->
            Link.fromUri(identifier + "?version=" + time.truncatedTo(SECONDS).getEpochSecond()).rel(MEMENTO)
                .param(DATETIME, ofInstant(time.truncatedTo(SECONDS), UTC)
                        .format(RFC_1123_DATE_TIME)).build();
    }
}
