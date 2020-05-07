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

import static io.jsonwebtoken.security.Keys.hmacShaKeyFor;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toMap;
import static org.slf4j.LoggerFactory.getLogger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Files;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

public final class OAuthUtils {

    public static final String WEBID = "webid";
    public static final String WEBSITE = "website";
    private static final Logger LOGGER = getLogger(OAuthUtils.class);

    /**
     * Generate a Principal from a webid claim.
     * @param claims the JWT claims
     * @return a Principal, if one can be generated from a webid claim
     */
    public static Principal withWebIdClaim(final Claims claims) {
        final String webid = claims.get(WEBID, String.class);
        if (webid != null) {
            LOGGER.debug("Using JWT claim with webid: {}", webid);
            return new OAuthPrincipal(webid);
        }
        return null;
    }

    /**
     * Generate a Principal from a subject claim.
     * @param claims the JWT claims
     * @return a Principal, if one can be generated from standard claims
     */
    public static Principal withSubjectClaim(final Claims claims) {
        final String subject = claims.getSubject();
        if (subject == null) return null;
        if (isUrl(subject)) {
            LOGGER.debug("Using JWT claim with sub: {}", subject);
            return new OAuthPrincipal(subject);
        }

        final String iss = claims.getIssuer();
        // combine the iss and sub fields if that appears possible
        if (iss != null && isUrl(iss)) {
            final String webid = iss.endsWith("/") ? iss + subject : iss + "/" + subject;
            LOGGER.debug("Using JWT claim with generated webid: {}", webid);
            return new OAuthPrincipal(webid);
        }

        // Use an OIDC website claim, if one exists
        if (claims.containsKey(WEBSITE)) {
            final String site = claims.get(WEBSITE, String.class);
            LOGGER.debug("Using JWT claim with website: {}", site);
            return new OAuthPrincipal(site);
        }
        return null;
    }

    /**
     * Build an authenticator.
     * @param location the key location
     * @return an Authenticator
     */
    public static Authenticator buildAuthenticatorWithJwk(final String location) {
        if (location != null && isUrl(location)) {
            return new JwksAuthenticator(location);
        }
        return null;
    }

    /**
     * Build an RSA public key.
     * @param keyType the algorithm (should be "RSA")
     * @param modulus the modulus
     * @param exponent the exponent
     * @return an RSA public key, if one could be successfully generated
     */
    public static Key buildRSAPublicKey(final String keyType, final BigInteger modulus,
            final BigInteger exponent) {
        try {
            return KeyFactory.getInstance(keyType).generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (final NoSuchAlgorithmException | InvalidKeySpecException ex) {
            LOGGER.error("Error generating RSA Key from JWKS entry", ex);
        }
        return null;
    }

    /**
     * Build an authenticator.
     * @param key the key
     * @return an Authenticator
     */
    public static Authenticator buildAuthenticatorWithSharedSecret(final String key) {
        if (key != null && !key.isEmpty()) {
            return new JwtAuthenticator(hmacShaKeyFor(key.getBytes(UTF_8)));
        }
        return null;
    }

    /**
     * Build an authenticator.
     * @param keystorePath the path to a keystore
     * @param keystorePassword the password for a keystore
     * @param keyids the key ids
     * @return an Authenticator, or null if there was an error
     */
    public static Authenticator buildAuthenticatorWithTruststore(final String keystorePath,
            final char[] keystorePassword, final List<String> keyids) {
        if (keystorePath != null) {
            final File file = new File(keystorePath);
            if (file.exists()) {
                try (final InputStream is = Files.newInputStream(file.toPath())) {
                    final KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                    ks.load(is, keystorePassword);
                    final List<String> keyIds = filterKeyIds(ks, keyids);
                    switch (keyIds.size()) {
                        case 0:
                            LOGGER.warn("No valid key ids provided! Skipping keystore: {}", keystorePath);
                            return null;
                        case 1:
                            return new JwtAuthenticator(ks.getCertificate(keyIds.get(0)).getPublicKey());
                        default:
                            return new FederatedJwtAuthenticator(ks, keyIds);
                    }
                } catch (final IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException ex) {
                    LOGGER.error("Error reading keystore: {}", ex.getMessage());
                    LOGGER.warn("Ignoring JWT authenticator with keystore: {}", keystorePath);
                }
            }
        }
        return null;
    }

    public static Authenticator buildAuthenticatorWithWebIdOIDC(final boolean enabled, final String baseUrl,
                                                                final int cacheSize, final int cacheExpireDays) {
        return enabled ? new WebIdOIDCAuthenticator(baseUrl, cacheSize, cacheExpireDays) : null;
    }

    private static List<String> filterKeyIds(final KeyStore ks, final List<String> keyids) throws KeyStoreException {
        final List<String> ids = new ArrayList<>();
        for (final String id : keyids) {
            final String keyId = id.trim();
            if (ks.containsAlias(keyId)) {
                ids.add(keyId);
            }
        }
        return ids;
    }

    /**
     * Check whether a string is a URL.
     * @param url the putative URL
     * @return true if the string looks like a URL; false otherwise
     */
    private static boolean isUrl(final String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * Fetches the key configurations from the given location and returns it in a keyId -> Key Map.
     * @param location string of the URL
     * @return Map containing the key configurations
     */
    static Map<String, Key> fetchKeys(final String location) {
        // TODO eventually, this will become part of the JJWT library
        final Deserializer<Map<String, List<Map<String, String>>>> deserializer = new JacksonDeserializer<>();
        try (final InputStream input = new URL(location).openStream()) {
            return deserializer.deserialize(IOUtils.toByteArray(input)).getOrDefault("keys", emptyList()).stream()
                    .map(OAuthUtils::buildKeyEntry).filter(Objects::nonNull).collect(collectingAndThen(
                            toMap(Map.Entry::getKey, Map.Entry::getValue), Collections::unmodifiableMap));
        } catch (final IOException ex) {
            LOGGER.error(String.format("Error fetching/parsing jwk document at location %s", location), ex);
        }
        return emptyMap();
    }

    private static Map.Entry<String, Key> buildKeyEntry(final Map<String, String> jwk) {
        final Key key = buildKey(jwk.get("n"), jwk.get("e"));
        if (key != null && jwk.containsKey("kid")) {
            return new AbstractMap.SimpleEntry<>(jwk.get("kid"), key);
        }
        return null;
    }

    /**
     * Builds a public key from its parameters as encoded as base64.
     * @param n modulus
     * @param e exponent
     * @return the key
     */
    static Key buildKey(final String n, final String e) {
        final BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        final BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        return OAuthUtils.buildRSAPublicKey("RSA", modulus, exponent);
    }

    private OAuthUtils() {
        // prevent instantiation
    }
}
