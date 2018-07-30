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
import static java.util.stream.Collectors.joining;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.Response.Status.GONE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.NO_CONTENT;
import static javax.ws.rs.core.Response.status;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_PATCH;
import static org.trellisldp.http.domain.HttpConstants.ACCEPT_POST;
import static org.trellisldp.http.domain.HttpConstants.ACL;
import static org.trellisldp.http.domain.HttpConstants.PATCH;
import static org.trellisldp.http.domain.HttpConstants.TIMEMAP;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.impl.RdfUtils.ldpResourceTypes;
import static org.trellisldp.vocabulary.LDP.NonRDFSource;
import static org.trellisldp.vocabulary.LDP.RDFSource;
import static org.trellisldp.vocabulary.Trellis.PreferAccessControl;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.slf4j.Logger;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.LdpRequest;

/**
 * The OPTIONS response builder.
 *
 * @author acoburn
 */
public class OptionsHandler extends BaseLdpHandler {

    private static final Logger LOGGER = getLogger(OptionsHandler.class);

    private final IRI graphName;

    /**
     * An OPTIONS response builder.
     *
     * @param req the LDP request
     * @param trellis the Trellis application bundle
     * @param baseUrl the base URL
     */
    public OptionsHandler(final LdpRequest req, final ServiceBundler trellis, final String baseUrl) {
        super(req, trellis, baseUrl);
        this.graphName = ACL.equals(req.getExt()) ? PreferAccessControl : PreferUserManaged;
    }

    /**
     * Initialize the request handler.
     * @param resource the Trellis resource
     * @return a response builder
     */
    public ResponseBuilder initialize(final Resource resource) {

        if (MISSING_RESOURCE.equals(resource)) {
            return status(NOT_FOUND);
        } else if (DELETED_RESOURCE.equals(resource)) {
            return status(GONE);
        }

        mayContinue(true);
        setResource(resource);
        return status(NO_CONTENT);
    }

    /**
     * Build the representation for the given resource.
     *
     * @param builder a response builder
     * @return the response builder
     */
    public ResponseBuilder ldpOptions(final ResponseBuilder builder) {
        if (!mayContinue()) {
            return builder;
        }

        LOGGER.debug("OPTIONS request for {}", getIdentifier());

        ldpResourceTypes(getResource().getInteractionModel())
            .forEach(type -> builder.link(type.getIRIString(), "type"));

        if (getResource().isMemento() || TIMEMAP.equals(getRequest().getExt())) {
            // Mementos and TimeMaps are read-only
            builder.header(ALLOW, join(",", GET, HEAD, OPTIONS));
        } else {
            builder.header(ACCEPT_PATCH, APPLICATION_SPARQL_UPDATE);
            // ACL resources allow a limited set of methods (no DELETE or POST)
            // If it's not a container, POST isn't allowed
            if (PreferAccessControl.equals(graphName) || getResource().getInteractionModel().equals(RDFSource) ||
                    getResource().getInteractionModel().equals(NonRDFSource)) {
                builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH, PUT, DELETE));
            } else {
                // Containers and binaries support POST
                builder.header(ALLOW, join(",", GET, HEAD, OPTIONS, PATCH, PUT, DELETE, POST));
                builder.header(ACCEPT_POST, getServices().getIOService().supportedWriteSyntaxes().stream()
                        .map(RDFSyntax::mediaType).collect(joining(",")));
            }
        }

        return builder;
    }
}
