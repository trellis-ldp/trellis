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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getDecoder;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

/**
 * A JWT-based authenticator.
 */
public class JwtAuthenticator extends AbstractJwtAuthenticator {

    private final Key key;

    /**
     * Create a JWT-based authenticator.
     * @param key a secret key
     * @param encoded whether the key is encoded as base64
     */
    public JwtAuthenticator(final String key, final Boolean encoded) {
        this(key, encoded, SignatureAlgorithm.HS512);
    }

    /**
     * Create a JWT-based authenticator.
     * @param key a secret key
     * @param encoded whether the key is encoded as base64
     * @param algorithm the signature algorithm
     */
    public JwtAuthenticator(final String key, final Boolean encoded, final SignatureAlgorithm algorithm) {
        this(new SecretKeySpec(encoded ? getDecoder().decode(key) : key.getBytes(UTF_8), algorithm.getJcaName()));
    }

    /**
     * Create a JWT-based authenticator.
     * @param key a key
     */
    public JwtAuthenticator(final Key key) {
        this.key = key;
    }

    @Override
    protected Claims parse(final String credentials) {
        // Parse the JWT claims
        return Jwts.parser().setSigningKey(key).parseClaimsJws(credentials).getBody();
    }
}
