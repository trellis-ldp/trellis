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

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Other (non-privileged) user authorization tests.
 */
public interface AuthOtherUserTests extends AuthCommonTests {

    /**
     * Verify that a user can read a public resource.
     */
    @Test
    @DisplayName("Verify that a user can read a public resource")
    default void testUserCanReadPublicResource() {
        try (final Response res = target(getPublicContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can read the child of a public resource.
     */
    @Test
    @DisplayName("Verify that a user can read the child of a public resource")
    default void testUserCanReadPublicResourceChile() {
        try (final Response res = target(getPublicContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can append to a public resource.
     */
    @Test
    @DisplayName("Verify that a user can append to a public resource")
    default void testUserCanAppendPublicResource() {
        try (final Response res = target(getPublicContainer()).request().header(AUTHORIZATION, getAuthorizationHeader())
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can write to a public resource.
     */
    @Test
    @DisplayName("Verify that a user can write to a public resource")
    default void testUserCanWritePublicResource() {
        try (final Response res = target(getPublicContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can write to the child of a public resource.
     */
    @Test
    @DisplayName("Verify that a user can write to the child of a public resource")
    default void testUserCanWritePublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a public resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control a public resource")
    default void testUserCanControlPublicResource() {
        try (final Response res = target(getPublicContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a public resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control the child of a public resource")
    default void testUserCanControlPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user can read a protected resource.
     */
    @Test
    @DisplayName("Verify that a user can read a protected resource")
    default void testUserCanReadProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can read the child of a protected resource.
     */
    @Test
    @DisplayName("Verify that a user can read the child of a protected resource")
    default void testUserCanReadProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can append to a public resource.
     */
    @Test
    @DisplayName("Verify that a user can append to a public resource")
    default void testUserCanAppendProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to a protected resource.
     */
    @Test
    @DisplayName("Verify that a user cannot write to a protected resource")
    default void testUserCanWriteProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to the child of a protected resource.
     */
    @Test
    @DisplayName("Verify that a user cannot write to the child of a protected resource")
    default void testUserCanWriteProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a protected resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control a protected resource")
    default void testUserCanControlProtectedResource() {
        try (final Response res = target(getProtectedContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a protected resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control the child of a protected resource")
    default void testUserCanControlProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot read a private resource.
     */
    @Test
    @DisplayName("Verify that a user cannot read a private resource")
    default void testUserCanReadPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot read the child of a private resource.
     */
    @Test
    @DisplayName("Verify that a user cannot read the child of a private resource")
    default void testUserCanReadPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot append to a private resource.
     */
    @Test
    @DisplayName("Verify that a user cannot append to a private resource")
    default void testUserCanAppendPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).post(entity("", TEXT_TURTLE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to a private resource.
     */
    @Test
    @DisplayName("Verify that a user cannot write to a private resource")
    default void testUserCanWritePrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to the child of a private resource.
     */
    @Test
    @DisplayName("Verify that a user cannot write to the child of a private resource")
    default void testUserCanWritePrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a private resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control a private resource")
    default void testUserCanControlPrivateResource() {
        try (final Response res = target(getPrivateContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a private resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control the child of a private resource")
    default void testUserCanControlPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user can read a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that a user can read a group-controlled resource")
    default void testCanReadGroupResource() {
        try (final Response res = target(getGroupContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can read the child of a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that a user can read the child of a group-controlled resource")
    default void testCanReadGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot append to a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that a user cannot append to a group-controlled resource")
    default void testUserCanAppendGroupResource() {
        try (final Response res = target(getGroupContainer()).request().header(AUTHORIZATION, getAuthorizationHeader())
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that a user cannot write to a group-controlled resource")
    default void testCanWriteGroupResource() {
        try (final Response res = target(getGroupContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to the child of a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that a user cannot write to the child of a group-controlled resource")
    default void testCanWriteGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control a group-controlled resource")
    default void testCanControlGroupResource() {
        try (final Response res = target(getGroupContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control the child of a group-controlled resource")
    default void testCanControlGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user can read a default ACL resource.
     */
    @Test
    @DisplayName("Verify that a user can read a default ACL resource")
    default void testCanReadDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot read the child of a default ACL resource.
     */
    @Test
    @DisplayName("Verify that a user cannot read the child of a default ACL resource")
    default void testCanReadDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot append to a default ACL resource.
     */
    @Test
    @DisplayName("Verify that a user cannot append to a default ACL resource")
    default void testUserCanAppendDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).post(entity("", TEXT_TURTLE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to a default ACL resource.
     */
    @Test
    @DisplayName("Verify that a user cannot write to a default ACL resource")
    default void testCanWriteDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to the child of a default ACL resource.
     */
    @Test
    @DisplayName("Verify that a user cannot write to the child of a default ACL resource")
    default void testCanWriteDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a default ACL resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control a default ACL resource")
    default void testCanControlDefaultAclResource() {
        try (final Response res = target(getDefaultContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a default ACL resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control the child of a default ACL resource")
    default void testCanControlDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }
}
