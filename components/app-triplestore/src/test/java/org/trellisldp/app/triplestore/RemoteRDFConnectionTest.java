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

package org.trellisldp.app.triplestore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;

import java.io.File;
import java.util.Objects;

import org.apache.jena.fuseki.FusekiException;
import org.apache.jena.fuseki.FusekiLib;
import org.apache.jena.fuseki.embedded.FusekiServer;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.jetty.security.ConstraintMapping;
import org.eclipse.jetty.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.PropertyUserStore;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.trellisldp.app.config.DatasetConnectionConfiguration;
import org.trellisldp.app.config.TrellisConfiguration;

public class RemoteRDFConnectionTest {
    private static DatasetGraph serverdsg = DatasetGraphFactory.createTxnMem();
    private static int PORT;
    private static String USER = "user1234";
    private static String PASSWORD = "password1234";
    private static TrellisConfiguration config;

    @BeforeAll
    public static void init() throws Exception {
        config = new YamlConfigurationFactory<>(TrellisConfiguration.class, Validators.newValidator(),
                Jackson.newObjectMapper(), "").build(
                new File(RemoteRDFConnectionTest.class.getResource("/config1.yml").toURI()));
        PORT = FusekiLib.choosePort();
        final FusekiServer server = FusekiServer.create().setPort(PORT).setSecurityHandler(
                makeSimpleSecurityHandler("/*", null, USER, PASSWORD, "FusekiTestRole")).add("/ds", serverdsg).build();
        server.start();
    }

    @Test
    public void testBasicAuthRemoteRDFConnection() {
        final DatasetConnectionConfiguration rc = new DatasetConnectionConfiguration();
        rc.setDatasetLocation("http://localhost:" + PORT + "/ds");
        rc.setUserName(USER);
        rc.setPassword(PASSWORD);
        config.setResources(rc);
        final RDFConnection conn = JenaRDFConnection.getRDFConnection(config).getConnection();
        final UpdateRequest req = UpdateFactory.create("INSERT DATA { <x> <p> 2 . }");
        conn.update(req);
        conn.querySelect("SELECT ?s ?p ?o WHERE {?s ?p ?o}", (qs) -> {
            final Literal o = qs.getLiteral("o");
            assertEquals(2, o.getInt());
        });
        assertNotNull(conn);
        assertFalse(conn.isClosed());
    }

    @Test
    public void testBasicAuthRemoteRDFConnectionException() {
        final DatasetConnectionConfiguration rc = new DatasetConnectionConfiguration();
        rc.setDatasetLocation("http://localhost:" + PORT + "/ds");
        rc.setUserName(USER);
        rc.setPassword("false-password");
        config.setResources(rc);
        final RDFConnection conn = JenaRDFConnection.getRDFConnection(config).getConnection();
        try {
            conn.querySelect("SELECT ?s { ?s ?p ?o }", (qs) -> {
            });
        } catch (QueryExceptionHTTP e) {
            assertEquals(401, e.getResponseCode());
        }
    }

    /**
     * Derived from org.apache.jena.fuseki.embedded.FusekiTestAuth.java
     * Create a Jetty {@link SecurityHandler} for basic authentication, one user/password/role.
     */
    private static SecurityHandler makeSimpleSecurityHandler(final String pathSpec, final String realm, final String
            user, final String password, final String role) {
        Objects.requireNonNull(user);
        Objects.requireNonNull(password);
        Objects.requireNonNull(role);

        final Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        final String[] roles = new String[]{role};
        constraint.setRoles(roles);
        constraint.setAuthenticate(true);

        final ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(pathSpec);

        final IdentityService identService = new DefaultIdentityService();

        final ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.addConstraintMapping(mapping);
        securityHandler.setIdentityService(identService);

        final UserStore userStore = makeUserStore(user, password, role);

        final HashLoginService loginService = new HashLoginService("Fuseki Authentication");
        loginService.setUserStore(userStore);
        loginService.setIdentityService(identService);

        securityHandler.setLoginService(loginService);
        securityHandler.setAuthenticator(new BasicAuthenticator());
        if (realm != null) {
            securityHandler.setRealmName(realm);
        }

        return securityHandler;
    }

    private static UserStore makeUserStore(final String user, final String password, final String role) {
        final Credential cred = new Password(password);
        final PropertyUserStore propertyUserStore = new PropertyUserStore();
        final String[] roles = role == null ? null : new String[]{role};
        propertyUserStore.addUser(user, cred, roles);
        try {
            propertyUserStore.start();
        } catch (Exception ex) {
            throw new FusekiException("UserStore", ex);
        }
        return propertyUserStore;
    }
}
