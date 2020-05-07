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
package org.trellisldp.test;

import static java.net.URI.create;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.trellisldp.http.core.HttpConstants.SLUG;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;
import static org.trellisldp.test.TestUtils.buildJwt;
import static org.trellisldp.test.TestUtils.getLinks;
import static org.trellisldp.test.TestUtils.getResourceAsString;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.trellisldp.vocabulary.LDP;

/**
 * A convenience class for running the Auth tests.
 */
public abstract class AbstractApplicationAuthTests {

    /**
     * Get the HTTP client.
     * @return the HTTP client
     */
    public abstract Client getClient();

    /**
     * Get the baseURL for the LDP server.
     * @return the base URL
     */
    public abstract String getBaseURL();

    /**
     * Get the JWT secret.
     * @return the JWT secret
     */
    public abstract String getJwtSecret();

    /**
     * Get the credentials for the first user.
     * @return the credentials
     */
    public abstract String getUser1Credentials();

    /**
     * Get the credentials for the second user.
     * @return the credentials
     */
    public abstract String getUser2Credentials();

    /**
     * Get the WebID for an admin-level user.
     * @return the admin webid
     */
    public abstract String getAdminWebId();

    public class AdministratorTests extends BasicTests implements AuthAdministratorTests {

        @Override
        public String getAuthorizationHeader() {
            return buildJwt(getAdminWebId(), AbstractApplicationAuthTests.this.getJwtSecret());
        }
    }

    public class UserTests extends BasicTests implements AuthUserTests {

        @Override
        public String getAuthorizationHeader() {
            return buildJwt("https://people.apache.org/~acoburn/#i",
                    AbstractApplicationAuthTests.this.getJwtSecret());
        }
    }

    public class UserBasicAuthTests extends BasicTests implements AuthUserTests {
        @Override
        public String getAuthorizationHeader() {
            return "Basic " + encodeBase64String(AbstractApplicationAuthTests.this.getUser1Credentials().getBytes());
        }
    }

    public class OtherUserTests extends BasicTests implements AuthOtherUserTests {
        @Override
        public String getAuthorizationHeader() {
            return buildJwt("https://madison.example.com/profile/#me",
                    AbstractApplicationAuthTests.this.getJwtSecret());
        }
    }

    public class OtherUserBasicAuthTests extends BasicTests implements AuthOtherUserTests {
        @Override
        public String getAuthorizationHeader() {
            return "Basic " + encodeBase64String(AbstractApplicationAuthTests.this.getUser2Credentials().getBytes());
        }
    }

    public class AnonymousTests extends BasicTests implements AuthAnonymousTests {
        @Override
        public String getAuthorizationHeader() {
            return null;
        }
    }

    @Test
    @DisplayName("Administrator JWT Auth tests")
    public void testAdminJwtAuth() {
        final AdministratorTests tests = new AdministratorTests();
        tests.setUp();
        assertAll("Test administrator authentication features with JWT", tests.runTests());
    }

    @Test
    @DisplayName("User JWT Auth tests")
    public void testUserJwtAuth() {
        final UserTests tests = new UserTests();
        tests.setUp();
        assertAll("Test user authentication features with JWT", tests.runTests());
    }

    @Test
    @DisplayName("User Basic Auth tests")
    public void testUserBasicAuth() {
        final UserBasicAuthTests tests = new UserBasicAuthTests();
        tests.setUp();
        assertAll("Test user basic authentication features", tests.runTests());
    }

    @Test
    @DisplayName("Other user JWT Auth tests")
    public void testOtherUserJwtAuth() {
        final OtherUserTests tests = new OtherUserTests();
        tests.setUp();
        assertAll("Test other user authentication features", tests.runTests());
    }

    @Test
    @DisplayName("Other user Basic Auth tests")
    public void testOtherUserBasicAuth() {
        final OtherUserBasicAuthTests tests = new OtherUserBasicAuthTests();
        tests.setUp();
        assertAll("Test other user basic authentication features", tests.runTests());
    }

    @Test
    @DisplayName("Anonymous Auth tests")
    public void testAnonymousAuth() {
        final AnonymousTests tests = new AnonymousTests();
        tests.setUp();
        assertAll("Test anonymous user authentication features", tests.runTests());
    }

    private abstract class BasicTests implements AuthCommonTests {

        private String publicContainer;
        private String publicContainerChild;
        private String protectedContainer;
        private String protectedContainerChild;
        private String privateContainer;
        private String privateContainerChild;
        private String groupContainer;
        private String groupContainerChild;
        private String defaultContainer;
        private String defaultContainerChild;

        @Override
        public String getBaseURL() {
            return AbstractApplicationAuthTests.this.getBaseURL();
        }

        @Override
        public Client getClient() {
            return AbstractApplicationAuthTests.this.getClient();
        }

        @Override
        public String getPublicContainer() {
            return publicContainer;
        }

        @Override
        public String getPublicContainerChild() {
            return publicContainerChild;
        }

        @Override
        public String getProtectedContainer() {
            return protectedContainer;
        }

        @Override
        public String getProtectedContainerChild() {
            return protectedContainerChild;
        }

        @Override
        public String getPrivateContainer() {
            return privateContainer;
        }

        @Override
        public String getPrivateContainerChild() {
            return privateContainerChild;
        }

        @Override
        public String getDefaultContainer() {
            return defaultContainer;
        }

        @Override
        public String getDefaultContainerChild() {
            return defaultContainerChild;
        }

        @Override
        public String getGroupContainer() {
            return groupContainer;
        }

        @Override
        public String getGroupContainerChild() {
            return groupContainerChild;
        }

        private void setPublicContainer(final String location) {
            this.publicContainer = location;
        }

        private void setPublicContainerChild(final String location) {
            this.publicContainerChild = location;
        }

        private void setProtectedContainer(final String location) {
            this.protectedContainer = location;
        }

        private void setProtectedContainerChild(final String location) {
            this.protectedContainerChild = location;
        }

        private void setPrivateContainer(final String location) {
            this.privateContainer = location;
        }

        private void setPrivateContainerChild(final String location) {
            this.privateContainerChild = location;
        }

        private void setDefaultContainer(final String location) {
            this.defaultContainer = location;
        }

        private void setDefaultContainerChild(final String location) {
            this.defaultContainerChild = location;
        }

        private void setGroupContainer(final String location) {
            this.groupContainer = location;
        }

        private void setGroupContainerChild(final String location) {
            this.groupContainerChild = location;
        }

        public void setUp() {
            final String acl = "acl";
            final String prefixAcl = "PREFIX acl: <http://www.w3.org/ns/auth/acl#>\n\n";
            final String jwt = buildJwt(getAdminWebId(), AbstractApplicationAuthTests.this.getJwtSecret());

            final String containerContent = getResourceAsString("/basicContainer.ttl");
            final String container;
            final String groupResource;

            // POST an LDP-BC
            try (final Response res = target().request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .header(SLUG, generateRandomValue(getClass().getSimpleName()))
                    .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check response for Auth container");
                container = res.getLocation().toString();
            }

            // Add an ACL for this container, with no permissions
            final String rootAcl = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + prefixAcl
                + "INSERT DATA { [ acl:accessTo <" + container + ">; acl:agentClass foaf:Agent; \n"
                + "    acl:default <" + container + ">]}";

            // Add an ACL for the quasi-root container
            try (final Response res = target(container + EXT_ACL).request().header(AUTHORIZATION, jwt)
                    .method(PATCH, entity(rootAcl, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check response for ACL to 'root' resource");
            }

            // POST a public container
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check POST response for 'public' container");
                setPublicContainer(res.getLocation().toString());
            }

            // Add a child to the public container
            try (final Response res = target(publicContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check response for public child");
                setPublicContainerChild(res.getLocation().toString());
                final String publicContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(acl))
                    .map(link -> link.getUri().toString()).findFirst().orElse("");
                assertEquals(getPublicContainer() + EXT_ACL,
                        create(getPublicContainer()).resolve(publicContainerAcl).toString(),
                        "Check ACL location for 'public'");
            }

            final String publicAcl = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + prefixAcl
                + "INSERT DATA { [acl:accessTo <" + publicContainer + ">; acl:mode acl:Read; "
                + "   acl:agentClass foaf:Agent; acl:default <" + publicContainer + "> ] }; \n"
                + prefixAcl
                + "INSERT DATA { [acl:accessTo <" + publicContainer + ">; acl:mode acl:Read, acl:Write;"
                + "   acl:agentClass acl:AuthenticatedAgent; acl:default <" + publicContainer + ">] }";

            // Add an ACL for the public container
            try (final Response res = target(getPublicContainer() + EXT_ACL).request().header(AUTHORIZATION, jwt)
                    .method(PATCH, entity(publicAcl, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check response creating ACL for 'public'");
            }

            // POST a protected container
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check response for 'protected' container");
                setProtectedContainer(res.getLocation().toString());
            }

            // Add a child to the protected container
            try (final Response res = target(protectedContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check add protected child");
                setProtectedContainerChild(res.getLocation().toString());
                final String protectedContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(acl))
                    .map(link -> link.getUri().toString()).findFirst().orElse("");
                assertEquals(getProtectedContainer() + EXT_ACL,
                        create(protectedContainer).resolve(protectedContainerAcl).toString(),
                        "Check 'protected' ACL URL");
            }

            final String protectedAcl = prefixAcl
                + "INSERT DATA { \n"
                + "[acl:accessTo  <" + protectedContainer + ">;  acl:mode acl:Read, acl:Write;"
                + "   acl:agent <https://people.apache.org/~acoburn/#i>; acl:default <" + protectedContainer + "> ] };"
                + prefixAcl
                + "INSERT DATA { \n"
                + "[acl:accessTo  <" + protectedContainer + ">; acl:mode acl:Read, acl:Append; "
                + "   acl:agent <https://madison.example.com/profile/#me>; "
                + "   acl:default <" + protectedContainer + "> ] }";

            // Add an ACL for the protected container
            try (final Response res = target(getProtectedContainer() + EXT_ACL).request().header(AUTHORIZATION, jwt)
                    .method(PATCH, entity(protectedAcl, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check 'protected' ACL creation");
            }

            // POST a private container
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Create 'private' container");
                setPrivateContainer(res.getLocation().toString());
            }

            // Add a child to the private container
            try (final Response res = target(privateContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Add 'private' child");
                setPrivateContainerChild(res.getLocation().toString());
                final String privateContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(acl))
                    .map(link -> link.getUri().toString()).findFirst().orElse("");
                assertEquals(getPrivateContainer() + EXT_ACL,
                        create(privateContainer).resolve(privateContainerAcl).toString(), "Check 'private' ACL URL");
            }

            final String privateAcl = prefixAcl
                + "INSERT DATA { "
                + "[acl:accessTo  <" + privateContainer + ">; acl:mode acl:Read, acl:Write; "
                + "   acl:agent <http://example.com/administrator>; acl:default <" + privateContainer + "> ] }";

            // Add an ACL for the private container
            try (final Response res = target(getPrivateContainer() + EXT_ACL).request().header(AUTHORIZATION, jwt)
                    .method(PATCH, entity(privateAcl, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check response for 'private' container");
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
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check 'group' resource");
                groupResource = res.getLocation().toString();
            }

            // POST a group-controlled container
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check 'group' container");
                setGroupContainer(res.getLocation().toString());
            }

            // Add a child to the group container
            try (final Response res = target(groupContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check 'group' child");
                setGroupContainerChild(res.getLocation().toString());
                final String groupContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(acl))
                    .map(link -> link.getUri().toString()).findFirst().orElse("");
                assertEquals(getGroupContainer() + EXT_ACL,
                        create(groupContainer).resolve(groupContainerAcl).toString(), "Check 'group' ACL URL");
            }

            final String groupAcl = prefixAcl
                + "INSERT DATA {  "
                + "[acl:accessTo <" + groupContainer + ">; acl:mode acl:Read, acl:Write; "
                + " acl:agentGroup <" + groupResource + "#Developers>; acl:default <" + groupContainer + "> ] };\n"
                + prefixAcl
                + "INSERT DATA {  "
                + "[acl:accessTo <" + groupContainer + ">; acl:mode acl:Read; "
                + " acl:agentGroup <" + groupResource + "#Management>; acl:default <" + groupContainer + "> ] }";

            // Add an ACL for the private container
            try (final Response res = target(groupContainer + EXT_ACL).request().header(AUTHORIZATION, jwt)
                    .method(PATCH, entity(groupAcl, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check create 'private' ACL");
            }

            // POST a container with a default ACL
            try (final Response res = target(container).request()
                    .header(LINK, fromUri(LDP.BasicContainer.getIRIString()).rel(TYPE).build())
                    .header(AUTHORIZATION, jwt).post(entity(containerContent, TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check 'default' ACL container");
                defaultContainer = res.getLocation().toString();
            }

            // Add a child to the public container
            try (final Response res = target(defaultContainer).request().header(AUTHORIZATION, jwt)
                    .post(entity("", TEXT_TURTLE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check add 'default' child");
                defaultContainerChild = res.getLocation().toString();
                final String defaultContainerAcl = getLinks(res).stream().filter(link -> link.getRel().equals(acl))
                    .map(link -> link.getUri().toString()).findFirst().orElse("");
                assertEquals(getDefaultContainer() + EXT_ACL,
                        create(defaultContainer).resolve(defaultContainerAcl).toString(), "Check 'default' ACL URL");
            }

            final String defaultAcl = "PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n"
                + prefixAcl
                + "INSERT DATA {  "
                + "[acl:accessTo <" + defaultContainer + ">; acl:mode acl:Read; acl:agentClass foaf:Agent ] }; \n"
                + prefixAcl
                + "INSERT DATA { [acl:accessTo <" + defaultContainer + ">; acl:mode acl:Read, acl:Write; \n"
                + "   acl:agent <https://people.apache.org/~acoburn/#i> ] }";

            // Add an ACL for the public container
            try (final Response res = target(getDefaultContainer() + EXT_ACL).request().header(AUTHORIZATION, jwt)
                    .method(PATCH, entity(defaultAcl, APPLICATION_SPARQL_UPDATE))) {
                assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), "Check 'public' ACL");
            }
        }
    }
}
