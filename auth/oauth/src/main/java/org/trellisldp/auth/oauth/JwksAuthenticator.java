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

import static org.slf4j.LoggerFactory.getLogger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.security.SecurityException;

import java.security.Key;
import java.util.Map;

import org.slf4j.Logger;

/**
 * A jwks-based authenticator.
 */
public class JwksAuthenticator implements Authenticator {

    private static final Logger LOGGER = getLogger(JwksAuthenticator.class);

    private final Map<String, Key> keys;

    /**
     * Build a jwks-based authenticator.
     * @param url the location of the public jwks keys
     */
    public JwksAuthenticator(final String url) {
        this.keys = OAuthUtils.fetchKeys(url);
    }

    @Override
    public Claims parse(final String token) {
        return Jwts.parserBuilder().setSigningKeyResolver(new SigningKeyResolverAdapter() {
            @Override
            public Key resolveSigningKey(final JwsHeader header, final Claims claims) {
                final String keyid = header.getKeyId();
                if (keyid == null) {
                    throw new JwtException("Missing Key ID (kid) header field");
                }
                if (keys.containsKey(keyid)) {
                    return keys.get(keyid);
                }
                throw new SecurityException("Could not locate key: " + keyid);
            }
        }).build().parseClaimsJws(token).getBody();
    }
}
