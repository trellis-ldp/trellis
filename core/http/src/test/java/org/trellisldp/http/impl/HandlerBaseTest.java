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

import static com.google.common.collect.Sets.newHashSet;
import static java.time.Instant.ofEpochSecond;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.of;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.HttpMethod.POST;
import static javax.ws.rs.HttpMethod.PUT;
import static javax.ws.rs.core.HttpHeaders.ALLOW;
import static javax.ws.rs.core.Link.TYPE;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.http.domain.HttpConstants.PATCH;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.RDFTerm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopAuditService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.api.Session;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

/**
 * Base class for the HTTP handler tests.
 */
abstract class HandlerBaseTest {

    protected static final String baseUrl = "http://example.org/repo/";
    protected static final RDF rdf = getInstance();
    protected static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    protected static final Set<IRI> allInteractionModels = newHashSet(LDP.Resource, LDP.RDFSource,
            LDP.NonRDFSource, LDP.Container, LDP.BasicContainer, LDP.DirectContainer, LDP.IndirectContainer);
    protected static final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
    protected static final Instant time = ofEpochSecond(1496262729);

    protected final AgentService agentService = new SimpleAgentService();
    protected final AuditService auditService = new NoopAuditService();
    protected final MementoService mementoService = new NoopMementoService();

    @Mock
    protected ServiceBundler mockBundler;

    @Mock
    protected ResourceService mockResourceService;

    @Mock
    protected IOService mockIoService;

    @Mock
    protected BinaryService mockBinaryService;

    @Mock
    protected Resource mockResource, mockParent;

    @Mock
    protected LdpRequest mockLdpRequest;

    @Mock
    protected Request mockRequest;

    @Mock
    protected HttpHeaders mockHttpHeaders;

    @Mock
    protected SecurityContext mockSecurityContext;

    @Captor
    protected ArgumentCaptor<IRI> iriArgument;

    @Captor
    protected ArgumentCaptor<Map<String, String>> metadataArgument;

    @BeforeEach
    public void setUp() {
        initMocks(this);

        setUpBundler();
        setUpResourceService();
        setUpIoService();
        setUpResources();

        when(mockBinaryService.generateIdentifier()).thenReturn("file:///" + randomUUID());

        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(TEXT_TURTLE_TYPE));

        when(mockLdpRequest.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockLdpRequest.getRequest()).thenReturn(mockRequest);
        when(mockLdpRequest.getPath()).thenReturn("");
        when(mockLdpRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockLdpRequest.getHeaders()).thenReturn(mockHttpHeaders);
    }

    protected Stream<Executable> checkAllowHeader(final Response res, final List<String> methods) {
        final String allow = res.getHeaderString(ALLOW);
        return of(
                () -> assertNotNull(allow),
                () -> assertEquals(allow.contains(GET), methods.contains(GET)),
                () -> assertEquals(allow.contains(HEAD), methods.contains(HEAD)),
                () -> assertEquals(allow.contains(OPTIONS), methods.contains(OPTIONS)),
                () -> assertEquals(allow.contains(PUT), methods.contains(PUT)),
                () -> assertEquals(allow.contains(DELETE), methods.contains(DELETE)),
                () -> assertEquals(allow.contains(POST), methods.contains(POST)),
                () -> assertEquals(allow.contains(PATCH), methods.contains(PATCH)));
    }

    protected Stream<Executable> checkLdpType(final Response res, final IRI type) {
        final Set<String> types = RdfUtils.ldpResourceTypes(type).map(IRI::getIRIString).collect(toSet());
        final Set<String> responseTypes = res.getLinks().stream().filter(link -> TYPE.equals(link.getRel()))
            .map(link -> link.getUri().toString()).collect(toSet());
        return of(
                () -> assertEquals(responseTypes.contains(LDP.Resource), types.contains(LDP.Resource)),
                () -> assertEquals(responseTypes.contains(LDP.RDFSource), types.contains(LDP.RDFSource)),
                () -> assertEquals(responseTypes.contains(LDP.NonRDFSource), types.contains(LDP.NonRDFSource)),
                () -> assertEquals(responseTypes.contains(LDP.Container), types.contains(LDP.Container)),
                () -> assertEquals(responseTypes.contains(LDP.BasicContainer), types.contains(LDP.BasicContainer)),
                () -> assertEquals(responseTypes.contains(LDP.DirectContainer), types.contains(LDP.DirectContainer)),
                () -> assertEquals(responseTypes.contains(LDP.IndirectContainer),
                                 types.contains(LDP.IndirectContainer)));
    }

    protected static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    protected static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, TYPE);
    }

    private void setUpResourceService() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(allInteractionModels);
        when(mockResourceService.get(any(IRI.class))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.create(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class),
                        any(), any())).thenReturn(completedFuture(true));
        when(mockResourceService.replace(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class),
                        any(), any())).thenReturn(completedFuture(true));
        when(mockResourceService.delete(any(IRI.class), any(Session.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));
        when(mockResourceService.add(any(IRI.class), any(Session.class), any(Dataset.class)))
            .thenReturn(completedFuture(true));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenAnswer(inv -> {
            final RDFTerm term = (RDFTerm) inv.getArgument(0);
            if (term instanceof IRI) {
                final String iri = ((IRI) term).getIRIString();
                if (iri.startsWith(baseUrl)) {
                    return rdf.createIRI(TRELLIS_DATA_PREFIX + iri.substring(baseUrl.length()));
                }
            }
            return term;
        });
    }

    private void setUpBundler() {
        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockBundler.getBinaryService()).thenReturn(mockBinaryService);
        when(mockBundler.getIOService()).thenReturn(mockIoService);
        when(mockBundler.getAuditService()).thenReturn(auditService);
        when(mockBundler.getMementoService()).thenReturn(mementoService);
        when(mockBundler.getAgentService()).thenReturn(agentService);
    }

    private void setUpIoService() {
        when(mockIoService.supportedReadSyntaxes()).thenReturn(asList(TURTLE, JSONLD, RDFA));
        when(mockIoService.supportedWriteSyntaxes()).thenReturn(asList(TURTLE, JSONLD));
        when(mockIoService.supportedUpdateSyntaxes()).thenReturn(asList(SPARQL_UPDATE));
    }

    private void setUpResources() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getBinary()).thenReturn(empty());
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());

        when(mockParent.getInteractionModel()).thenReturn(LDP.Container);
    }
}
