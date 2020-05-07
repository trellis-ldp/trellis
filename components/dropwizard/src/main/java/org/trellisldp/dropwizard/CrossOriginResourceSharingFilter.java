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
package org.trellisldp.dropwizard;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

/**
 * A {@link ContainerResponseFilter} that adds CORS-related headers to HTTP responses.
 *
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS">Cross-Origin
 * Resource Sharing</a>
 *
 * @author acoburn
 */
@Provider
public class CrossOriginResourceSharingFilter implements ContainerResponseFilter {

    private static final Logger LOGGER = getLogger(CrossOriginResourceSharingFilter.class);
    private static final Set<String> simpleResponseHeaders = unmodifiableSet(new HashSet<>(asList("cache-control",
                    "content-language", "expires", "last-modified", "pragma")));
    private static final Set<String> simpleHeaders = unmodifiableSet(new HashSet<>(asList("accept-language",
                    "content-language")));

    private final Set<String> origins;
    private final Set<String> allowedMethods;
    private final Set<String> allowedHeaders;
    private final Set<String> exposedHeaders;
    private final boolean credentials;
    private final int cacheSeconds;

    /**
     * Create a CORS filter.
     *
     * @param origins a collection of allowed origin values
     * @param allowedMethods a collection of allowed methods
     * @param allowedHeaders a collection of allowed headers
     * @param exposedHeaders a collection of exposed headers
     * @param credentials true if the Access-Control-Allow-Credentials header is to be set
     * @param cacheSeconds set this to a value greater than zero to set the Access-Control-Max-Age header
     */
    public CrossOriginResourceSharingFilter(final Collection<String> origins, final Collection<String> allowedMethods,
            final Collection<String> allowedHeaders, final Collection<String> exposedHeaders, final boolean credentials,
            final int cacheSeconds) {
        this.origins = new HashSet<>(origins);
        this.allowedMethods = new HashSet<>(allowedMethods);
        this.allowedHeaders = allowedHeaders.stream().map(String::toLowerCase)
            .filter(x -> !simpleHeaders.contains(x)).collect(toSet());
        this.exposedHeaders = exposedHeaders.stream().map(String::toLowerCase)
            .filter(x -> !simpleResponseHeaders.contains(x)).collect(toSet());
        this.credentials = credentials;
        this.cacheSeconds = cacheSeconds;
    }

    @Override
    public void filter(final ContainerRequestContext req, final ContainerResponseContext res) {
        handleRequest(req, res).forEach(res.getHeaders()::add);
    }

    private Map<String, String> handleRequest(final ContainerRequestContext req, final ContainerResponseContext res) {
        if (OPTIONS.equals(req.getMethod())) {
            return handlePreflightRequest(req, res);
        }
        return handleSimpleRequest(req);
    }

    private boolean originMatches(final String origin) {
        return origins.contains(origin) || origins.contains("*");
    }

    private Map<String, String> handleSimpleRequest(final ContainerRequestContext req) {
        final Map<String, String> headers = new HashMap<>();
        final String origin = req.getHeaderString("Origin");

        // 6.1.1 Terminate if an Origin header is not present
        if (origin == null) {
            LOGGER.debug("CORS: No Origin header");
            return emptyMap();
        }

        // 6.1.2 Check for a case-sensitive match of the origin header string
        if (!originMatches(origin)) {
            LOGGER.debug("CORS: No Origin header match");
            return emptyMap();
        }

        // 6.1.3 Add the origin and credentials values
        headers.put("Access-Control-Allow-Origin", origin);
        if (credentials) {
            headers.put("Access-Control-Allow-Credentials", "true");
        }

        if (!exposedHeaders.isEmpty()) {
            headers.put("Access-Control-Expose-Headers", join(",", exposedHeaders));
        }

        return headers;
    }

    private Map<String, String> handlePreflightRequest(final ContainerRequestContext req,
            final ContainerResponseContext res) {
        final Map<String, String> headers = new HashMap<>();

        final String origin = req.getHeaderString("Origin");

        // 6.1.1 Terminate if an Origin header is not present
        if (origin == null) {
            LOGGER.debug("CORS PreFlight: No Origin header");
            return emptyMap();
        }

        // 6.1.2 Check for a case-sensitive match of the origin header string
        if (!originMatches(origin)) {
            LOGGER.debug("CORS PreFlight: No Origin header match");
            return emptyMap();
        }

        // 6.2.3 Set method as the value of Access-Control-Request-Method
        final String method = req.getHeaderString("Access-Control-Request-Method");

        // 6.2.4 Set field-names as the value of Access-Control-Request-Headers
        final Set<String> fieldNames = populateFieldNames(req.getHeaderString("Access-Control-Request-Headers"));

        // 6.2.5 If the method is not a case-sensitive match for the values
        // in the list of allowed methods, then terminate this set of steps.
        if (!allowedMethods.contains(method)) {
            return emptyMap();
        }

        // 6.2.6 If any of the requested header fields is not a case-insensitive match
        // for any of the values in the list of headers, terminate this set of steps.
        if (fieldNames.stream().map(String::toLowerCase).filter(x -> !simpleHeaders.contains(x))
                .anyMatch(x -> !allowedHeaders.contains(x))) {
            return emptyMap();
        }

        // 6.2.7 If the resource supports credentials, add a single Access-Control-Allow-Origin header,
        // with the value of the Origin header as value and add a single Access-Control-Allow-Credentials
        // header with the case-sensitive string "true" as value.
        headers.put("Access-Control-Allow-Origin", origin);
        if (credentials) {
            headers.put("Access-Control-Allow-Credentials", "true");
        }

        // 6.2.8 Optionally add an Access-Control-Max-Age header
        if (cacheSeconds > 0) {
            headers.put("Access-Control-Max-Age", Integer.toString(cacheSeconds));
        }

        // 6.2.9 If this method is a simple method, this step may be skipped. Add one or more
        // Access-Control-Allow-Methods headers consisting of (a subset of) the list of methods.
        headers.put("Access-Control-Allow-Methods", allowedMethods.stream()
                .filter(res.getAllowedMethods()::contains).collect(joining(",")));

        // 6.2.10 If each of the header field names is a simple header and none is Content-Type, this may be
        // skipped. Add one or more Access-Control-Allow-Headers consisting of (a subset of) the list of headers.
        if (fieldNames.stream().map(String::toLowerCase).anyMatch(x -> !simpleHeaders.contains(x))) {
            headers.put("Access-Control-Allow-Headers", join(",", allowedHeaders));
        }

        return headers;
    }

    private static Set<String> populateFieldNames(final String requestHeaders) {
        return requestHeaders == null
            ? emptySet()
            : stream(requestHeaders.split(",")).map(String::trim).collect(toSet());
    }
}
