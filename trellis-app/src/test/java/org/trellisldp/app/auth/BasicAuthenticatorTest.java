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
package org.trellisldp.app.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.basic.BasicCredentials;

import java.io.File;
import java.security.Principal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class BasicAuthenticatorTest {

    @Test
    public void testAuthenticate() throws AuthenticationException {
        final Authenticator<BasicCredentials, Principal> authenticator = new BasicAuthenticator(getAuthFile());
        final BasicCredentials credentials = new BasicCredentials("acoburn", "secret");

        final Optional<Principal> res = authenticator.authenticate(credentials);
        assertTrue(res.isPresent());
        res.ifPresent(p -> {
            assertEquals("https://people.apache.org/~acoburn/#i", p.getName());
        });
    }

    @Test
    public void testAuthenticateNoWebid() throws AuthenticationException {
        final Authenticator<BasicCredentials, Principal> authenticator = new BasicAuthenticator(getAuthFile());
        final BasicCredentials credentials = new BasicCredentials("other", "pass");

        final Optional<Principal> res = authenticator.authenticate(credentials);
        assertFalse(res.isPresent());
    }

    @Test
    public void testAuthenticateInvalid() throws AuthenticationException {
        final Authenticator<BasicCredentials, Principal> authenticator = new BasicAuthenticator(getAuthFile());
        final BasicCredentials credentials = new BasicCredentials("acoburn", "incorrect");

        final Optional<Principal> res = authenticator.authenticate(credentials);
        assertFalse(res.isPresent());
    }

    @Test
    public void testAuthenticateInvalidFile() throws AuthenticationException {
        final Authenticator<BasicCredentials, Principal> authenticator = new BasicAuthenticator(
                getAuthFile() + "missing");
        final BasicCredentials credentials = new BasicCredentials("acoburn", "incorrect");

        final Optional<Principal> res = authenticator.authenticate(credentials);
        assertFalse(res.isPresent());
    }

    @Test
    public void testAuthenticateUnreadableFile() throws AuthenticationException {
        final Authenticator<BasicCredentials, Principal> authenticator = new BasicAuthenticator(getAuthFile());
        final BasicCredentials credentials = new BasicCredentials("acoburn", "secret");

        final File userFile = new File(getAuthFile());
        assumeTrue(userFile.setReadable(false));

        final Optional<Principal> res = authenticator.authenticate(credentials);
        assertFalse(res.isPresent());
        userFile.setReadable(true);
    }



    private String getAuthFile() {
        final String prefix = "file:";
        return getClass().getResource("/users.auth").toString().substring(prefix.length());
    }
}
