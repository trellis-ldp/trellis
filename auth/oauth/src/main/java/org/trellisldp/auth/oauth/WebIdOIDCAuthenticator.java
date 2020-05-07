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

import static java.util.concurrent.TimeUnit.DAYS;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Key;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.trellisldp.cache.TrellisCache;

/**
 * An authenticator for Solid's WebId-OIDC protocol, <a href="https://github.com/solid/webid-oidc-spec">WebId-OIDC</a>.
 * This implementation requires no WebId provider configuration, the validation is performed based on the in JWT payload
 * supplied key data.
 */
public class WebIdOIDCAuthenticator implements Authenticator {
    private static final String OPENID_CONFIGURATION_PATH = "/.well-known/openid-configuration";
    private static final String JWKS_URI_CLAIM = "jwks_uri";
    static final String KEY_ID_HEADER = "kid";
    static final String ID_TOKEN_CLAIM = "id_token";
    static final String CNF_CLAIM = "cnf";
    static final String TOKEN_TYPE_CLAIM = "token_type";
    static final String POP_TOKEN = "pop";

    private final String baseUrl;
    private final TrellisCache<String, Key> keys;

    /**
     * Own exception to signal a malformed JWT according to the expectations of this authenticator.
     */
    static class WebIdOIDCJwtException extends MalformedJwtException {
        public WebIdOIDCJwtException(final String message) {
            super(message);
        }

        public WebIdOIDCJwtException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Build the authenticator.
     * @param baseUrl the server's base URL.
     * @param cacheSize the maximum cache size
     * @param cacheExpireDays the number of days after which the cache expires
     */
    public WebIdOIDCAuthenticator(final String baseUrl, final int cacheSize, final int cacheExpireDays) {
        this(baseUrl, cacheSize, cacheExpireDays, Collections.emptyMap());
    }

    /**
     * Constructor for test purposes.
     * @param baseUrl the server's base URL.
     * @param cacheSize the maximum cache size
     * @param cacheExpireDays the number of days after which the cache expires
     * @param keys an initial key cache for this instance.
     */
    WebIdOIDCAuthenticator(final String baseUrl, final int cacheSize, final int cacheExpireDays,
                           final Map<String, Key> keys) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("Received null as baseUrl, it is required for the WebId-OIDC support.");
        }
        this.baseUrl = baseUrl;
        final Cache<String, Key> cache =
                CacheBuilder.newBuilder().maximumSize(cacheSize).expireAfterAccess(cacheExpireDays, DAYS).build();
        this.keys = new TrellisCache<>(cache);
        for (final String id : keys.keySet()) {
            this.keys.get(id, keys::get);
        }
    }

    @Override
    public Claims parse(final String token) {
        final Claims[] idTokenClaims = new Claims[1];
        final Claims claims = Jwts.parserBuilder()
                .require(TOKEN_TYPE_CLAIM, POP_TOKEN)
                .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                    @Override
                    public Key resolveSigningKey(final JwsHeader header, final Claims claims) {
                        final String issuer = claims.getIssuer();
                        if (issuer == null) {
                            throw new WebIdOIDCJwtException("Missing the issuer claim in outer JWT payload");
                        }
                        final String idToken = claims.get(ID_TOKEN_CLAIM, String.class);
                        if (idToken == null) {
                            throw new WebIdOIDCJwtException("Missing the id_token claim in JWT payload");
                        }
                        idTokenClaims[0] = getValidatedIdTokenClaims(issuer, idToken);
                        return getKey(idTokenClaims[0]);
                    }
                })
                .build()
                .parseClaimsJws(token).getBody();

        doWebIdProviderConfirmation(idTokenClaims[0]);
        final String audience = claims.getAudience();
        if (audience == null || !audience.endsWith(baseUrl)) {
            throw new WebIdOIDCJwtException(
                    String.format("Proof of Possession failed, wrong audience claim: aud=%s", audience));
        }
        return idTokenClaims[0];
    }

    private Claims getValidatedIdTokenClaims(final String issuer, final String idToken) {
        return Jwts.parserBuilder()
                .requireAudience(issuer)
                .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                    @Override
                    public Key resolveSigningKey(final JwsHeader header, final Claims claims) {
                        final String kid = (String) header.get(KEY_ID_HEADER);
                        if (kid == null) {
                            throw new WebIdOIDCJwtException(
                                    String.format("Missing the key id in Id token header: idTokenHeader=%s", header));
                        }
                        return keys.get(kid, id -> {
                            final String issuer = claims.getIssuer();
                            if (issuer == null) {
                                throw new WebIdOIDCJwtException("Found no issuer claim to resolve the provider keys");
                            }
                            final Map<String, Key> fetchedKeys = OAuthUtils.fetchKeys(fetchJwksUri(issuer));
                            if (!fetchedKeys.containsKey(id)) {
                                throw new WebIdOIDCJwtException(
                                        String.format("Couldn't find key id %s for token issuer %s", id, issuer));
                            }
                            return fetchedKeys.get(id);
                        });
                    }
                })
                .build()
                .parseClaimsJws(idToken).getBody();
    }

    private static String fetchJwksUri(final String issuer) {
        final Deserializer<Map<String, Object>> deserializer = new JacksonDeserializer<>();
        final String oidcCnfUrl = issuer + OPENID_CONFIGURATION_PATH;
        try (final InputStream input = new URL(oidcCnfUrl).openConnection().getInputStream()) {
            final String jwksUri = (String) deserializer.deserialize(IOUtils.toByteArray(input)).get(JWKS_URI_CLAIM);
            if (jwksUri == null) {
                throw new WebIdOIDCJwtException("No JWKS URI claim found in WebId-OIDC provider configuration");
            }
            return jwksUri;
        } catch (final Exception ex) {
            throw new WebIdOIDCJwtException(
                    String.format("Error fetching/parsing WebId-OIDC provider configuration for %s", issuer), ex);
        }
    }

    private static void doWebIdProviderConfirmation(final Claims idTokenClaims) {
        final String issuer = idTokenClaims.getIssuer();
        if (issuer == null) {
            throw new WebIdOIDCJwtException("WebId provider confirmation failed. Missing needed issuer claim.");
        }
        final String webId = idTokenClaims.get(OAuthUtils.WEBID, String.class);
        final String subject = idTokenClaims.getSubject();
        if (webId == null && subject == null) {
            final String msg =
                "WebId provider confirmation failed. At least the webid or the sub claim must be present, found none.";
            throw new WebIdOIDCJwtException(msg);
        }
        final String webIdClaim = webId != null ? webId : subject;
        doSameOriginOrSubDomainConfirmation(issuer, webIdClaim);
    }

    private static void doSameOriginOrSubDomainConfirmation(final String issuer, final String webIdClaim) {
        try {
            final URI webIdURI = new URI(webIdClaim);
            final URI issuerURI = new URI(issuer);
            final boolean isIssuerHttpURI = issuerURI.getScheme().startsWith("http");
            final boolean isWebIdHttpURI = webIdURI.getScheme().startsWith("http");
            final boolean isSubDomainOrSameOrigin = webIdURI.getHost().endsWith(issuerURI.getHost());
            if (!isIssuerHttpURI || !isWebIdHttpURI) {
                throw new WebIdOIDCJwtException(String.format(
                        "Issuer and WebId must be HTTP(S) URIs, (iss, webid)=(%s, %s).", issuer, webIdClaim));
            }
            if (!isSubDomainOrSameOrigin) {
                throw new WebIdOIDCJwtException(String.format(
                        "WebId has neither same domain nor is subdomain of issuer, (iss, webid)=(%s, %s).",
                        issuer, webIdClaim));
            }
        } catch (final URISyntaxException e) {
            throw new WebIdOIDCJwtException(
                String.format("WebId provider confirmation failed, received invalid URI: (iss, webid)=(%s, %s).",
                    issuer, webIdClaim), e);
        }
    }

    private static Key getKey(final Claims idTokenClaims) {
        final Map<String, Map<String, String>> cnf;
        try {
            cnf = idTokenClaims.get(CNF_CLAIM, Map.class);
            if (cnf == null) {
                throw new WebIdOIDCJwtException(String.format("Missing cnf in: %s", idTokenClaims));
            }
            final Map<String, String> jwk = cnf.get("jwk");
            final String alg = jwk.get("alg");
            final String n = jwk.get("n");
            final String e = jwk.get("e");
            if (alg == null || n == null || e == null) {
                final String msg =
                    String.format("Missing at least one algorithm parameter under cnf.jwk. (alg, n, e): (%s, %s, %s)",
                        alg, n, e);
                throw new WebIdOIDCJwtException(msg);
            }
            if (!alg.startsWith("RS")) {
                final String msg = String.format("Expecting RSA algorithm under: %s, but got: %s", "cnf.jwk", alg);
                throw new WebIdOIDCJwtException(msg);
            }
            return OAuthUtils.buildKey(n, e);
        } catch (ClassCastException | RequiredTypeException e) {
            throw new WebIdOIDCJwtException(String.format("Malformed cnf in: %s", idTokenClaims), e);
        }
    }
}
