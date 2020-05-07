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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.ofEpochSecond;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
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
import static org.apache.commons.io.IOUtils.readLines;
import static org.apache.commons.rdf.api.RDFSyntax.JSONLD;
import static org.apache.commons.rdf.api.RDFSyntax.RDFA;
import static org.apache.commons.rdf.api.RDFSyntax.TURTLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Syntax.SPARQL_UPDATE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_BNODE_PREFIX;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.http.core.HttpConstants.ACL;
import static org.trellisldp.http.core.HttpConstants.PATCH;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE_TYPE;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

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
import org.trellisldp.api.AuditService;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Metadata;
import org.trellisldp.api.NoopAuditService;
import org.trellisldp.api.NoopMementoService;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.constraint.LdpConstraintService;
import org.trellisldp.http.core.DefaultTimemapGenerator;
import org.trellisldp.http.core.ServiceBundler;
import org.trellisldp.http.core.TrellisRequest;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Base class for the HTTP handler tests.
 */
class BaseTestHandler {

    static final String ERR_RESPONSE_CODE = "Incorrect response code!";
    static final String CHECK_LINK_TYPES = "Check LDP type link headers";
    static final String RESOURCE_SIMPLE = "/simpleData.txt";
    static final String RESOURCE_TURTLE = "/simpleTriple.ttl";
    static final String RESOURCE_EMPTY = "/emptyData.txt";
    static final String RESOURCE_NAME = "resource";

    static final String baseUrl = "http://example.org/";
    static final RDF rdf = RDFFactory.getInstance();
    static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    static final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + RESOURCE_NAME);
    static final Instant time = ofEpochSecond(1496262729);
    static final Map<String, IRI> extensions = singletonMap(ACL, Trellis.PreferAccessControl);

    private static final Set<IRI> allInteractionModels = new HashSet<>(asList(LDP.Resource, LDP.RDFSource,
            LDP.NonRDFSource, LDP.Container, LDP.BasicContainer, LDP.DirectContainer, LDP.IndirectContainer));

    private final AuditService auditService = new NoopAuditService();
    private final MementoService mementoService = new NoopMementoService();

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
    protected Binary mockBinary;

    @Mock
    protected Resource mockResource, mockParent;

    @Mock
    protected TrellisRequest mockTrellisRequest;

    @Captor
    protected ArgumentCaptor<IRI> iriArgument;

    @Captor
    protected ArgumentCaptor<BinaryMetadata> metadataArgument;

    @BeforeEach
    void setUp() {
        initMocks(this);

        setUpBundler();
        setUpResourceService();
        setUpBinaryService();
        setUpIoService();
        setUpResources();

        when(mockTrellisRequest.getPath()).thenReturn("");
        when(mockTrellisRequest.getBaseUrl()).thenReturn(baseUrl);
        when(mockTrellisRequest.getAcceptableMediaTypes()).thenReturn(singletonList(TEXT_TURTLE_TYPE));
        when(mockTrellisRequest.getHeaders()).thenReturn(new MultivaluedHashMap<>());
    }

    Stream<Executable> checkAllowHeader(final Response res, final List<String> methods) {
        final String allow = res.getHeaderString(ALLOW);
        return concat(of(() -> assertNotNull(allow, "Missing Allow header!")),
                of(GET, HEAD, OPTIONS, PUT, DELETE, POST, PATCH).map(m -> checkAllowHeader(methods, allow, m)));
    }

    private static Executable checkAllowHeader(final List<String> expected, final String actual, final String method) {
        final boolean expectation = expected.contains(method);
        return () -> assertEquals(expectation, actual.contains(method), "Expecting method " + method + " to be "
                + (expectation ? "present" : "absent"));
    }

    Stream<Executable> checkLdpType(final Response res, final IRI type) {
        final Set<String> types = HttpUtils.ldpResourceTypes(type).map(IRI::getIRIString).collect(toSet());
        final Set<String> responseTypes = res.getLinks().stream().filter(link -> TYPE.equals(link.getRel()))
            .map(link -> link.getUri().toString()).collect(toSet());
        return of(LDP.Resource, LDP.RDFSource, LDP.NonRDFSource, LDP.Container, LDP.BasicContainer, LDP.DirectContainer,
                LDP.IndirectContainer).map(t -> checkLdpType(types, responseTypes, t));
    }

    private static Executable checkLdpType(final Set<String> expected, final Set<String> actual, final IRI type) {
        final boolean expectation = expected.contains(type.getIRIString());
        return () -> assertEquals(expectation, actual.contains(type.getIRIString()), "Expecting " + type + " to be "
                + (expectation ? "present" : "absent"));
    }

    void unwrapAsyncError(final CompletionStage async) {
        try {
            async.toCompletableFuture().join();
        } catch (final CompletionException ex) {
            if (ex.getCause() instanceof WebApplicationException) {
                throw (WebApplicationException) ex.getCause();
            }
            throw ex;
        }
    }

    static Predicate<Link> hasLink(final IRI iri, final String rel) {
        return link -> rel.equals(link.getRel()) && iri.getIRIString().equals(link.getUri().toString());
    }

    static Predicate<Link> hasType(final IRI iri) {
        return hasLink(iri, TYPE);
    }

    static CompletionStage<Void> asyncException() {
        return runAsync(() -> {
            throw new RuntimeTrellisException("Expected exception");
        });
    }

    private void setUpResourceService() {
        when(mockResourceService.supportedInteractionModels()).thenReturn(allInteractionModels);
        when(mockResourceService.get(any(IRI.class))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.create(any(Metadata.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.replace(any(Metadata.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.delete(any(Metadata.class))).thenReturn(completedFuture(null));
        when(mockResourceService.add(any(IRI.class), any(Dataset.class))).thenReturn(completedFuture(null));
        when(mockResourceService.unskolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.unskolemize(any(IRI.class))).thenCallRealMethod();
        when(mockResourceService.skolemize(any(Literal.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(IRI.class))).then(returnsFirstArg());
        when(mockResourceService.skolemize(any(BlankNode.class))).thenAnswer(inv ->
                rdf.createIRI(TRELLIS_BNODE_PREFIX + ((BlankNode) inv.getArgument(0)).uniqueReference()));
        when(mockResourceService.toInternal(any(RDFTerm.class), any())).thenCallRealMethod();
        when(mockResourceService.toExternal(any(RDFTerm.class), any())).thenCallRealMethod();
        when(mockResourceService.touch(any(IRI.class))).thenReturn(completedFuture(null));
    }

    private void setUpBinaryService() {
        when(mockBinary.getContent(eq(3), eq(10)))
                        .thenReturn(new ByteArrayInputStream("e input".getBytes(UTF_8)));
        when(mockBinary.getContent())
                        .thenReturn(new ByteArrayInputStream("Some input stream".getBytes(UTF_8)));
        when(mockBinaryService.generateIdentifier()).thenReturn("file:///" + randomUUID());
        when(mockBinaryService.get(any(IRI.class))).thenAnswer(inv -> completedFuture(mockBinary));
        when(mockBinaryService.purgeContent(any(IRI.class))).thenReturn(completedFuture(null));
        when(mockBinaryService.setContent(any(BinaryMetadata.class), any(InputStream.class)))
        .thenAnswer(inv -> {
            readLines((InputStream) inv.getArguments()[1], UTF_8);
            return completedFuture(null);
        });
    }

    private void setUpBundler() {
        when(mockBundler.getResourceService()).thenReturn(mockResourceService);
        when(mockBundler.getBinaryService()).thenReturn(mockBinaryService);
        when(mockBundler.getIOService()).thenReturn(mockIoService);
        when(mockBundler.getAuditService()).thenReturn(auditService);
        when(mockBundler.getMementoService()).thenReturn(mementoService);
        when(mockBundler.getEventService()).thenReturn(mockEventService);
        when(mockBundler.getConstraintServices()).thenReturn(singletonList(new LdpConstraintService()));
        when(mockBundler.getTimemapGenerator()).thenReturn(new DefaultTimemapGenerator());
    }

    private void setUpIoService() {
        when(mockIoService.supportedReadSyntaxes()).thenReturn(asList(TURTLE, JSONLD, RDFA));
        when(mockIoService.supportedWriteSyntaxes()).thenReturn(asList(TURTLE, JSONLD));
        when(mockIoService.supportedUpdateSyntaxes()).thenReturn(singletonList(SPARQL_UPDATE));
    }

    private void setUpResources() {
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getContainer()).thenReturn(Optional.of(root));
        when(mockResource.getIdentifier()).thenReturn(identifier);
        when(mockResource.getBinaryMetadata()).thenReturn(empty());
        when(mockResource.getModified()).thenReturn(time);
        when(mockResource.getExtraLinkRelations()).thenAnswer(inv -> Stream.empty());
        doCallRealMethod().when(mockResource).getRevision();

        when(mockParent.getInteractionModel()).thenReturn(LDP.Container);
        when(mockParent.getIdentifier()).thenReturn(root);
        when(mockParent.getMembershipResource()).thenReturn(empty());
        when(mockParent.getModified()).thenReturn(time);
        doCallRealMethod().when(mockParent).getRevision();
    }
}
