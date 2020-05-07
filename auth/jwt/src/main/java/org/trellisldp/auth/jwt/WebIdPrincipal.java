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
package org.trellisldp.auth.jwt;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.slf4j.Logger;

/** A WebID-based principal. */
public class WebIdPrincipal implements JsonWebToken {

    private static final Logger LOGGER = getLogger(WebIdPrincipal.class);

    private final JsonWebToken jwt;
    private final String webid;

    /**
     * Create a WebID-based principal from a JWT.
     * @param jwt the JWT
     */
    public WebIdPrincipal(final JsonWebToken jwt) {
        this.jwt = jwt;
        this.webid = getWebId(jwt);
        LOGGER.debug("Using webid: {}", webid);
    }

    @Override
    public <T> T getClaim(final String claim) {
        return jwt.getClaim(claim);
    }

    @Override
    public Set<String> getClaimNames() {
        return jwt.getClaimNames();
    }

    @Override
    public String getName() {
        return webid;
    }

    static String getWebId(final JsonWebToken jwt) {
        if (jwt.containsClaim("webid")) {
            return jwt.getClaim("webid");
        }

        final String subject = jwt.getSubject();
        if (isUrl(subject)) {
            return subject;
        }

        final String issuer = jwt.getIssuer();
        if (isUrl(issuer)) {
            return concat(issuer, subject);
        }

        return null;
    }

    static String concat(final String issuer, final String subject) {
        if (subject != null) {
            if (issuer.endsWith("/")) {
                return issuer + subject;
            }
            return issuer + "/" + subject;
        }

        return null;
    }

    static boolean isUrl(final String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }
}

