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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.security.SecurityException;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.List;

/**
 * A Federated JWT-based authenticator.
 */
public class FederatedJwtAuthenticator implements Authenticator {

    private final KeyStore keyStore;
    private final List<String> keyIds;

    /**
     * Create a Federated JWT-based authenticator.
     * @param keyStore a keystore
     * @param keyIds a list of keyIds to use
     */
    public FederatedJwtAuthenticator(final KeyStore keyStore, final List<String> keyIds) {
        this.keyStore = keyStore;
        this.keyIds = keyIds;
    }

    @Override
    public Claims parse(final String credentials) {
        // Parse the JWT claims
        return Jwts.parserBuilder().setSigningKeyResolver(new SigningKeyResolverAdapter() {
            @Override
            public Key resolveSigningKey(final JwsHeader header, final Claims claims) {
                if (header.getKeyId() == null) {
                    throw new JwtException("Missing Key ID (kid) header field");
                }
                try {
                    if (keyIds.contains(header.getKeyId()) && keyStore.containsAlias(header.getKeyId())) {
                        return keyStore.getCertificate(header.getKeyId()).getPublicKey();
                    }
                } catch (final KeyStoreException ex) {
                    throw new SecurityException("Error retrieving key from keystore", ex);
                }
                throw new SecurityException("Could not locate key in keystore: " + header.getKeyId());
            }
        }).build().parseClaimsJws(credentials).getBody();
    }
}
