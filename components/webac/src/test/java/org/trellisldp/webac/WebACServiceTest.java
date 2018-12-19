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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.trellisldp.api.Resource.SpecialResources.DELETED_RESOURCE;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
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
import org.junit.jupiter.api.function.Executable;
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

    private static final IRI memberIRI = rdf.createIRI(TRELLIS_DATA_PREFIX + "member");
    private static final IRI nonexistentIRI = rdf.createIRI(TRELLIS_DATA_PREFIX + "parent/child/nonexistent");
    private static final IRI resourceIRI = rdf.createIRI(TRELLIS_DATA_PREFIX + "parent/child/resource");
    private static final IRI childIRI = rdf.createIRI(TRELLIS_DATA_PREFIX + "parent/child");
    private static final IRI parentIRI = rdf.createIRI(TRELLIS_DATA_PREFIX + "parent");
    private static final IRI rootIRI = rdf.createIRI(TRELLIS_DATA_PREFIX);

    private static final IRI authIRI1 = rdf.createIRI(TRELLIS_DATA_PREFIX + "acl/public/auth1");
    private static final IRI authIRI2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "acl/public/auth2");
    private static final IRI authIRI3 = rdf.createIRI(TRELLIS_DATA_PREFIX + "acl/public/auth3");
    private static final IRI authIRI4 = rdf.createIRI(TRELLIS_DATA_PREFIX + "acl/public/auth4");
    private static final IRI authIRI5 = rdf.createIRI(TRELLIS_DATA_PREFIX + "acl/private/auth5");
    private static final IRI authIRI6 = rdf.createIRI(TRELLIS_DATA_PREFIX + "acl/private/auth6");
    private static final IRI authIRI8 = rdf.createIRI(TRELLIS_DATA_PREFIX + "acl/private/auth8");

    private static final IRI addisonIRI = rdf.createIRI("info:user/addison");
    private static final IRI acoburnIRI = rdf.createIRI("info:user/acoburn");
    private static final IRI agentIRI = rdf.createIRI("info:user/agent");

    private static final IRI groupIRI = rdf.createIRI(TRELLIS_DATA_PREFIX + "group/test");
    private static final IRI groupIRI2 = rdf.createIRI(TRELLIS_DATA_PREFIX + "group/test/");

    private static final Set<IRI> allModels = new HashSet<>();

    static {
        allModels.add(LDP.RDFSource);
        allModels.add(LDP.NonRDFSource);
        allModels.add(LDP.DirectContainer);
        allModels.add(LDP.IndirectContainer);
        allModels.add(LDP.BasicContainer);
        allModels.add(LDP.Container);
    }

    private AccessControlService testService;

    @Mock
    private ResourceService mockResourceService;

    @Mock
    private Session mockSession;

    @Mock
    private CacheService<String, Set<IRI>> mockCache;

    @Mock
    private Resource mockResource, mockChildResource, mockParentResource, mockRootResource, mockGroupResource,
            mockMemberResource;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        initMocks(this);

        testService = new WebACService(mockResourceService);

        when(mockCache.get(anyString(), any(Function.class))).thenAnswer(inv -> {
            final String key = inv.getArgument(0);
            final Function<String, String> mapper = inv.getArgument(1);
            return mapper.apply(key);
        });

        setUpResourceService();

        setUpChildResource();
        setUpRootResource();
        setUpMemberResource();

        when(mockResource.hasAcl()).thenReturn(false);
        when(mockResource.getIdentifier()).thenReturn(resourceIRI);
        when(mockResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockResource.getMembershipResource()).thenReturn(empty());

        when(mockParentResource.hasAcl()).thenReturn(false);
        when(mockParentResource.getIdentifier()).thenReturn(parentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.Container);
        when(mockParentResource.getMembershipResource()).thenReturn(empty());

        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockSession.getDelegatedBy()).thenReturn(empty());
    }

    @Test
    public void testDefaultResourceService() {
        assertDoesNotThrow(() -> new WebACService());
    }

    @Test
    public void testCanRead1() {
        when(mockResourceService.get(eq(nonexistentIRI))).thenAnswer(inv -> completedFuture(DELETED_RESOURCE));
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertAll("Check readability for " + acoburnIRI,
                checkCannotRead(nonexistentIRI),
                checkCannotRead(resourceIRI),
                checkCannotRead(childIRI),
                checkCannotRead(parentIRI),
                checkCannotRead(rootIRI));
    }

    @Test
    public void testCanRead2() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        assertAll("Check user can read all resources", checkAllCanRead());
    }

    @Test
    public void testCanRead3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        assertAll("Check user can read all resources", checkAllCanRead());
    }

    @Test
    public void testCanRead4() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockResourceService.supportedInteractionModels()).thenReturn(singleton(LDP.DirectContainer));
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertAll("Check user can read all resources", checkAllCanRead());
    }

    @Test
    public void testCanRead5() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        when(mockResourceService.supportedInteractionModels()).thenReturn(singleton(LDP.IndirectContainer));
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertAll("Check user can read all resources", checkAllCanRead());
    }

    @Test
    public void testCanWrite1() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertAll("Check writability for " + acoburnIRI,
                checkCannotWrite(nonexistentIRI),
                checkCannotWrite(resourceIRI),
                checkCannotWrite(childIRI),
                checkCannotWrite(parentIRI),
                checkCannotWrite(rootIRI));
    }

    @Test
    public void testCanWrite2() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        when(mockResourceService.supportedInteractionModels()).thenReturn(singleton(LDP.Container));
        assertAll("Check writability for " + addisonIRI,
                checkCanWrite(nonexistentIRI),
                checkCanWrite(resourceIRI),
                checkCanWrite(childIRI),
                checkCannotWrite(parentIRI),
                checkCannotWrite(rootIRI));
    }

    @Test
    public void testCanWrite3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        assertAll("Check user can write to all resources", checkAllCanWrite());
    }

    @Test
    public void testCanWrite4() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertAll("Check writability for " + agentIRI, checkCanWrite(memberIRI));
        assertAll("Check user can write to all resources", checkAllCanWrite());
    }

    @Test
    public void testCanWrite5() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertAll("Check writability for " + addisonIRI,
                checkCanWrite(nonexistentIRI),
                checkCanWrite(resourceIRI),
                checkCannotWrite(childIRI),
                checkCannotWrite(parentIRI),
                checkCannotWrite(rootIRI));
    }

    @Test
    public void testCanWrite6() {
        final AccessControlService testService2 = new WebACService(mockResourceService,
                new WebACService.NoopAuthorizationCache(), false);
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertAll("Check writability for " + agentIRI,
                () -> assertTrue(testService2.getAccessModes(memberIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + memberIRI),
                () -> assertTrue(testService2.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + nonexistentIRI),
                () -> assertTrue(testService2.getAccessModes(resourceIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + resourceIRI),
                () -> assertTrue(testService2.getAccessModes(childIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + childIRI),
                () -> assertTrue(testService2.getAccessModes(parentIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + parentIRI),
                () -> assertTrue(testService2.getAccessModes(rootIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + rootIRI));
    }

    @Test
    public void testCanWrite7() {
        final AccessControlService testService2 = new WebACService(mockResourceService,
                new WebACService.NoopAuthorizationCache(), false);
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertAll("Check writability for " + addisonIRI,
                () -> assertTrue(testService2.getAccessModes(nonexistentIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + nonexistentIRI),
                () -> assertTrue(testService2.getAccessModes(resourceIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + resourceIRI),
                () -> assertTrue(testService2.getAccessModes(childIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + childIRI),
                () -> assertFalse(testService2.getAccessModes(parentIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + parentIRI),
                () -> assertFalse(testService2.getAccessModes(rootIRI, mockSession).contains(ACL.Write),
                                 "Cannot write to " + rootIRI));
    }

    @Test
    public void testCanControl1() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertAll("Test controlability for " + acoburnIRI,
                checkCannotWrite(nonexistentIRI),
                checkCannotWrite(resourceIRI),
                checkCannotWrite(childIRI),
                checkCannotWrite(parentIRI),
                checkCannotWrite(rootIRI));
    }

    @Test
    public void testCanControl2() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        assertAll("Test controlability for " + addisonIRI,
                checkCanControl(nonexistentIRI),
                checkCanControl(resourceIRI),
                checkCanControl(childIRI),
                checkCannotControl(parentIRI), checkCannotControl(rootIRI));
    }

    @Test
    public void testCanControl3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        assertAll("Test controlability for " + agentIRI,
                checkCanControl(nonexistentIRI),
                checkCanControl(resourceIRI),
                checkCanControl(childIRI),
                checkCannotControl(parentIRI),
                checkCannotControl(rootIRI));
    }

    @Test
    public void testCanAppend1() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertAll("Test appendability for " + acoburnIRI,
                checkCannotAppend(nonexistentIRI),
                checkCannotAppend(resourceIRI),
                checkCannotAppend(childIRI),
                checkCanAppend(parentIRI),
                checkCanAppend(rootIRI));
    }

    @Test
    public void testCanAppend2() {
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        assertAll("Test appendability for " + addisonIRI,
                checkCannotAppend(nonexistentIRI),
                checkCannotAppend(resourceIRI),
                checkCannotAppend(childIRI),
                checkCanAppend(parentIRI),
                checkCanAppend(rootIRI));
    }

    @Test
    public void testCanAppend3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        assertAll("Test appendability for " + agentIRI,
                checkCannotAppend(nonexistentIRI),
                checkCannotAppend(resourceIRI),
                checkCannotAppend(childIRI),
                checkCannotAppend(parentIRI),
                checkCannotAppend(rootIRI));
    }

    @Test
    public void testCanAppend4() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.IndirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertAll("Test appendability for " + acoburnIRI,
                checkCanWrite(memberIRI),
                checkCannotAppend(nonexistentIRI),
                checkCannotAppend(resourceIRI),
                checkCannotAppend(childIRI),
                checkCanAppend(parentIRI),
                checkCanAppend(rootIRI));
    }

    @Test
    public void testCanAppend5() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockParentResource.getInteractionModel()).thenReturn(LDP.DirectContainer);
        when(mockParentResource.getMembershipResource()).thenReturn(of(memberIRI));
        assertAll("Test appendability for " + agentIRI,
                checkCannotAppend(nonexistentIRI),
                checkCannotAppend(resourceIRI),
                checkCannotAppend(childIRI),
                checkCannotAppend(parentIRI),
                checkCannotAppend(rootIRI));
    }

    @Test
    public void testAdmin1() {
        when(mockSession.getAgent()).thenReturn(Trellis.AdministratorAgent);
        assertAll("Test appendability for admin",
                checkCanAppend(nonexistentIRI),
                checkCanAppend(resourceIRI),
                checkCanAppend(childIRI),
                checkCanAppend(parentIRI),
                checkCanAppend(rootIRI),
                checkCanControl(nonexistentIRI),
                checkCanControl(resourceIRI),
                checkCanControl(childIRI),
                checkCanControl(parentIRI),
                checkCanControl(rootIRI));
        assertAll(checkAllCanRead());
        assertAll(checkAllCanWrite());
    }

    @Test
    public void testDelegate1() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockSession.getDelegatedBy()).thenReturn(of(acoburnIRI));

        assertAll("Test delegated read access 1", checkNoneCanRead());
        assertAll("Test delegated write access 1", checkNoneCanWrite());
    }

    @Test
    public void testDelegate2() {
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        when(mockSession.getDelegatedBy()).thenReturn(of(agentIRI));

        assertAll("Test delegated read access 2", checkNoneCanRead());
        assertAll("Test delegated write access 2", checkNoneCanWrite());
    }

    @Test
    public void testDelegate3() {
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockSession.getDelegatedBy()).thenReturn(of(addisonIRI));
        assertAll("Test delegated writabliity for " + agentIRI + " via " + addisonIRI,
                checkCanWrite(resourceIRI),
                checkCanWrite(childIRI),
                checkCannotWrite(parentIRI),
                checkCannotWrite(rootIRI));
        assertAll(checkAllCanRead());
    }

    @Test
    public void testInheritance() {
        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI8, type, ACL.Authorization),
                rdf.createTriple(authIRI8, ACL.agent, agentIRI),
                rdf.createTriple(authIRI8, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Append)));

        when(mockSession.getAgent()).thenReturn(agentIRI);

        assertAll("Test default ACL writability",
                checkCanWrite(resourceIRI),
                checkCanWrite(childIRI),
                checkCannotWrite(parentIRI),
                checkCannotWrite(rootIRI));
    }

    @Test
    public void testNoInheritance() {
        when(mockChildResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI2, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI2, ACL.agent, agentIRI),
                rdf.createTriple(authIRI2, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI3, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI3, ACL.agent, agentIRI),
                rdf.createTriple(authIRI3, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI4, ACL.agent, agentIRI),
                rdf.createTriple(authIRI4, type, ACL.Authorization)));

        assertAll("Test default ACL writability",
                checkCanWrite(resourceIRI),
                checkCanWrite(childIRI),
                checkCanWrite(parentIRI),
                checkCanWrite(rootIRI));
    }

    @Test
    public void testInheritRoot() {
        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI8, type, ACL.Authorization),
                rdf.createTriple(authIRI8, ACL.agent, agentIRI),
                rdf.createTriple(authIRI8, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Append)));

        when(mockChildResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI2, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI2, ACL.agent, agentIRI),
                rdf.createTriple(authIRI2, ACL.accessTo, childIRI)));

        when(mockSession.getAgent()).thenReturn(agentIRI);

        assertAll("Test default ACL writability",
                checkCannotWrite(resourceIRI),
                checkCanWrite(childIRI),
                checkCannotWrite(parentIRI),
                checkCannotWrite(rootIRI));
    }

    @Test
    public void testFoafAgent() {
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        when(mockChildResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI1, type, ACL.Authorization),
                rdf.createTriple(authIRI1, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI1, ACL.agentClass, FOAF.Agent),
                rdf.createTriple(authIRI1, ACL.accessTo, childIRI),
                rdf.createTriple(authIRI1, ACL.default_, childIRI),

                rdf.createTriple(authIRI3, type, ACL.Authorization),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI3, ACL.agentClass, FOAF.Agent),
                rdf.createTriple(authIRI3, ACL.accessTo, childIRI),
                rdf.createTriple(authIRI3, ACL.default_, childIRI)));
        assertAll("Test foaf:Agent writability",
                checkCanWrite(nonexistentIRI),
                checkCanWrite(resourceIRI),
                checkCanWrite(childIRI),
                checkCannotWrite(parentIRI),
                checkCannotWrite(rootIRI));
        assertAll("Test foaf:Agent readability",
                checkCanRead(nonexistentIRI),
                checkCanRead(resourceIRI),
                checkCanRead(childIRI),
                checkCannotRead(parentIRI),
                checkCannotRead(rootIRI));
        assertAll("Test foaf:Agent controlability",
                checkCannotControl(nonexistentIRI),
                checkCannotControl(resourceIRI),
                checkCannotControl(childIRI),
                checkCannotControl(parentIRI),
                checkCannotControl(rootIRI));
        assertAll("Test foaf:Agent appendability",
                checkCannotAppend(nonexistentIRI),
                checkCannotAppend(resourceIRI),
                checkCannotAppend(childIRI),
                checkCannotAppend(parentIRI),
                checkCannotAppend(rootIRI));
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
                rdf.createTriple(authIRI8, type, ACL.Authorization),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.agent, agentIRI),
                rdf.createTriple(authIRI8, ACL.accessTo, rootIRI)));

        when(mockSession.getAgent()).thenReturn(agentIRI);
        assertAll("Test non-inheritance writability",
                checkCanWrite(resourceIRI),
                checkCanWrite(childIRI),
                checkCannotWrite(parentIRI),
                checkCanWrite(rootIRI));
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

        assertAll("Test group readability", checkAllCanRead());
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

                rdf.createTriple(authIRI3, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI3, ACL.agentGroup, groupIRI2),
                rdf.createTriple(authIRI3, ACL.accessTo, childIRI),

                rdf.createTriple(authIRI4, ACL.agentGroup, groupIRI2),
                rdf.createTriple(authIRI4, type, ACL.Authorization)));

        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, type, ACL.Authorization),
                rdf.createTriple(authIRI5, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append),

                rdf.createTriple(authIRI8, type, ACL.Authorization),
                rdf.createTriple(authIRI8, ACL.agentGroup, groupIRI2),
                rdf.createTriple(authIRI8, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI8, ACL.mode, ACL.Write)));
        assertAll("Test group readability", checkAllCanRead());
    }

    @Test
    public void testAuthenticatedUser() {
        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.agentClass, ACL.AuthenticatedAgent),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append)));
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertAll("Test authenticated user access",
                checkCanRead(rootIRI),
                checkCanAppend(rootIRI),
                checkCannotWrite(rootIRI),
                checkCannotControl(rootIRI));
    }

    @Test
    public void testUnauthenticatedUser() {
        when(mockRootResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI5, ACL.accessTo, rootIRI),
                rdf.createTriple(authIRI5, ACL.agentClass, ACL.AuthenticatedAgent),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI5, ACL.mode, ACL.Append)));
        when(mockSession.getAgent()).thenReturn(Trellis.AnonymousAgent);
        assertAll("Test unauthenticated user access",
                checkCannotRead(rootIRI),
                checkCannotWrite(rootIRI),
                checkCannotAppend(rootIRI),
                checkCannotControl(rootIRI));
    }

    @Test
    public void testCacheCanWrite1() {
        final AccessControlService testCacheService = new WebACService(mockResourceService, mockCache);
        when(mockSession.getAgent()).thenReturn(acoburnIRI);
        assertAll("Check writability with cache", checkCannotWrite(testCacheService, nonexistentIRI),
                checkCannotWrite(testCacheService, resourceIRI), checkCannotWrite(testCacheService, childIRI),
                checkCannotWrite(testCacheService, parentIRI), checkCannotWrite(testCacheService, rootIRI));
    }

    @Test
    public void testCacheCanWrite2() {
        final AccessControlService testCacheService = new WebACService(mockResourceService, mockCache);
        when(mockSession.getAgent()).thenReturn(addisonIRI);
        assertAll("Check writability with cache", checkCanWrite(testCacheService, nonexistentIRI),
                checkCanWrite(testCacheService, resourceIRI), checkCanWrite(testCacheService, childIRI),
                checkCannotWrite(testCacheService, parentIRI), checkCannotWrite(testCacheService, rootIRI));
    }

    @Test
    public void testCacheCanWrite3() {
        final AccessControlService testCacheService = new WebACService(mockResourceService, mockCache);
        when(mockSession.getAgent()).thenReturn(agentIRI);
        when(mockSession.getDelegatedBy()).thenReturn(of(addisonIRI));
        assertAll("Check delegated writability with cache", checkCanWrite(testCacheService, nonexistentIRI),
                checkCanWrite(testCacheService, resourceIRI), checkCanWrite(testCacheService, childIRI),
                checkCannotWrite(testCacheService, parentIRI), checkCannotWrite(testCacheService, rootIRI));
    }

    private Stream<Executable> checkAllCanRead() {
        return Stream.of(checkCanRead(nonexistentIRI), checkCanRead(resourceIRI), checkCanRead(childIRI),
                checkCanRead(parentIRI), checkCanRead(rootIRI));
    }

    private Stream<Executable> checkAllCanWrite() {
        return Stream.of(checkCanWrite(memberIRI), checkCanWrite(nonexistentIRI), checkCanWrite(resourceIRI),
                checkCanWrite(childIRI), checkCanWrite(parentIRI), checkCanWrite(rootIRI));
    }

    private Stream<Executable> checkNoneCanRead() {
        return Stream.of(checkCannotRead(memberIRI), checkCannotRead(nonexistentIRI), checkCannotRead(resourceIRI),
                checkCannotRead(childIRI), checkCannotRead(parentIRI), checkCannotRead(rootIRI));
    }

    private Stream<Executable> checkNoneCanWrite() {
        return Stream.of(checkCannotWrite(nonexistentIRI), checkCannotWrite(resourceIRI), checkCannotWrite(childIRI),
                checkCannotWrite(parentIRI), checkCannotWrite(rootIRI));
    }

    private void setUpChildResource() {
        when(mockChildResource.hasAcl()).thenReturn(true);
        when(mockChildResource.getIdentifier()).thenReturn(childIRI);
        when(mockChildResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockChildResource.getMembershipResource()).thenReturn(empty());
        when(mockChildResource.stream(eq(Trellis.PreferAccessControl))).thenAnswer(inv -> Stream.of(
                rdf.createTriple(authIRI1, type, ACL.Authorization),
                rdf.createTriple(authIRI1, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI1, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI1, ACL.accessTo, childIRI),
                rdf.createTriple(authIRI1, ACL.default_, childIRI),

                rdf.createTriple(authIRI2, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI2, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI2, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI2, ACL.agent, agentIRI),
                rdf.createTriple(authIRI2, ACL.accessTo, childIRI),
                rdf.createTriple(authIRI2, ACL.default_, childIRI),

                rdf.createTriple(authIRI3, type, PROV.Activity),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Read),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Write),
                rdf.createTriple(authIRI3, ACL.mode, ACL.Control),
                rdf.createTriple(authIRI3, ACL.agent, addisonIRI),
                rdf.createTriple(authIRI3, ACL.agent, agentIRI),
                rdf.createTriple(authIRI3, ACL.accessTo, childIRI),
                rdf.createTriple(authIRI3, ACL.default_, childIRI),

                rdf.createTriple(authIRI4, ACL.agent, agentIRI),
                rdf.createTriple(authIRI4, type, ACL.Authorization)));
    }

    private void setUpRootResource() {
        when(mockRootResource.hasAcl()).thenReturn(true);
        when(mockRootResource.getIdentifier()).thenReturn(rootIRI);
        when(mockRootResource.getInteractionModel()).thenReturn(LDP.BasicContainer);
        when(mockRootResource.getMembershipResource()).thenReturn(empty());
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
    }

    private void setUpMemberResource() {
        when(mockMemberResource.hasAcl()).thenReturn(true);
        when(mockMemberResource.getIdentifier()).thenReturn(memberIRI);
        when(mockMemberResource.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(mockMemberResource.getMembershipResource()).thenReturn(empty());
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
    }

    private void setUpResourceService() {
        when(mockResourceService.get(eq(nonexistentIRI))).thenAnswer(inv -> completedFuture(MISSING_RESOURCE));
        when(mockResourceService.supportedInteractionModels()).thenReturn(allModels);
        when(mockResourceService.get(eq(resourceIRI))).thenAnswer(inv -> completedFuture(mockResource));
        when(mockResourceService.get(eq(childIRI))).thenAnswer(inv -> completedFuture(mockChildResource));
        when(mockResourceService.get(eq(parentIRI))).thenAnswer(inv -> completedFuture(mockParentResource));
        when(mockResourceService.get(eq(rootIRI))).thenAnswer(inv -> completedFuture(mockRootResource));
        when(mockResourceService.get(eq(groupIRI))).thenAnswer(inv -> completedFuture(mockGroupResource));
        when(mockResourceService.get(eq(memberIRI))).thenAnswer(inv -> completedFuture(mockMemberResource));
    }

    private Executable checkCannotRead(final IRI id) {
        return () -> assertFalse(testService.getAccessModes(id, mockSession).contains(ACL.Read),
                mockSession.getAgent() + " can Read from " + id);
    }

    private Executable checkCanRead(final IRI id) {
        return () -> assertTrue(testService.getAccessModes(id, mockSession).contains(ACL.Read),
                mockSession.getAgent() + " cannot Read from " + id);
    }

    private Executable checkCannotWrite(final AccessControlService svc, final IRI id) {
        return () -> assertFalse(svc.getAccessModes(id, mockSession).contains(ACL.Write),
                mockSession.getAgent() + " can Write to " + id);
    }

    private Executable checkCanWrite(final AccessControlService svc, final IRI id) {
        return () -> assertTrue(svc.getAccessModes(id, mockSession).contains(ACL.Write),
                mockSession.getAgent() + " cannot Write to " + id);
    }

    private Executable checkCannotWrite(final IRI id) {
        return () -> assertFalse(testService.getAccessModes(id, mockSession).contains(ACL.Write),
                mockSession.getAgent() + " can Write to " + id);
    }

    private Executable checkCanWrite(final IRI id) {
        return () -> assertTrue(testService.getAccessModes(id, mockSession).contains(ACL.Write),
                mockSession.getAgent() + " cannot Write to " + id);
    }

    private Executable checkCannotControl(final IRI id) {
        return () -> assertFalse(testService.getAccessModes(id, mockSession).contains(ACL.Control),
                mockSession.getAgent() + " can Control " + id);
    }

    private Executable checkCanControl(final IRI id) {
        return () -> assertTrue(testService.getAccessModes(id, mockSession).contains(ACL.Control),
                mockSession.getAgent() + " cannot Control " + id);
    }

    private Executable checkCannotAppend(final IRI id) {
        return () -> assertFalse(testService.getAccessModes(id, mockSession).contains(ACL.Append),
                mockSession.getAgent() + " can Append to " + id);
    }

    private Executable checkCanAppend(final IRI id) {
        return () -> assertTrue(testService.getAccessModes(id, mockSession).contains(ACL.Append),
                mockSession.getAgent() + " cannot Append to " + id);
    }
}
