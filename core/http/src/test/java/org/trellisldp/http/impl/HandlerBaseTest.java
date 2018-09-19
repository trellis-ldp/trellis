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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.ofEpochSecond;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Stream.concat;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.RDFUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.http.domain.HttpConstants.PATCH;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE_TYPE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
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
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NoopAuditService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.http.domain.LdpRequest;
import org.trellisldp.vocabulary.LDP;

/**
 * Base class for the HTTP handler tests.
 */
abstract class HandlerBaseTest {

    protected static final String baseUrl = "http://example.org/";
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
    protected EventService mockEventService;

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
        setUpBinaryService();
        setUpIoService();
        setUpResources();

        when(mockHttpHeaders.getAcceptableMediaTypes()).thenReturn(singletonList(TEXT_TURTLE_TYPE));

        when(mockLdpRequest.getSecurityContext()).thenReturn(mockSecurityContext);
        when(mockLdpRequest.getRequest()).thenReturn(mockRequest);
        when(mockLdpRequest.getPath()).thenReturn("");
        when(mockLdpRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockLdpRequest.getHeaders()).thenReturn(mockHttpHeaders);
    }

    protected Stream<Executable> checkAllowHeader(final Response res, final List<String> methods) {
        final String allow = res.getHeaderString(ALLOW);
        return concat(of(() -> assertNotNull(allow, "Missing Allow header!")),
                of(GET, HEAD, OPTIONS, PUT, DELETE, POST, PATCH).map(m -> checkAllowHeader(methods, allow, m)));
    }

    private static Executable checkAllowHeader(final List<String> expected, final String actual, final String method) {
        final Boolean expectation = expected.contains(method);
        return () -> assertEquals(expectation, actual.contains(method), "Expecting method " + method + " to be "
                + (expectation ? "present" : "absent"));
    }

    protected Stream<Executable> checkLdpType(final Response res, final IRI type) {
        final Set<String> types = RdfUtils.ldpResourceTypes(type).map(IRI::getIRIString).collect(toSet());
        final Set<String> responseTypes = res.getLinks().stream().filter(link -> TYPE.equals(link.getRel()))
            .map(link -> link.getUri().toString()).collect(toSet());
        return of(LDP.Resource, LDP.RDFSource, LDP.NonRDFSource, LDP.Container, LDP.BasicContainer, LDP.DirectContainer,
                LDP.IndirectContainer).map(t -> checkLdpType(types, responseTypes, t));
    }

    private static Executable checkLdpType(final Set<String> expected, final Set<String> actual, final IRI type) {
        final Boolean expectation = expected.contains(type);
        return () -> assertEquals(expectation, actual.contains(type), "Expecting " + type + " to be "
                + (expectation ? "present" : "absent"));
    }
    protected void unwrapAsyncError(final CompletableFuture async) {
        try {
            async.join();
        } catch (final CompletionException ex) {
            if (ex.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) ex.getCause();
            }
            throw ex;
        }
    }

    protected static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    protected static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, TYPE);
    }

    protected static CompletableFuture<Void> asyncException() {
        return runAsync(() -> {
            throw new RuntimeTrellisException("Expected exception");
        });
    }

    private void setUpResourceService() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(allInteractionModels);
        when(mockResourceService.get(any(IRI.class))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.create(any(IRI.class), any(IRI.class), any(Dataset.class), any(), any()))
            .thenReturn(completedFuture(null));
        when(mockResourceService.replace(any(IRI.class), any(IRI.class), any(Dataset.class), any(), any()))
            .thenReturn(completedFuture(null));
        when(mockResourceService.delete(any(IRI.class), any(IRI.class), any(Dataset.class)))
            .thenReturn(completedFuture(null));
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenCallRealMethod();
        when(mockResourceService.toExternal(any(RDFTerm.class), any())).thenCallRealMethod();
    }

    private void setUpBinaryService() {
        when(mockBinaryService.generateIdentifier()).thenReturn("file:///" + randomUUID());
        when(mockBinaryService.supportedAlgorithms()).thenReturn(new HashSet<>(asList("MD5", "SHA")));
        when(mockBinaryService.calculateDigest(any(IRI.class), eq("MD5")))
            .thenReturn(completedFuture("md5-digest"));
        when(mockBinaryService.calculateDigest(any(IRI.class), eq("SHA")))
            .thenReturn(completedFuture("sha1-digest"));
        when(mockBinaryService.getContent(any(IRI.class), eq(3), eq(10)))
            .thenAnswer(x -> completedFuture(new ByteArrayInputStream("e input".getBytes(UTF_8))));
        when(mockBinaryService.getContent(any(IRI.class)))
            .thenAnswer(x -> completedFuture(new ByteArrayInputStream("Some input stream".getBytes(UTF_8))));
        when(mockBinaryService.setContent(any(IRI.class), any(InputStream.class), any()))
            .thenAnswer(x -> completedFuture(null));
    }

    private void setUpBundler() {
        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockBundler.getBinaryService()).thenReturn(mockBinaryService);
        when(mockBundler.getIOService()).thenReturn(mockIoService);
        when(mockBundler.getAuditService()).thenReturn(auditService);
        when(mockBundler.getMementoService()).thenReturn(mementoService);
        when(mockBundler.getAgentService()).thenReturn(agentService);
        when(mockBundler.getEventService()).thenReturn(mockEventService);
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
        when(mockParent.getIdentifier()).thenReturn(root);
        when(mockParent.getMembershipResource()).thenReturn(empty());
    }
}
