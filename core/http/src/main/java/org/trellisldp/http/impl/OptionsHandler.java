/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
 *
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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.MediaType.WILDCARD;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.core.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.core.HttpConstants.PATCH;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.TrellisRequest;

/**
 * The OPTIONS response builder.
 *
 * @author acoburn
 */
public class OptionsHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(OptionsHandler.class);

    /**
     * An OPTIONS response builder.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param extensions the extension graph mapping
     */
    public OptionsHandler(final TrellisRequest req, final ServiceBundler trellis, final Map<String, IRI> extensions) {
        super(req, trellis, extensions, null);
    }

    /**
     * Build the representation for the given resource.
     *
     * @return the options response builder
     */
    public ResponseBuilder ldpOptions() {
        LOGGER.debug("OPTIONS request for {}", getIdentifier());

        final List<RDFSyntax> rdfSyntaxes = getServices().getIOService().supportedWriteSyntaxes();
        final Stream<String> allSyntaxes = concat(rdfSyntaxes.stream().map(RDFSyntax::mediaType), of(WILDCARD));

        return status(NO_CONTENT)
            .header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH, PUT, DELETE, POST))
            .header(ACCEPT_PATCH, APPLICATION_SPARQL_UPDATE)
            .header(ACCEPT_POST, allSyntaxes.collect(joining(",")));
    }
}
