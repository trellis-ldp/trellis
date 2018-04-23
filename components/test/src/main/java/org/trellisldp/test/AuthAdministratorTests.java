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
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Admin Authorization tests.
 *
 * @author acoburn
 */
public interface AuthAdministratorTests extends AuthCommonTests {

    /**
     * Verify that an administrator can read a public resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read a public resource")
    default void testAdminCanReadPublicResource() {
        try (final Response res = target(getPublicContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can read the child of a public resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read the child of a public resource")
    default void testAdminCanReadPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to a public resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to a public resource")
    default void testAdminCanWritePublicResource() {
        try (final Response res = target(getPublicContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to the child of a public resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to the child of a public resource")
    default void testAdminCanWritePublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can control a public resource.
     */
    @Test
    @DisplayName("Verify that an administrator can control a public resource")
    default void testAdminCanControlPublicResource() {
        try (final Response res = target(getPublicContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can control the child of a public resource.
     */
    @Test
    @DisplayName("Verify that an administrator can control the child of a public resource")
    default void testAdminCanControlPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an administrator can read a protected resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read a protected resource")
    default void testAdminCanReadProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can read the child of a protected resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read the child of a protected resource")
    default void testAdminCanReadProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to a protected resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to a protected resource")
    default void testAdminCanWriteProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to the child of a protected resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to the child of a protected resource")
    default void testAdminCanWriteProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can control a protected resource.
     */
    @Test
    @DisplayName("Verify that an administrator can control a protected resource")
    default void testAdminCanControlProtectedResource() {
        try (final Response res = target(getProtectedContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can control the child of a protected resource.
     */
    @Test
    @DisplayName("Verify that an administrator can control the child of a protected resource")
    default void testAdminCanControlProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an administrator can read a private resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read a private resource")
    default void testAdminCanReadPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can read the child of a private resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read the child of a private resource")
    default void testAdminCanReadPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to a private resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to a private resource")
    default void testAdminCanWritePrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to the child of a private resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to the child of a private resource")
    default void testAdminCanWritePrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can control a private resource.
     */
    @Test
    @DisplayName("Verify that an administrator can control a private resource")
    default void testAdminCanControlPrivateResource() {
        try (final Response res = target(getPrivateContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can control the child of a private resource.
     */
    @Test
    @DisplayName("Verify that an administrator can control the child of a private resource")
    default void testAdminCanControlPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an administrator can read a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read a group-controlled resource")
    default void testAdminCanReadGroupResource() {
        try (final Response res = target(getGroupContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can read the child of a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read the child of a group-controlled resource")
    default void testAdminCanReadGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to a group-controlled resource")
    default void testAdminCanWriteGroupResource() {
        try (final Response res = target(getGroupContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to the child of a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to the child of a group-controlled resource")
    default void testAdminCanWriteGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can control a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an administrator can control a group-controlled resource")
    default void testAdminCanControlGroupResource() {
        try (final Response res = target(getGroupContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can't find the ACL of a child resource.
     */
    @Test
    @DisplayName("Verify that an administrator can't find the ACL of a child resource")
    default void testAdminCanControlGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an administrator can read a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read a default ACL resource")
    default void testCanReadDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can read the child of a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an administrator can read the child of a default ACL resource")
    default void testCanReadDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to a default ACL resource")
    default void testCanWriteDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can write to the child of a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an administrator can write to the child of a default ACL resource")
    default void testCanWriteDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can control a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an administrator can control a default ACL resource")
    default void testCanControlDefaultAclResource() {
        try (final Response res = target(getDefaultContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an administrator can't find the ACL resource.
     */
    @Test
    @DisplayName("Verify that an administrator can't find the ACL resource")
    default void testCanControlDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(NOT_FOUND, fromStatusCode(res.getStatus()));
        }
    }
}
