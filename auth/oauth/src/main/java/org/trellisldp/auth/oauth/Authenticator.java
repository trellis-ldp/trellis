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
package org.trellisldp.auth.oauth;

import static org.trellisldp.auth.oauth.OAuthUtils.withSubjectClaim;
import static org.trellisldp.auth.oauth.OAuthUtils.withWebIdClaim;

import io.jsonwebtoken.Claims;

import java.security.Principal;

public interface Authenticator {

    /**
     * Parse a token into a set of claims.
     * @param token the token
     * @return JWT claims
     */
    Claims parse(String token);

    /**
     * Authenticate credentials.
     * @param token the token
     * @return the principal if present
     */
    default Principal authenticate(final String token) {
        final Claims claims = parse(token);
        // Use a webid claim, if one exists or try generating a webid from other elements
        final Principal webid = withWebIdClaim(claims);
        if (webid != null) return webid;

        return withSubjectClaim(claims);
    }
}
