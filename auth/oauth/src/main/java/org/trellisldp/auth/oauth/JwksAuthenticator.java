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
package org.trellisldp.auth.oauth;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.security.SecurityException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.Key;
import java.util.AbstractMap.SimpleEntry;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
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
        this.keys = buildKeys(url);
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

    private static Map<String, Key> buildKeys(final String location) {
        // TODO eventually, this will become part of the JJWT library
        final Deserializer<Map<String, List<Map<String, String>>>> deserializer = new JacksonDeserializer<>();
        try (final InputStream input = new URL(location).openConnection().getInputStream()) {
            return deserializer.deserialize(IOUtils.toByteArray(input)).getOrDefault("keys", emptyList()).stream()
                .map(JwksAuthenticator::buildKeyEntry).filter(Objects::nonNull).collect(collectingAndThen(
                            toMap(Map.Entry::getKey, Map.Entry::getValue), Collections::unmodifiableMap));
        } catch (final IOException ex) {
            LOGGER.error("Error fetching/parsing jwk document", ex);
        }
        return emptyMap();
    }

    private static Map.Entry<String, Key> buildKeyEntry(final Map<String, String> jwk) {
        final BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("n")));
        final BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.get("e")));
        final Key key = OAuthUtils.buildRSAPublicKey("RSA", modulus, exponent);
        if (key != null && jwk.containsKey("kid")) {
            return new SimpleEntry<>(jwk.get("kid"), key);
        }
        return null;
    }
}
