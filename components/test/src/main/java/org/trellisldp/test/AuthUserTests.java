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
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.Response.Status.fromStatusCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.trellisldp.http.core.RdfMediaType.APPLICATION_SPARQL_UPDATE;
import static org.trellisldp.http.core.RdfMediaType.TEXT_TURTLE;

import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.function.Executable;

/**
 * User Authorization tests.
 *
 * @author acoburn
 */
public interface AuthUserTests extends AuthCommonTests {

    /**
     * Run the tests.
     * @return the tests
     */
    default Stream<Executable> runTests() {
        return Stream.of(this::testUserCanReadPublicResource,
                this::testUserCanReadPublicResourceChild,
                this::testUserCanAppendPublicResource,
                this::testUserCanWritePublicResource,
                this::testUserCanWritePublicResourceChild,
                this::testUserCannotControlPublicResource,
                this::testUserCannotControlPublicResourceChild,
                this::testUserCanReadProtectedResource,
                this::testUserCanReadProtectedResourceChild,
                this::testUserCanAppendProtectedResource,
                this::testUserCanWriteProtectedResource,
                this::testUserCanWriteProtectedResourceChild,
                this::testUserCannotControlProtectedResource,
                this::testUserCannotControlProtectedResourceChild,
                this::testUserCannotReadPrivateResource,
                this::testUserCannotReadPrivateResourceChild,
                this::testUserCannotAppendPrivateResource,
                this::testUserCannotWritePrivateResource,
                this::testUserCannotWritePrivateResourceChild,
                this::testUserCannotControlPrivateResource,
                this::testUserCannotControlPrivateResourceChild,
                this::testUserCanReadGroupResource,
                this::testUserCanReadGroupResourceChild,
                this::testUserCanWriteGroupResource,
                this::testUserCanWriteGroupResourceChild,
                this::testUserCannotControlGroupResource,
                this::testUserCannotControlGroupResourceChild,
                this::testUserCanReadDefaultAclResource,
                this::testUserCannotReadDefaultAclResourceChild,
                this::testUserCanWriteDefaultAclResource,
                this::testUserCannotWriteDefaultAclResourceChild,
                this::testUserCannotControlDefaultAclResource,
                this::testUserCannotControlDefaultAclResourceChild);
    }

    /**
     * Verify that a user can read a public resource.
     */
    default void testUserCanReadPublicResource() {
        try (final Response res = target(getPublicContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can read the child of a public resource.
     */
    default void testUserCanReadPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can append to a public resource.
     */
    default void testUserCanAppendPublicResource() {
        try (final Response res = target(getPublicContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can write to a public resource.
     */
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
    default void testUserCannotControlPublicResource() {
        try (final Response res = target(getPublicContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a public resource.
     */
    default void testUserCannotControlPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user can read a protected resource.
     */
    default void testUserCanReadProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can read the child of a protected resource.
     */
    default void testUserCanReadProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can append to a protected resource.
     */
    default void testUserCanAppendProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can write to a protected resource.
     */
    default void testUserCanWriteProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can write to the child of a protected resource.
     */
    default void testUserCanWriteProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a protected resource.
     */
    default void testUserCannotControlProtectedResource() {
        try (final Response res = target(getProtectedContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a protected resource.
     */
    default void testUserCannotControlProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot read a private resource.
     */
    default void testUserCannotReadPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot read the child of a private resource.
     */
    default void testUserCannotReadPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot append to a private resource.
     */
    default void testUserCannotAppendPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).post(entity("", TEXT_TURTLE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to a private resource.
     */
    default void testUserCannotWritePrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to the child of a private resource.
     */
    default void testUserCannotWritePrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a private resource.
     */
    default void testUserCannotControlPrivateResource() {
        try (final Response res = target(getPrivateContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a private resource.
     */
    default void testUserCannotControlPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user can read a group-controlled resource.
     */
    default void testUserCanReadGroupResource() {
        try (final Response res = target(getGroupContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can read the child of a group-controlled resource.
     */
    default void testUserCanReadGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can write to a group-controlled resource.
     */
    default void testUserCanWriteGroupResource() {
        try (final Response res = target(getGroupContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user can write to the child of a group-controlled resource.
     */
    default void testUserCanWriteGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a group-controlled resource.
     */
    default void testUserCannotControlGroupResource() {
        try (final Response res = target(getGroupContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a group-controlled resource.
     */
    default void testUserCannotControlGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user can read a non-inheritable ACL resource.
     */
    default void testUserCanReadDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot read the child of a non-inheritable ACL resource.
     */
    default void testUserCannotReadDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user can write to a non-inheritable ACL resource.
     */
    default void testUserCanWriteDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot write to the child of a non-inheritable ACL resource.
     */
    default void testUserCannotWriteDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control a non-inheritable ACL resource.
     */
    default void testUserCannotControlDefaultAclResource() {
        try (final Response res = target(getDefaultContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that a user cannot control the child of a non-inheritable ACL resource.
     */
    default void testUserCannotControlDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }
}
