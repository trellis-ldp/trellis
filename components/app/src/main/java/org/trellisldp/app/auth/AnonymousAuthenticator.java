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

import static java.util.Optional.of;

import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.PrincipalImpl;

import java.security.Principal;
import java.util.Optional;

import org.trellisldp.vocabulary.Trellis;

/**
 * Anonymous Authenticator.
 */
public class AnonymousAuthenticator implements Authenticator<String, Principal> {

    @Override
    public Optional<Principal> authenticate(final String credentials) throws AuthenticationException {
        return of(new PrincipalImpl(Trellis.AnonymousAgent.getIRIString()));
    }
}
