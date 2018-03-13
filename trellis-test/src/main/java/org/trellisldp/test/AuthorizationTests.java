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
package org.trellisldp.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

/**
 * Authorization tests.
 *
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class AuthorizationTests extends BaseCommonTests {

    private static final String JWT_SECRET = "secret";
    private static final String ACL = "acl";
    private static final String PATCH = "PATCH";
    private static final String EXT_ACL = "?ext=acl";
    private static final String INSERT_PROP_FOO = "INSERT { <> <http://example.com/prop> \"Foo\" } WHERE {}";
    private static final String INSERT_PROP_BAR = "INSERT { <> <http://example.com/prop> \"Bar\" } WHERE {}";
    private static final String PREFIX_ACL = "PREFIX acl: <http://www.w3.org/ns/auth/acl#>\n\n";

    private static String container;
    private static String publicContainer;
    private static String publicContainerAcl;
    private static String publicContainerChild;
    private static String protectedContainer;
    private static String protectedContainerAcl;
    private static String protectedContainerChild;
    private static String privateContainer;
    private static String privateContainerAcl;
    private static String privateContainerChild;
    private static String groupContainer;
    private static String groupContainerAcl;
    private static String groupContainerChild;
    private static String defaultContainer;
    private static String defaultContainerAcl;
    private static String defaultContainerChild;
    private static String groupResource;

    protected static void setUp() {
        final String jwt = buildJwt(Trellis.AdministratorAgent.getIRIString(), JWT_SECRET);

        final String containerContent = getResourceAsString("/basicContainer.ttl");

        // POST an LDP-BC
        try (final Response res = target().request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            container = res.getLocation().toString();
        }

        // POST a public container
        try (final Response res = target(container).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            publicContainer = res.getLocation().toString();
        }

        // Add a child to the public container
        try (final Response res = target(publicContainer).request().header(AUTHORIZATION, jwt)
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            publicContainerChild = res.getLocation().toString();
            publicContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(ACL))
                .map(link -> link.getUri().toString()).findFirst().orElse("");
        }

        final String publicAcl = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
            + PREFIX_ACL
            + "INSERT DATA { [acl:accessTo <" + publicContainer + ">; acl:mode acl:Read; "
            + "   acl:agentClass foaf:Agent ] }; \n"
            + PREFIX_ACL
            + "INSERT DATA { [acl:accessTo <" + publicContainer + ">; acl:mode acl:Read, acl:Write;"
            + "   acl:agentClass acl:AuthenticatedAgent ] }";

        // Add an ACL for the public container
        try (final Response res = target(publicContainerAcl).request().header(AUTHORIZATION, jwt).method(PATCH,
                    entity(publicAcl, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        // POST a protected container
        try (final Response res = target(container).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            protectedContainer = res.getLocation().toString();
        }

        // Add a child to the protected container
        try (final Response res = target(protectedContainer).request().header(AUTHORIZATION, jwt)
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            protectedContainerChild = res.getLocation().toString();
            protectedContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(ACL))
                .map(link -> link.getUri().toString()).findFirst().orElse("");
        }

        final String protectedAcl = PREFIX_ACL
            + "INSERT DATA { \n"
            + "[acl:accessTo  <" + protectedContainer + ">;  acl:mode acl:Read, acl:Write;"
            + "   acl:agent <https://people.apache.org/~acoburn/#i> ] };"
            + PREFIX_ACL
            + "INSERT DATA { \n"
            + "[acl:accessTo  <" + protectedContainer + ">; acl:mode acl:Read, acl:Append; "
            + "   acl:agent <https://madison.example.com/profile/#me> ] }";

        // Add an ACL for the protected container
        try (final Response res = target(protectedContainerAcl).request().header(AUTHORIZATION, jwt).method(PATCH,
                    entity(protectedAcl, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        // POST a private container
        try (final Response res = target(container).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            privateContainer = res.getLocation().toString();
        }

        // Add a child to the private container
        try (final Response res = target(privateContainer).request().header(AUTHORIZATION, jwt)
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            privateContainerChild = res.getLocation().toString();
            privateContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(ACL))
                .map(link -> link.getUri().toString()).findFirst().orElse("");
        }

        final String privateAcl = PREFIX_ACL
            + "INSERT DATA { "
            + "[acl:accessTo  <" + privateContainer + ">; acl:mode acl:Read, acl:Write; "
            + "   acl:agent <http://example.com/administrator> ] }";

        // Add an ACL for the private container
        try (final Response res = target(privateContainerAcl).request().header(AUTHORIZATION, jwt).method(PATCH,
                    entity(privateAcl, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        final String groupContent
            = "@prefix acl: <http://www.w3.org/ns/auth/acl#>.\n"
            + "@prefix vcard: <http://www.w3.org/2006/vcard/ns#> .\n"
            + "<> a acl:GroupListing.\n"
            + "<#Developers> a vcard:Group;\n"
            + "  vcard:hasUID <urn:uuid:8831CBAD-1111-2222-8563-F0F4787E5398:ABGroup>;\n"
            + "  vcard:hasMember <https://pat.example.com/profile/card#me>;\n"
            + "  vcard:hasMember <https://people.apache.org/~acoburn/#i>.\n"
            + "<#Management> a vcard:Group;\n"
            + "  vcard:hasUID <urn:uuid:8831CBAD-3333-4444-8563-F0F4787E5398:ABGroup>;\n"
            + "  vcard:hasMember <https://madison.example.com/profile/#me>.";

        // POST a group listing
        try (final Response res = target(container).request()
                .header(AUTHORIZATION, jwt).post(entity(groupContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            groupResource = res.getLocation().toString();
        }

        // POST a group-controlled container
        try (final Response res = target(container).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            groupContainer = res.getLocation().toString();
        }

        // Add a child to the group container
        try (final Response res = target(groupContainer).request().header(AUTHORIZATION, jwt)
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            groupContainerChild = res.getLocation().toString();
            groupContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(ACL))
                .map(link -> link.getUri().toString()).findFirst().orElse("");
        }

        final String groupAcl = PREFIX_ACL
            + "INSERT DATA {  "
            + "[acl:accessTo <" + groupContainer + ">; acl:mode acl:Read, acl:Write; "
            + " acl:agentGroup <" + groupResource + "#Developers> ] };\n"
            + PREFIX_ACL
            + "INSERT DATA {  "
            + "[acl:accessTo <" + groupContainer + ">; acl:mode acl:Read; "
            + " acl:agentGroup <" + groupResource + "#Management> ] }";

        // Add an ACL for the private container
        try (final Response res = target(groupContainerAcl).request().header(AUTHORIZATION, jwt).method(PATCH,
                    entity(groupAcl, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }

        // POST a container with a default ACL
        try (final Response res = target(container).request()
                .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            defaultContainer = res.getLocation().toString();
        }

        // Add a child to the public container
        try (final Response res = target(defaultContainer).request().header(AUTHORIZATION, jwt)
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            defaultContainerChild = res.getLocation().toString();
            defaultContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(ACL))
                .map(link -> link.getUri().toString()).findFirst().orElse("");
        }

        final String defaultAcl = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
            + PREFIX_ACL
            + "INSERT DATA {  "
            + "[acl:accessTo <" + defaultContainer + ">; acl:mode acl:Read; acl:agentClass foaf:Agent ] }; \n"
            + PREFIX_ACL
            + "INSERT DATA { [acl:accessTo <" + defaultContainer + ">; acl:mode acl:Read, acl:Write; \n"
            + "   acl:default <" + defaultContainer + ">; \n"
            + "   acl:agent <https://people.apache.org/~acoburn/#i> ] }";

        // Add an ACL for the public container
        try (final Response res = target(defaultContainerAcl).request().header(AUTHORIZATION, jwt).method(PATCH,
                    entity(defaultAcl, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Test for an ACL Link header on a public resource.
     */
    @Test
    public void testPublicLinkHeader() {
        assertEquals(publicContainer + EXT_ACL, publicContainerAcl);
    }

    /**
     * Test for an ACL Link header on a protected resource.
     */
    @Test
    public void testProtectedLinkHeader() {
        assertEquals(protectedContainer + EXT_ACL, protectedContainerAcl);
    }

    /**
     * Test for an ACL Link header on a private resource.
     */
    @Test
    public void testPrivateLinkHeader() {
        assertEquals(privateContainer + EXT_ACL, privateContainerAcl);
    }

    /**
     * Jwt Administrator tests.
     */
    @Nested
    @DisplayName("Jwt Administrator tests")
    public class JwtAdministratorTests {

        private final String jwt = "Bearer " + Jwts.builder().claim("webid",
                Trellis.AdministratorAgent.getIRIString())
            .signWith(SignatureAlgorithm.HS512, JWT_SECRET.getBytes(UTF_8))
            .compact();

        /**
         * Verify that an administrator can read a public resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read a public resource")
        public void testAdminCanReadPublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can read the child of a public resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read the child of a public resource")
        public void testAdminCanReadPublicResourceChild() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to a public resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to a public resource")
        public void testAdminCanWritePublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to the child of a public resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to the child of a public resource")
        public void testAdminCanWritePublicResourceChild() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can control a public resource.
         */
        @Test
        @DisplayName("Verify that an administrator can control a public resource")
        public void testAdminCanControlPublicResource() {
            try (final Response res = target(publicContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can control the child of a public resource.
         */
        @Test
        @DisplayName("Verify that an administrator can control the child of a public resource")
        public void testAdminCanControlPublicResourceChild() {
            try (final Response res = target(publicContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an administrator can read a protected resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read a protected resource")
        public void testAdminCanReadProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can read the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read the child of a protected resource")
        public void testAdminCanReadProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to a protected resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to a protected resource")
        public void testAdminCanWriteProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to the child of a protected resource")
        public void testAdminCanWriteProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can control a protected resource.
         */
        @Test
        @DisplayName("Verify that an administrator can control a protected resource")
        public void testAdminCanControlProtectedResource() {
            try (final Response res = target(protectedContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can control the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that an administrator can control the child of a protected resource")
        public void testAdminCanControlProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an administrator can read a private resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read a private resource")
        public void testAdminCanReadPrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can read the child of a private resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read the child of a private resource")
        public void testAdminCanReadPrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to a private resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to a private resource")
        public void testAdminCanWritePrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to the child of a private resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to the child of a private resource")
        public void testAdminCanWritePrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can control a private resource.
         */
        @Test
        @DisplayName("Verify that an administrator can control a private resource")
        public void testAdminCanControlPrivateResource() {
            try (final Response res = target(privateContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can control the child of a private resource.
         */
        @Test
        @DisplayName("Verify that an administrator can control the child of a private resource")
        public void testAdminCanControlPrivateResourceChild() {
            try (final Response res = target(privateContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an administrator can read a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read a group-controlled resource")
        public void testAdminCanReadGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can read the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read the child of a group-controlled resource")
        public void testAdminCanReadGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to a group-controlled resource")
        public void testAdminCanWriteGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to the child of a group-controlled resource")
        public void testAdminCanWriteGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can control a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an administrator can control a group-controlled resource")
        public void testAdminCanControlGroupResource() {
            try (final Response res = target(groupContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can't find the ACL of a child resource.
         */
        @Test
        @DisplayName("Verify that an administrator can't find the ACL of a child resource")
        public void testAdminCanControlGroupResourceChild() {
            try (final Response res = target(groupContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an administrator can read a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read a default ACL resource")
        public void testCanReadDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can read the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an administrator can read the child of a default ACL resource")
        public void testCanReadDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to a default ACL resource")
        public void testCanWriteDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can write to the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an administrator can write to the child of a default ACL resource")
        public void testCanWriteDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can control a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an administrator can control a default ACL resource")
        public void testCanControlDefaultAclResource() {
            try (final Response res = target(defaultContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an administrator can't find the ACL resource.
         */
        @Test
        @DisplayName("Verify that an administrator can't find the ACL resource")
        public void testCanControlDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
            }
        }
    }

    /**
     * Basic Auth User tests.
     */
    @Nested
    @DisplayName("Basic Auth User tests")
    public class BasicAuthUserTests {

        private final String auth = "Basic " + encodeBase64String("acoburn:secret".getBytes());

        /**
         * Verify that a user can read a public resource.
         */
        @Test
        @DisplayName("Verify that a user can read a public resource")
        public void testUserCanReadPublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a public resource")
        public void testUserCanReadPublicResourceChild() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can append to a public resource.
         */
        @Test
        @DisplayName("Verify that a user can append to a public resource")
        public void testUserCanAppendPublicResource() {
            try (final Response res = target(publicContainer).request().header(AUTHORIZATION, auth)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a public resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a public resource")
        public void testUserCanWritePublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a public resource")
        public void testUserCanWritePublicResourceChild() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a public resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a public resource")
        public void testUserCanControlPublicResource() {
            try (final Response res = target(publicContainer + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a public resource")
        public void testUserCanControlPublicResourceChild() {
            try (final Response res = target(publicContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can read a protected resource")
        public void testUserCanReadProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a protected resource")
        public void testUserCanReadProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can append to a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can append to a protected resource")
        public void testUserCanAppendProtectedResource() {
            try (final Response res = target(protectedContainer).request().header(AUTHORIZATION, auth)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a protected resource")
        public void testUserCanWriteProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a protected resource")
        public void testUserCanWriteProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a protected resource")
        public void testUserCanControlProtectedResource() {
            try (final Response res = target(protectedContainerAcl).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a protected resource")
        public void testUserCanControlProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot read a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read a private resource")
        public void testUserCanReadPrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot read the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read the child of a private resource")
        public void testUserCanReadPrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot append to a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot append to a private resource")
        public void testUserCanAppendPrivateResource() {
            try (final Response res = target(privateContainer).request().header(AUTHORIZATION, auth)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a private resource")
        public void testUserCanWritePrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a private resource")
        public void testUserCanWritePrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a private resource")
        public void testUserCanControlPrivateResource() {
            try (final Response res = target(privateContainer + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a private resource")
        public void testUserCanControlPrivateResourceChild() {
            try (final Response res = target(privateContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can read a group-controlled resource")
        public void testCanReadGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a group-controlled resource")
        public void testCanReadGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a group-controlled resource")
        public void testCanWriteGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a group-controlled resource")
        public void testCanWriteGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a group-controlled resource")
        public void testCanControlGroupResource() {
            try (final Response res = target(groupContainerAcl).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a group-controlled resource")
        public void testCanControlGroupResourceChild() {
            try (final Response res = target(groupContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can read a default ACL resource")
        public void testCanReadDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a default ACL resource")
        public void testCanReadDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a default ACL resource")
        public void testCanWriteDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a default ACL resource")
        public void testCanWriteDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a default ACL resource")
        public void testCanControlDefaultAclResource() {
            try (final Response res = target(defaultContainerAcl).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a default ACL resource")
        public void testCanControlDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }
    }

    /**
     * JWT user tests.
     */
    @Nested
    @DisplayName("Jwt User tests")
    public class JwtUserTests {

        private final String jwt = buildJwt("https://people.apache.org/~acoburn/#i", JWT_SECRET);

        /**
         * Verify that a user can read a public resource.
         */
        @Test
        @DisplayName("Verify that a user can read a public resource")
        public void testUserCanReadPublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a public resource")
        public void testUserCanReadPublicResourceChild() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can append to a public resource.
         */
        @Test
        @DisplayName("Verify that a user can append to a public resource")
        public void testUserCanAppendPublicResource() {
            try (final Response res = target(publicContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a public resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a public resource")
        public void testUserCanWritePublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a public resource")
        public void testUserCanWritePublicResourceChild() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a public resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a public resource")
        public void testUserCanControlPublicResource() {
            try (final Response res = target(publicContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a public resource")
        public void testUserCanControlPublicResourceChild() {
            try (final Response res = target(publicContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can read a protected resource")
        public void testUserCanReadProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a protected resource")
        public void testUserCanReadProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can append to a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can append to a protected resource")
        public void testUserCanAppendProtectedResource() {
            try (final Response res = target(protectedContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a protected resource")
        public void testUserCanWriteProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a protected resource")
        public void testUserCanWriteProtectedResourceChile() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a protected resource")
        public void testUserCanControlProtectedResource() {
            try (final Response res = target(protectedContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a protected resource")
        public void testUserCanControlProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot read a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read a private resource")
        public void testUserCanReadPrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot read the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read the child of a private resource")
        public void testUserCanReadPrivateResourceChile() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot append to a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot append to a private resource")
        public void testUserCanAppendPrivateResource() {
            try (final Response res = target(privateContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a private resource")
        public void testUserCanWritePrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a private resource")
        public void testUserCanWritePrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a private resource")
        public void testUserCanControlPrivateResource() {
            try (final Response res = target(privateContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a private resource")
        public void testUserCanControlPrivateResourceChild() {
            try (final Response res = target(privateContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can read a group-controlled resource")
        public void testCanReadGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a group-controlled resource")
        public void testCanReadGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a group-controlled resource")
        public void testCanWriteGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a group-controlled resource")
        public void testCanWriteGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a group-controlled resource")
        public void testCanControlGroupResource() {
            try (final Response res = target(groupContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a group-controlled resource")
        public void testCanControlGroupResourceChild() {
            try (final Response res = target(groupContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can read a default ACL resource")
        public void testCanReadDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a default ACL resource")
        public void testCanReadDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a default ACL resource")
        public void testCanWriteDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a default ACL resource")
        public void testCanWriteDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a default ACL resource")
        public void testCanControlDefaultAclResource() {
            try (final Response res = target(defaultContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a default ACL resource")
        public void testCanControlDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }
    }

    /**
     * Basic Auth other user tests.
     */
    @Nested
    @DisplayName("Basic Auth other user tests")
    public class BasicAuthOtherUserTests {

        private final String auth = "Basic " + encodeBase64String("user:password".getBytes(UTF_8));

        /**
         * Verify that a user can read a public resource.
         */
        @Test
        @DisplayName("Verify that a user can read a public resource")
        public void testUserCanReadPublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a public resource")
        public void testUserCanReadPublicResourceChile() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can append to a public resource.
         */
        @Test
        @DisplayName("Verify that a user can append to a public resource")
        public void testUserCanAppendPublicResource() {
            try (final Response res = target(publicContainer).request().header(AUTHORIZATION, auth)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a public resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a public resource")
        public void testUserCanWritePublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a public resource")
        public void testUserCanWritePublicResourceChild() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a public resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a public resource")
        public void testUserCanControlPublicResource() {
            try (final Response res = target(publicContainer + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a public resource")
        public void testUserCanControlPublicResourceChild() {
            try (final Response res = target(publicContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can read a protected resource")
        public void testUserCanReadProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a protected resource")
        public void testUserCanReadProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can append to a public resource.
         */
        @Test
        @DisplayName("Verify that a user can append to a public resource")
        public void testUserCanAppendProtectedResource() {
            try (final Response res = target(protectedContainer).request().header(AUTHORIZATION, auth)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot write to a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a protected resource")
        public void testUserCanWriteProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a protected resource")
        public void testUserCanWriteProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a protected resource")
        public void testUserCanControlProtectedResource() {
            try (final Response res = target(protectedContainerAcl).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a protected resource")
        public void testUserCanControlProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot read a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read a private resource")
        public void testUserCanReadPrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot read the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read the child of a private resource")
        public void testUserCanReadPrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot append to a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot append to a private resource")
        public void testUserCanAppendPrivateResource() {
            try (final Response res = target(privateContainer).request().header(AUTHORIZATION, auth)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a private resource")
        public void testUserCanWritePrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a private resource")
        public void testUserCanWritePrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a private resource")
        public void testUserCanControlPrivateResource() {
            try (final Response res = target(privateContainerAcl).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a private resource")
        public void testUserCanControlPrivateResourceChild() {
            try (final Response res = target(privateContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can read a group-controlled resource")
        public void testCanReadGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a group-controlled resource")
        public void testCanReadGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot append to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot append to a group-controlled resource")
        public void testUserCanAppendGroupResource() {
            try (final Response res = target(groupContainer).request().header(AUTHORIZATION, auth)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a group-controlled resource")
        public void testCanWriteGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a group-controlled resource")
        public void testCanWriteGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a group-controlled resource")
        public void testCanControlGroupResource() {
            try (final Response res = target(groupContainerAcl).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a group-controlled resource")
        public void testCanControlGroupResourceChild() {
            try (final Response res = target(groupContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can read a default ACL resource")
        public void testCanReadDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot read the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read the child of a default ACL resource")
        public void testCanReadDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot append to a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot append to a default ACL resource")
        public void testUserCanAppendDefaultAclResource() {
            try (final Response res = target(defaultContainer).request().header(AUTHORIZATION, auth)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a default ACL resource")
        public void testCanWriteDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a default ACL resource")
        public void testCanWriteDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, auth).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a default ACL resource")
        public void testCanControlDefaultAclResource() {
            try (final Response res = target(defaultContainerAcl).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a default ACL resource")
        public void testCanControlDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, auth).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }
    }

    /**
     * JWT Other user tests.
     */
    @Nested
    @DisplayName("Jwt Other user tests")
    public class JwtOtherUserTests {

        private final String jwt = buildJwt("https://madison.example.com/profile/#me", JWT_SECRET);

        /**
         * Verify that a user can read a public resource.
         */
        @Test
        @DisplayName("Verify that a user can read a public resource")
        public void testUserCanReadPublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a public resource")
        public void testUserCanReadPublicResourceChild() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can append to a public resource.
         */
        @Test
        @DisplayName("Verify that a user can append to a public resource")
        public void testUserCanAppendPublicResource() {
            try (final Response res = target(publicContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to a public resource.
         */
        @Test
        @DisplayName("Verify that a user can write to a public resource")
        public void testUserCanWritePublicResource() {
            try (final Response res = target(publicContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can write to the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user can write to the child of a public resource")
        public void testUserCanWritePublicResourceChild() {
            try (final Response res = target(publicContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot control a public resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a public resource")
        public void testUserCanControlPublicResource() {
            try (final Response res = target(publicContainer + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a public resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a public resource")
        public void testUserCanControlPublicResourceChild() {
            try (final Response res = target(publicContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can read a protected resource")
        public void testUserCanReadProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a protected resource")
        public void testUserCanReadProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can append to a protected resource.
         */
        @Test
        @DisplayName("Verify that a user can append to a protected resource")
        public void testUserCanAppendProtectedResource() {
            try (final Response res = target(protectedContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot write to a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a protected resource")
        public void testUserCanWriteProtectedResource() {
            try (final Response res = target(protectedContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a protected resource")
        public void testUserCanWriteProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a protected resource")
        public void testUserCanControlProtectedResource() {
            try (final Response res = target(protectedContainer + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a protected resource")
        public void testUserCanControlProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot read a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read a private resource")
        public void testUserCanReadPrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot read the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read the child of a private resource")
        public void testUserCanReadPrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot append to a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot append to a private resource")
        public void testUserCanAppendPrivateResource() {
            try (final Response res = target(privateContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a private resource")
        public void testUserCanWritePrivateResource() {
            try (final Response res = target(privateContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a private resource")
        public void testUserCanWritePrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a private resource")
        public void testUserCanControlPrivateResource() {
            try (final Response res = target(privateContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a private resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a private resource")
        public void testUserCanControlPrivateResourceChild() {
            try (final Response res = target(privateContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can read a group-controlled resource")
        public void testCanReadGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user can read the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user can read the child of a group-controlled resource")
        public void testCanReadGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot append to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot append to a group-controlled resource")
        public void testUserCanAppendGroupResource() {
            try (final Response res = target(groupContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a group-controlled resource")
        public void testCanWriteGroupResource() {
            try (final Response res = target(groupContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a group-controlled resource")
        public void testCanWriteGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a group-controlled resource")
        public void testCanControlGroupResource() {
            try (final Response res = target(groupContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a group-controlled resource")
        public void testCanControlGroupResourceChild() {
            try (final Response res = target(groupContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user can read a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user can read a default ACL resource")
        public void testCanReadDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that a user cannot read the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot read the child of a default ACL resource")
        public void testCanReadDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot append to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that a user cannot append to a group-controlled resource")
        public void testUserCanAppendDefaultAclResource() {
            try (final Response res = target(defaultContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to a default ACL resource")
        public void testCanWriteDefaultAclResource() {
            try (final Response res = target(defaultContainer).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot write to the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot write to the child of a default ACL resource")
        public void testCanWriteDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request()
                    .header(AUTHORIZATION, jwt).method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a default ACL resource")
        public void testCanControlDefaultAclResource() {
            try (final Response res = target(defaultContainerAcl).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a default ACL resource")
        public void testCanControlDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild + EXT_ACL).request()
                    .header(AUTHORIZATION, jwt).get()) {
                assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()));
            }
        }
    }

    /**
     * Anonymous tests.
     */
    @Nested
    @DisplayName("Anonymous tests")
    public class AnonymousTests {

        /**
         * Verify that an anonymous user can read a public resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user can read a public resource")
        public void testCanReadPublicResource() {
            try (final Response res = target(publicContainer).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an anonymous user can read the child of a public resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user can read the child of a public resource")
        public void testCanReadPublicResourceChild() {
            try (final Response res = target(publicContainerChild).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an anonymous user cannot append to a public resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot append to a public resource")
        public void testUserCanAppendPublicResource() {
            try (final Response res = target(publicContainer).request().post(entity("", TEXT_TURTLE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to a public resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to a public resource")
        public void testCanWritePublicResource() {
            try (final Response res = target(publicContainer).request().method(PATCH, entity(INSERT_PROP_BAR,
                            APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to the child of a public resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to the child of a public resource")
        public void testCanWritePublicResourceChild() {
            try (final Response res = target(publicContainerChild).request().method(PATCH,
                        entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot control a public resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot control a public resource")
        public void testCanControlPublicResource() {
            try (final Response res = target(publicContainerAcl).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot control the child of a public resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot control the child of a public resource")
        public void testCanControlPublicResourceChild() {
            try (final Response res = target(publicContainerChild + EXT_ACL).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot read a protected resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot read a protected resource")
        public void testCanReadProtectedResource() {
            try (final Response res = target(protectedContainer).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot read the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot read the child of a protected resource")
        public void testCanReadProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot append to a protected resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot append to a protected resource")
        public void testUserCanAppendProtectedResource() {
            try (final Response res = target(protectedContainer).request().post(entity("", TEXT_TURTLE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to a protected resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to a protected resource")
        public void testCanWriteProtectedResource() {
            try (final Response res = target(protectedContainer).request().method(PATCH,
                        entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to the child of a protected resource")
        public void testCanWriteProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild).request().method(PATCH,
                        entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot control a protected resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot control a protected resource")
        public void testCanControlProtectedResource() {
            try (final Response res = target(protectedContainerAcl).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot control the child of a protected resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot control the child of a protected resource")
        public void testCanControlProtectedResourceChild() {
            try (final Response res = target(protectedContainerChild + EXT_ACL).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot read a private resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot read a private resource")
        public void testCanReadPrivateResource() {
            try (final Response res = target(privateContainer).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot read the child of a private resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot read the child of a private resource")
        public void testCanReadPrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot append to a private resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot append to a private resource")
        public void testUserCanAppendPrivateResource() {
            try (final Response res = target(privateContainer).request().post(entity("", TEXT_TURTLE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to a private resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to a private resource")
        public void testCanWritePrivateResource() {
            try (final Response res = target(privateContainer).request().method(PATCH,
                        entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to the child of a private resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to the child of a private resource")
        public void testCanWritePrivateResourceChild() {
            try (final Response res = target(privateContainerChild).request().method(PATCH,
                        entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot control a private resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot control a private resource")
        public void testCanControlPrivateResource() {
            try (final Response res = target(privateContainerAcl).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot control the child of a private resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot control the child of a private resource")
        public void testCanControlPrivateResourceChild() {
            try (final Response res = target(privateContainerChild + EXT_ACL).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot read a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot read a group-controlled resource")
        public void testCanReadGroupResource() {
            try (final Response res = target(groupContainer).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot read the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot read the child of a group-controlled resource")
        public void testCanReadGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot append to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot append to a group-controlled resource")
        public void testUserCanAppendGroupResource() {
            try (final Response res = target(groupContainer).request().post(entity("", TEXT_TURTLE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to a group-controlled resource")
        public void testCanWriteGroupResource() {
            try (final Response res = target(groupContainer).request().method(PATCH,
                        entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to the child of a group-controlled resource")
        public void testCanWriteGroupResourceChild() {
            try (final Response res = target(groupContainerChild).request().method(PATCH,
                        entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot control a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot control a group-controlled resource")
        public void testCanControlGroupResource() {
            try (final Response res = target(groupContainerAcl).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot control the child of a group-controlled resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot control the child of a group-controlled resource")
        public void testCanControlGroupResourceChild() {
            try (final Response res = target(groupContainerChild + EXT_ACL).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user can read a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user can read a default ACL resource")
        public void testCanReadDefaultAclResource() {
            try (final Response res = target(defaultContainer).request().get()) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
            }
        }

        /**
         * Verify that an anonymous user cannot read the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot read the child of a default ACL resource")
        public void testCanReadDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot append to a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot append to a default ACL resource")
        public void testUserCanAppendDefaultAclResource() {
            try (final Response res = target(defaultContainer).request().post(entity("", TEXT_TURTLE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to a default ACL resource")
        public void testCanWriteDefaultAclResource() {
            try (final Response res = target(defaultContainer).request().method(PATCH,
                        entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that an anonymous user cannot write to the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that an anonymous user cannot write to the child of a default ACL resource")
        public void testCanWriteDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild).request().method(PATCH,
                        entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control a default ACL resource")
        public void testCanControlDefaultAclResource() {
            try (final Response res = target(defaultContainerAcl).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }

        /**
         * Verify that a user cannot control the child of a default ACL resource.
         */
        @Test
        @DisplayName("Verify that a user cannot control the child of a default ACL resource")
        public void testCanControlDefaultAclResourceChild() {
            try (final Response res = target(defaultContainerChild + EXT_ACL).request().get()) {
                assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
            }
        }
    }
}
