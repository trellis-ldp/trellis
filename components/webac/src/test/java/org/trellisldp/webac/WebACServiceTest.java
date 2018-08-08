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
package org.trellisldp.webac;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.vocabulary.RDF.type;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.jena.JenaRDF;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.trellisldp.api.AccessControlService;
import org.trellisldp.api.CacheService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.Session;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.FOAF;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.PROV;
import org.trellisldp.vocabulary.Trellis;
import org.trellisldp.vocabulary.VCARD;

/**
 * Tests to verify the correctness of WebAC authorization decisions.
 */
public class WebACServiceTest {

    private static final RDF rdf = new JenaRDF();

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private Session mockSession;

    @Mock
    private CacheService<String, Set<IRI>> mockCache;

    @Mock
    private Resource mockResource, mockChildResource, mockParentResource, mockRootResource, mockGroupResource,
            mockMemberResource;

    private AccessControlService testService;

    private static final IRI nonexistentIRI = rdf.createIRI("trellis:data/parent/child/nonexistent");

    private static final IRI resourceIRI = rdf.createIRI("trellis:data/parent/child/resource");

    private static final IRI childIRI = rdf.createIRI("trellis:data/parent/child");

    private static final IRI parentIRI = rdf.createIRI("trellis:data/parent");

    private static final IRI rootIRI = rdf.createIRI("trellis:data/");

    private static final IRI authIRI1 = rdf.createIRI("trellis:data/acl/public/auth1");

    private static final IRI authIRI2 = rdf.createIRI("trellis:data/acl/public/auth2");

    private static final IRI authIRI3 = rdf.createIRI("trellis:data/acl/public/auth3");

    private static final IRI authIRI4 = rdf.createIRI("trellis:data/acl/public/auth4");

    private static final IRI authIRI5 = rdf.createIRI("trellis:data/acl/private/auth5");

    private static final IRI authIRI6 = rdf.createIRI("trellis:data/acl/private/auth6");

    private static final IRI authIRI8 = rdf.createIRI("trellis:data/acl/private/auth8");

    private static final IRI memberIRI = rdf.createIRI("trellis:data/member");

    private static final IRI addisonIRI = rdf.createIRI("info:user/addison");

    private static final IRI acoburnIRI = rdf.createIRI("info:user/acoburn");

    private static final IRI agentIRI = rdf.createIRI("info:user/agent");

    private static final IRI groupIRI = rdf.createIRI("trellis:data/group/test");

    private static final IRI groupIRI2 = rdf.createIRI("trellis:data/group/test/");


    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        initMocks(this);
        final Set<IRI> allModels = new HashSet<>();
        allModels.add(LDP.RDFSource);
        allModels.add(LDP.NonRDFSource);
        allModels.add(LDP.DirectContainer);
        allModels.add(LDP.IndirectContainer);
        allModels.add(LDP.BasicContainer);
        allModels.add(LDP.Container);

        testService = new WebACService(mockResourceService);

        when(mockCache.get(anyString(), any(Function.class))).thenAnswer(inv -> {
            final String key = inv.getArgument(0);
            final Function<String, String> mapper = inv.getArgument(1);
            return mapper.apply(key);
        });

        when(mockChildResource.hasAcl()).thenReturn(true);
        when(mockChildResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI1, type, ACL.Authorization),
                rdf.createTriple(authIRI1, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI1, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI1, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI2, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI2, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI2, ACL.agent, agentIRI),
                rdf.createTriple(authIRI2, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI3, type, PROV.Activity),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI3, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI3, ACL.agent, agentIRI),
                rdf.createTriple(authIRI3, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI4, ACL.agent, agentIRI),
                rdf.createTriple(authIRI4, type, ACL.Authorization)));

        when(mockRootResource.hasAcl()).thenReturn(true);
        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append),

                rdf.createTriple(authIRI6, type, ACL.Authorization),
                rdf.createTriple(authIRI6, ACL.agent, acoburnIRI),
                rdf.createTriple(authIRI6, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI6, ACL.mode, ACL.Append),

                rdf.createTriple(authIRI8, type, ACL.Authorization),
                rdf.createTriple(authIRI8, ACL.agent, agentIRI),
                rdf.createTriple(authIRI8, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Write)));

        when(mockMemberResource.hasAcl()).thenReturn(true);
        when(mockMemberResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, ACL.accessTo, memberIRI),
                rdf.createTriple(authIRI5, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append),

                rdf.createTriple(authIRI6, type, ACL.Authorization),
                rdf.createTriple(authIRI6, ACL.agent, acoburnIRI),
                rdf.createTriple(authIRI6, ACL.accessTo, memberIRI),
                rdf.createTriple(authIRI6, ACL.mode, ACL.Write),

                rdf.createTriple(authIRI8, type, ACL.Authorization),
                rdf.createTriple(authIRI8, ACL.agent, agentIRI),
                rdf.createTriple(authIRI8, ACL.accessTo, memberIRI),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Write)));

        when(mockResourceService.get(eq(nonexistentIRI))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockResourceService.supportedInteractionModels()).thenReturn(allModels);
        when(mockResourceService.get(eq(resourceIRI))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.get(eq(childIRI))).thenAnswer(inv -> completedFuture(mockChildResource));
        when(mockResourceService.get(eq(parentIRI))).thenAnswer(inv -> completedFuture(mockParentResource));
        when(mockResourceService.get(eq(rootIRI))).thenAnswer(inv -> completedFuture(mockRootResource));
        when(mockResourceService.get(eq(groupIRI))).thenAnswer(inv -> completedFuture(mockGroupResource));
        when(mockResourceService.get(eq(memberIRI))).thenAnswer(inv -> completedFuture(mockMemberResource));
        when(mockResourceService.getContainer(nonexistentIRI)).thenReturn(of(resourceIRI));
        when(mockResourceService.getContainer(resourceIRI)).thenReturn(of(childIRI));
        when(mockResourceService.getContainer(childIRI)).thenReturn(of(parentIRI));
        when(mockResourceService.getContainer(parentIRI)).thenReturn(of(rootIRI));

        when(mockResource.getIdentifier()).thenReturn(resourceIRI);
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getMembershipResource()).thenReturn(empty());
        when(mockChildResource.getIdentifier()).thenReturn(childIRI);
        when(mockChildResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockChildResource.getMembershipResource()).thenReturn(empty());
        when(mockParentResource.getIdentifier()).thenReturn(parentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockParentResource.getMembershipResource()).thenReturn(empty());
        when(mockRootResource.getIdentifier()).thenReturn(rootIRI);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(empty());
        when(mockMemberResource.getIdentifier()).thenReturn(memberIRI);
        when(mockMemberResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockMemberResource.getMembershipResource()).thenReturn(empty());
        when(mockResource.hasAcl()).thenReturn(false);
        when(mockParentResource.hasAcl()).thenReturn(false);

        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockSession.getDelegatedBy()).thenReturn(empty());
    }

    @Test
    public void testCanRead1() {
        when(mockResourceService.get(eq(nonexistentIRI))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Read));
    }

    @Test
    public void testCanRead2() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        checkAllCanRead();
    }

    @Test
    public void testCanRead3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        checkAllCanRead();
    }

    @Test
    public void testCanRead4() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockResourceService.supportedInteractionModels()).thenReturn(singleton(LDP.DirectContainer));
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        checkAllCanRead();
    }

    @Test
    public void testCanRead5() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        when(mockResourceService.supportedInteractionModels()).thenReturn(singleton(LDP.IndirectContainer));
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        checkAllCanRead();
    }

    @Test
    public void testCanWrite1() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }

    @Test
    public void testCanWrite2() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        when(mockResourceService.supportedInteractionModels()).thenReturn(singleton(LDP.Container));
        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }

    @Test
    public void testCanWrite3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        checkAllCanWrite();
    }

    @Test
    public void testCanWrite4() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertTrue(testService.getAccessModes(memberIRI, mockSession).contains(ACL.Write));
        checkAllCanWrite();
    }

    @Test
    public void testCanWrite5() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }

    @Test
    public void testCanWrite6() {
        final AccessControlService testService2 = new WebACService(mockResourceService, null, false);
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        checkAllCanWrite();
    }

    @Test
    public void testCanWrite7() {
        final AccessControlService testService2 = new WebACService(mockResourceService, null, false);
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertTrue(testService2.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertTrue(testService2.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertTrue(testService2.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testService2.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService2.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }

    @Test
    public void testCanControl1() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Control));
    }

    @Test
    public void testCanControl2() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Control));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Control));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Control));
    }

    @Test
    public void testCanControl3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Control));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Control));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Control));
    }

    @Test
    public void testCanAppend1() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Append));
    }

    @Test
    public void testCanAppend2() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Append));
    }

    @Test
    public void testCanAppend3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Append));
    }

    @Test
    public void testCanAppend4() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertTrue(testService.getAccessModes(memberIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Append));
    }

    @Test
    public void testCanAppend5() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Append));
    }

    @Test
    public void testAdmin1() {
        when(mockSession.getAgent()).thenReturn(Trellis.AdministratorAgent);
        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Append));
        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Control));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Control));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Control));
        assertTrue(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Control));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Control));
        checkAllCanRead();
        checkAllCanWrite();
    }

    @Test
    public void testDelegate1() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockSession.getDelegatedBy()).thenReturn(of(acoburnIRI));

        checkNoneCanRead();
        checkNoneCanWrite();
    }

    @Test
    public void testDelegate2() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        when(mockSession.getDelegatedBy()).thenReturn(of(agentIRI));

        checkNoneCanRead();
        checkNoneCanWrite();
    }

    @Test
    public void testDelegate3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockSession.getDelegatedBy()).thenReturn(of(addisonIRI));

        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));

        checkAllCanRead();
    }

    @Test
    public void testDefaultForNew() {
        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, type, ACL.Authorization),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.agent, addisonIRI),

                rdf.createTriple(authIRI6, type, ACL.Authorization),
                rdf.createTriple(authIRI6, ACL.mode, ACL.Append),
                rdf.createTriple(authIRI6, ACL.agent, acoburnIRI),
                rdf.createTriple(authIRI6, ACL.accessTo, rootIRI),

                rdf.createTriple(authIRI8, type, ACL.Authorization),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI8, ACL.agent, agentIRI),
                rdf.createTriple(authIRI8, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI8, ACL.default_, rootIRI)));

        when(mockSession.getAgent()).thenReturn(agentIRI);

        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }

    @Test
    public void testFoafAgent() {
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        when(mockChildResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI1, type, ACL.Authorization),
                rdf.createTriple(authIRI1, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI1, ACL.agentClass, FOAF.Agent),
                rdf.createTriple(authIRI1, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI2, type, ACL.Authorization),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI2, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI2, ACL.agent, agentIRI),
                rdf.createTriple(authIRI2, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI3, type, ACL.Authorization),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI3, ACL.agentClass, FOAF.Agent),
                rdf.createTriple(authIRI3, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI4, ACL.agent, agentIRI),
                rdf.createTriple(authIRI4, type, ACL.Authorization)));

        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));

        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Read));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Read));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Read));

        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Control));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Control));

        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Append));
    }

    @Test
    public void testNotInherited() {
        when(mockParentResource.hasAcl()).thenReturn(true);
        when(mockParentResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                    rdf.createTriple(authIRI5, type, ACL.Authorization),
                    rdf.createTriple(authIRI5, ACL.accessTo, parentIRI),
                    rdf.createTriple(authIRI5, ACL.agent, agentIRI),
                    rdf.createTriple(authIRI5, ACL.mode, ACL.Read)));

        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, type, ACL.Authorization),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append),
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.agent, addisonIRI),

                rdf.createTriple(authIRI6, type, ACL.Authorization),
                rdf.createTriple(authIRI6, ACL.mode, ACL.Append),
                rdf.createTriple(authIRI6, ACL.agent, acoburnIRI),
                rdf.createTriple(authIRI6, ACL.accessTo, rootIRI),

                rdf.createTriple(authIRI8, type, ACL.Authorization),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.agent, agentIRI),
                rdf.createTriple(authIRI8, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI8, ACL.default_, rootIRI)));

        when(mockSession.getAgent()).thenReturn(agentIRI);

        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }

    @Test
    public void testGroup() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        when(mockGroupResource.stream(eq(Trellis.PreferUserManaged))).thenAnswer(inv -> Stream.of(
                    rdf.createTriple(authIRI1, VCARD.hasMember, acoburnIRI),
                    rdf.createTriple(groupIRI, VCARD.hasMember, addisonIRI),
                    rdf.createTriple(groupIRI, type, VCARD.Group),
                    rdf.createTriple(groupIRI, VCARD.hasMember, acoburnIRI)));

        when(mockChildResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI2, type, ACL.Authorization),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI2, ACL.agentGroup, groupIRI),
                rdf.createTriple(authIRI2, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI3, type, PROV.Activity),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI3, ACL.agentGroup, groupIRI),
                rdf.createTriple(authIRI3, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI4, ACL.agentGroup, groupIRI),
                rdf.createTriple(authIRI4, type, ACL.Authorization)));

        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append),

                rdf.createTriple(authIRI8, ACL.agentGroup, groupIRI),
                rdf.createTriple(authIRI8, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Write)));

        checkAllCanRead();
    }

    @Test
    public void testGroup2() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        when(mockGroupResource.stream(eq(Trellis.PreferUserManaged))).thenAnswer(inv -> Stream.of(
                    rdf.createTriple(authIRI1, VCARD.hasMember, acoburnIRI),
                    rdf.createTriple(groupIRI2, VCARD.hasMember, addisonIRI),
                    rdf.createTriple(groupIRI2, type, VCARD.Group),
                    rdf.createTriple(groupIRI2, VCARD.hasMember, acoburnIRI)));

        when(mockChildResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI2, type, ACL.Authorization),
                rdf.createTriple(authIRI2, ACL.agentGroup, groupIRI2),
                rdf.createTriple(authIRI2, ACL.accessTo, childIRI),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Control),

                rdf.createTriple(authIRI3, type, PROV.Activity),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI3, ACL.agentGroup, groupIRI2),
                rdf.createTriple(authIRI3, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI4, ACL.agentGroup, groupIRI2),
                rdf.createTriple(authIRI4, type, ACL.Authorization)));

        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, type, ACL.Authorization),
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append),

                rdf.createTriple(authIRI8, type, ACL.Authorization),
                rdf.createTriple(authIRI8, ACL.agentGroup, groupIRI2),
                rdf.createTriple(authIRI8, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Write)));

        checkAllCanRead();
    }

    @Test
    public void testAuthenticatedUser() {
        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.agentClass, ACL.AuthenticatedAgent),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append)));
        when(mockSession.getAgent()).thenReturn(acoburnIRI);

        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Read));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Control));
    }

    @Test
    public void testUnauthenticatedUser() {
        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.agentClass, ACL.AuthenticatedAgent),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append)));
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);

        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Append));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Control));
    }

    @Test
    public void testCacheCanWrite1() {
        final AccessControlService testCacheService = new WebACService(mockResourceService, mockCache);
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        checkNoneCanWrite();
    }

    @Test
    public void testCacheCanWrite2() {
        final AccessControlService testCacheService = new WebACService(mockResourceService, mockCache);
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        assertTrue(testCacheService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertTrue(testCacheService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertTrue(testCacheService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testCacheService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertFalse(testCacheService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }

    @Test
    public void testCacheCanWrite3() {
        final AccessControlService testCacheService = new WebACService(mockResourceService, mockCache);
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockSession.getDelegatedBy()).thenReturn(of(addisonIRI));

        assertTrue(testCacheService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertTrue(testCacheService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertTrue(testCacheService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testCacheService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertFalse(testCacheService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }

    private void checkAllCanRead() {
        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Read));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Read));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Read));
        assertTrue(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Read));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Read));
    }

    private void checkAllCanWrite() {
        assertTrue(testService.getAccessModes(memberIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertTrue(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }

    private void checkNoneCanRead() {
        assertFalse(testService.getAccessModes(memberIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Read));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Read));
    }

    private void checkNoneCanWrite() {
        assertFalse(testService.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(resourceIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(childIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(parentIRI, mockSession).contains(ACL.Write));
        assertFalse(testService.getAccessModes(rootIRI, mockSession).contains(ACL.Write));
    }
}
