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
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.trellisldp.http.domain.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.domain.RdfMediaType.TEXT_TURTLE;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Anonymous auth tests.
 */
public interface AuthAnonymousTests extends AuthCommonTests {

    /**
     * Verify that an anonymous user can read a public resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user can read a public resource")
    default void testCanReadPublicResource() {
        try (final Response res = target(getPublicContainer()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an anonymous user can read the child of a public resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user can read the child of a public resource")
    default void testCanReadPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an anonymous user cannot append to a public resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot append to a public resource")
    default void testUserCanAppendPublicResource() {
        try (final Response res = target(getPublicContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to a public resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to a public resource")
    default void testCanWritePublicResource() {
        try (final Response res = target(getPublicContainer()).request().method(PATCH, entity(INSERT_PROP_BAR,
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a public resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to the child of a public resource")
    default void testCanWritePublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot control a public resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot control a public resource")
    default void testCanControlPublicResource() {
        try (final Response res = target(getPublicContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot control the child of a public resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot control the child of a public resource")
    default void testCanControlPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot read a protected resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot read a protected resource")
    default void testCanReadProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot read the child of a protected resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot read the child of a protected resource")
    default void testCanReadProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot append to a protected resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot append to a protected resource")
    default void testUserCanAppendProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to a protected resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to a protected resource")
    default void testCanWriteProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request().method(PATCH,
                    entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a protected resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to the child of a protected resource")
    default void testCanWriteProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot control a protected resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot control a protected resource")
    default void testCanControlProtectedResource() {
        try (final Response res = target(getProtectedContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot control the child of a protected resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot control the child of a protected resource")
    default void testCanControlProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot read a private resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot read a private resource")
    default void testCanReadPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot read the child of a private resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot read the child of a private resource")
    default void testCanReadPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot append to a private resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot append to a private resource")
    default void testUserCanAppendPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to a private resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to a private resource")
    default void testCanWritePrivateResource() {
        try (final Response res = target(getPrivateContainer()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a private resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to the child of a private resource")
    default void testCanWritePrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot control a private resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot control a private resource")
    default void testCanControlPrivateResource() {
        try (final Response res = target(getPrivateContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot control the child of a private resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot control the child of a private resource")
    default void testCanControlPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot read a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot read a group-controlled resource")
    default void testCanReadGroupResource() {
        try (final Response res = target(getGroupContainer()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot read the child of a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot read the child of a group-controlled resource")
    default void testCanReadGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot append to a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot append to a group-controlled resource")
    default void testUserCanAppendGroupResource() {
        try (final Response res = target(getGroupContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to a group-controlled resource")
    default void testCanWriteGroupResource() {
        try (final Response res = target(getGroupContainer()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to the child of a group-controlled resource")
    default void testCanWriteGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot control a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot control a group-controlled resource")
    default void testCanControlGroupResource() {
        try (final Response res = target(getGroupContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot control the child of a group-controlled resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot control the child of a group-controlled resource")
    default void testCanControlGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user can read a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user can read a default ACL resource")
    default void testCanReadDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily());
        }
    }

    /**
     * Verify that an anonymous user cannot read the child of a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot read the child of a default ACL resource")
    default void testCanReadDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot append to a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot append to a default ACL resource")
    default void testUserCanAppendDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to a default ACL resource")
    default void testCanWriteDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a default ACL resource.
     */
    @Test
    @DisplayName("Verify that an anonymous user cannot write to the child of a default ACL resource")
    default void testCanWriteDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that a user cannot control a default ACL resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control a default ACL resource")
    default void testCanControlDefaultAclResource() {
        try (final Response res = target(getDefaultContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }

    /**
     * Verify that a user cannot control the child of a default ACL resource.
     */
    @Test
    @DisplayName("Verify that a user cannot control the child of a default ACL resource")
    default void testCanControlDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()));
        }
    }
}

