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
 * Other (non-privileged) user authorization tests.
 */
public interface AuthOtherUserTests extends AuthCommonTests {

    /**
     * Run the tests.
     * @return the tests
     */
    default Stream<Executable> runTests() {
        return Stream.of(this::testOtherUserCanReadPublicResource,
                this::testOtherUserCanReadPublicResourceChild,
                this::testOtherUserCanAppendPublicResource,
                this::testOtherUserCanWritePublicResource,
                this::testOtherUserCanWritePublicResourceChild,
                this::testOtherUserCanControlPublicResource,
                this::testOtherUserCanControlPublicResourceChild,
                this::testOtherUserCanReadProtectedResource,
                this::testOtherUserCanReadProtectedResourceChild,
                this::testOtherUserCanAppendProtectedResource,
                this::testOtherUserCanWriteProtectedResource,
                this::testOtherUserCanWriteProtectedResourceChild,
                this::testOtherUserCanControlProtectedResource,
                this::testOtherUserCanControlProtectedResourceChild,
                this::testOtherUserCanReadPrivateResource,
                this::testOtherUserCanReadPrivateResourceChild,
                this::testOtherUserCanAppendPrivateResource,
                this::testOtherUserCanWritePrivateResource,
                this::testOtherUserCanWritePrivateResourceChild,
                this::testOtherUserCanControlPrivateResource,
                this::testOtherUserCanControlPrivateResourceChild,
                this::testOtherUserCanReadGroupResource,
                this::testOtherUserCanReadGroupResourceChild,
                this::testOtherUserCanAppendGroupResource,
                this::testOtherUserCanWriteGroupResource,
                this::testOtherUserCanWriteGroupResourceChild,
                this::testOtherUserCanControlGroupResource,
                this::testOtherUserCanControlGroupResourceChild,
                this::testOtherUserCanReadDefaultAclResource,
                this::testOtherUserCanReadDefaultAclResourceChild,
                this::testOtherUserCanAppendDefaultAclResource,
                this::testOtherUserCanWriteDefaultAclResource,
                this::testOtherUserCanWriteDefaultAclResourceChild,
                this::testOtherUserCanControlDefaultAclResource,
                this::testOtherUserCanControlDefaultAclResourceChild);
    }

    /**
     * Verify that another user can read a public resource.
     */
    default void testOtherUserCanReadPublicResource() {
        try (final Response res = target(getPublicContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user can read the child of a public resource.
     */
    default void testOtherUserCanReadPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user can append to a public resource.
     */
    default void testOtherUserCanAppendPublicResource() {
        try (final Response res = target(getPublicContainer()).request().header(AUTHORIZATION, getAuthorizationHeader())
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user can write to a public resource.
     */
    default void testOtherUserCanWritePublicResource() {
        try (final Response res = target(getPublicContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user can write to the child of a public resource.
     */
    default void testOtherUserCanWritePublicResourceChild() {
        try (final Response res = target(getPublicContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control a public resource.
     */
    default void testOtherUserCanControlPublicResource() {
        try (final Response res = target(getPublicContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control the child of a public resource.
     */
    default void testOtherUserCanControlPublicResourceChild() {
        try (final Response res = target(getPublicContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user can read a protected resource.
     */
    default void testOtherUserCanReadProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user can read the child of a protected resource.
     */
    default void testOtherUserCanReadProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user can append to a public resource.
     */
    default void testOtherUserCanAppendProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).post(entity("", TEXT_TURTLE))) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot write to a protected resource.
     */
    default void testOtherUserCanWriteProtectedResource() {
        try (final Response res = target(getProtectedContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot write to the child of a protected resource.
     */
    default void testOtherUserCanWriteProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_BAR, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control a protected resource.
     */
    default void testOtherUserCanControlProtectedResource() {
        try (final Response res = target(getProtectedContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control the child of a protected resource.
     */
    default void testOtherUserCanControlProtectedResourceChild() {
        try (final Response res = target(getProtectedContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot read a private resource.
     */
    default void testOtherUserCanReadPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot read the child of a private resource.
     */
    default void testOtherUserCanReadPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot append to a private resource.
     */
    default void testOtherUserCanAppendPrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).post(entity("", TEXT_TURTLE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot write to a private resource.
     */
    default void testOtherUserCanWritePrivateResource() {
        try (final Response res = target(getPrivateContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot write to the child of a private resource.
     */
    default void testOtherUserCanWritePrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control a private resource.
     */
    default void testOtherUserCanControlPrivateResource() {
        try (final Response res = target(getPrivateContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control the child of a private resource.
     */
    default void testOtherUserCanControlPrivateResourceChild() {
        try (final Response res = target(getPrivateContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user can read a group-controlled resource.
     */
    default void testOtherUserCanReadGroupResource() {
        try (final Response res = target(getGroupContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user can read the child of a group-controlled resource.
     */
    default void testOtherUserCanReadGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot append to a group-controlled resource.
     */
    default void testOtherUserCanAppendGroupResource() {
        try (final Response res = target(getGroupContainer()).request().header(AUTHORIZATION, getAuthorizationHeader())
                .post(entity("", TEXT_TURTLE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot write to a group-controlled resource.
     */
    default void testOtherUserCanWriteGroupResource() {
        try (final Response res = target(getGroupContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot write to the child of a group-controlled resource.
     */
    default void testOtherUserCanWriteGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control a group-controlled resource.
     */
    default void testOtherUserCanControlGroupResource() {
        try (final Response res = target(getGroupContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control the child of a group-controlled resource.
     */
    default void testOtherUserCanControlGroupResourceChild() {
        try (final Response res = target(getGroupContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user can read a default ACL resource.
     */
    default void testOtherUserCanReadDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(SUCCESSFUL, res.getStatusInfo().getFamily(), OK_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot read the child of a default ACL resource.
     */
    default void testOtherUserCanReadDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot append to a default ACL resource.
     */
    default void testOtherUserCanAppendDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).post(entity("", TEXT_TURTLE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot write to a default ACL resource.
     */
    default void testOtherUserCanWriteDefaultAclResource() {
        try (final Response res = target(getDefaultContainer()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot write to the child of a default ACL resource.
     */
    default void testOtherUserCanWriteDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild()).request()
                .header(AUTHORIZATION, getAuthorizationHeader())
                .method(PATCH, entity(INSERT_PROP_FOO, APPLICATION_SPARQL_UPDATE))) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control a default ACL resource.
     */
    default void testOtherUserCanControlDefaultAclResource() {
        try (final Response res = target(getDefaultContainer() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }

    /**
     * Verify that another user cannot control the child of a default ACL resource.
     */
    default void testOtherUserCanControlDefaultAclResourceChild() {
        try (final Response res = target(getDefaultContainerChild() + EXT_ACL).request()
                .header(AUTHORIZATION, getAuthorizationHeader()).get()) {
            assertEquals(FORBIDDEN, fromStatusCode(res.getStatus()), FORBIDDEN_RESPONSE);
        }
    }
}
