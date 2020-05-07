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

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.UNAUTHORIZED;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;

import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.function.Executable;

/**
 * Anonymous auth tests.
 */
public interface AuthAnonymousTests extends AuthCommonTests {

    /**
     * Run the tests.
     * @return the tests
     */
    default Stream<Executable> runTests() {
        return Stream.of(this::testCanReadPublicResource,
                this::testCanReadPublicResourceChild,
                this::testUserCanAppendPublicResource,
                this::testCanWritePublicResource,
                this::testCanWritePublicResourceChild,
                this::testCanControlPublicResource,
                this::testCanControlPublicResourceChild,
                this::testCanReadProtectedResource,
                this::testCanReadProtectedResourceChild,
                this::testUserCanAppendProtectedResource,
                this::testCanWriteProtectedResource,
                this::testCanWriteProtectedResourceChild,
                this::testCanControlProtectedResource,
                this::testCanControlProtectedResourceChild,
                this::testCanReadPrivateResource,
                this::testCanReadPrivateResourceChild,
                this::testUserCanAppendPrivateResource,
                this::testCanWritePrivateResource,
                this::testCanWritePrivateResourceChild,
                this::testCanControlPrivateResource,
                this::testCanControlPrivateResourceChild,
                this::testCanReadGroupResource,
                this::testCanReadGroupResourceChild,
                this::testUserCanAppendGroupResource,
                this::testCanWriteGroupResource,
                this::testCanWriteGroupResourceChild,
                this::testCanControlGroupResource,
                this::testCanControlGroupResourceChild,
                this::testCanReadDefaultAclResource,
                this::testCanReadDefaultAclResourceChild,
                this::testUserCanAppendDefaultAclResource,
                this::testCanWriteDefaultAclResource,
                this::testCanWriteDefaultAclResourceChild,
                this::testCanControlDefaultAclResource,
                this::testCanControlDefaultAclResourceChild);
    }

    /**
     * Verify that an anonymous user can read a public resource.
     */
    default void testCanReadPublicResource() {
        try (final Response res = target(getPublicContainer()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user can read the child of a public resource.
     */
    default void testCanReadPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot append to a public resource.
     */
    default void testUserCanAppendPublicResource() {
        try (final Response res = target(getPublicContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to a public resource.
     */
    default void testCanWritePublicResource() {
        try (final Response res = target(getPublicContainer()).request().method(PATCH, entity(INSERT_PROP_BAR,
                        APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a public resource.
     */
    default void testCanWritePublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot control a public resource.
     */
    default void testCanControlPublicResource() {
        try (final Response res = target(getPublicContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot control the child of a public resource.
     */
    default void testCanControlPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot read a protected resource.
     */
    default void testCanReadProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot read the child of a protected resource.
     */
    default void testCanReadProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot append to a protected resource.
     */
    default void testUserCanAppendProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to a protected resource.
     */
    default void testCanWriteProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request().method(PATCH,
                    entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a protected resource.
     */
    default void testCanWriteProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot control a protected resource.
     */
    default void testCanControlProtectedResource() {
        try (final Response res = target(getProtectedContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot control the child of a protected resource.
     */
    default void testCanControlProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot read a private resource.
     */
    default void testCanReadPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot read the child of a private resource.
     */
    default void testCanReadPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot append to a private resource.
     */
    default void testUserCanAppendPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to a private resource.
     */
    default void testCanWritePrivateResource() {
        try (final Response res = target(getPrivateContainer()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a private resource.
     */
    default void testCanWritePrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot control a private resource.
     */
    default void testCanControlPrivateResource() {
        try (final Response res = target(getPrivateContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot control the child of a private resource.
     */
    default void testCanControlPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot read a group-controlled resource.
     */
    default void testCanReadGroupResource() {
        try (final Response res = target(getGroupContainer()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot read the child of a group-controlled resource.
     */
    default void testCanReadGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot append to a group-controlled resource.
     */
    default void testUserCanAppendGroupResource() {
        try (final Response res = target(getGroupContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to a group-controlled resource.
     */
    default void testCanWriteGroupResource() {
        try (final Response res = target(getGroupContainer()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a group-controlled resource.
     */
    default void testCanWriteGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot control a group-controlled resource.
     */
    default void testCanControlGroupResource() {
        try (final Response res = target(getGroupContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot control the child of a group-controlled resource.
     */
    default void testCanControlGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user can read a non-inheriting ACL resource.
     */
    default void testCanReadDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request().get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot read the child of a resource with no
     * default ACL inheritance.
     */
    default void testCanReadDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot append to a default ACL resource.
     */
    default void testUserCanAppendDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request().post(entity("", TEXT_TURTLE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to a default ACL resource.
     */
    default void testCanWriteDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that an anonymous user cannot write to the child of a default ACL resource.
     */
    default void testCanWriteDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request().method(PATCH,
                    entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a default ACL resource.
     */
    default void testCanControlDefaultAclResource() {
        try (final Response res = target(getDefaultContainer() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a default ACL resource.
     */
    default void testCanControlDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild() + EXT_ACL).request().get()) {
            assertEquals(UNAUTHORIZED, fromStatusCode(res.getStatus()), UNAUTHORIZED_RESPONSE);
        }
    }
}
