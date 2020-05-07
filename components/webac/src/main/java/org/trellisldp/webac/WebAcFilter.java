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
package org.trellisldp.webac;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static java.util.function.Predicate.isEqual;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.HttpMethod.DELETE;
import static javax.ws.rs.Priorities.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static javax.ws.rs.core.UriBuilder.fromPath;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.buildTrellisIdentifier;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_BASE_URL;
import static org.trellisldp.http.core.HttpConstants.PREFER;
import static org.trellisldp.http.core.Prefer.PREFER_REPRESENTATION;
import static org.trellisldp.http.core.TrellisRequest.buildBaseUrl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.metrics.annotation.Timed;
import org.slf4j.Logger;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.Session;
import org.trellisldp.http.core.HttpConstants;
import org.trellisldp.http.core.HttpSession;
import org.trellisldp.http.core.Prefer;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.Trellis;

/**
 * A {@link ContainerRequestFilter} that implements WebAC-based authorization.
 *
 * @see <a href="https://github.com/solid/web-access-control-spec">SOLID WebACL Specification</a>
 *
 * @author acoburn
 */
@Provider
@Priority(AUTHORIZATION)
public class WebAcFilter implements ContainerRequestFilter, ContainerResponseFilter {

    /**
     * The configuration key controlling which WWW-Authenticate challenges are provided on 401 errors.
     *
     * <p>Multiple challenges should be separated with commas.
     */
    public static final String CONFIG_WEBAC_CHALLENGES = "trellis.webac.challenges";

    /**
     * The configuration key controlling with HTTP methods should apply to the acl:Read.
     *
     * <p>Values defined here will be in addition to GET, HEAD and OPTIONS. Multiple methods should
     * be separated with commas.
     */
    public static final String CONFIG_WEBAC_READABLE_METHODS = "trellis.webac.readable-methods";
    /**
     * The configuration key controlling with HTTP methods should apply to the acl:Write.
     *
     * <p>Values defined here will be in addition to GET, HEAD and OPTIONS. Multiple methods should
     * be separated with commas.
     */
    public static final String CONFIG_WEBAC_WRITABLE_METHODS = "trellis.webac.writable-methods";
    /**
     * The configuration key controlling with HTTP methods should apply to the acl:Append.
     *
     * <p>Values defined here will be in addition to GET, HEAD and OPTIONS. Multiple methods should
     * be separated with commas.
     */
    public static final String CONFIG_WEBAC_APPENDABLE_METHODS = "trellis.webac.appendable-methods";

    /** The configuration key controlling the realm used in a WWW-Authenticate header, or 'trellis' by default. */
    public static final String CONFIG_WEBAC_REALM = "trellis.webac.realm";

    /** The configuration key controlling the scope(s) used in a WWW-Authenticate header. */
    public static final String CONFIG_WEBAC_SCOPE = "trellis.webac.scope";

    /** The session value for storing access modes. */
    public static final String SESSION_WEBAC_MODES = "trellis.webac.session-modes";

    private static final Logger LOGGER = getLogger(WebAcFilter.class);
    private static final Set<String> readable = new HashSet<>(asList("GET", "HEAD", "OPTIONS"));
    private static final Set<String> writable = new HashSet<>(asList("PUT", "PATCH", "DELETE"));
    private static final Set<String> appendable = new HashSet<>(singletonList("POST"));
    private static final RDF rdf = RDFFactory.getInstance();

    protected final WebAcService accessService;
    private final List<String> challenges;
    private final String baseUrl;

    /**
     * For use with RESTeasy and CDI proxies.
     *
     * @apiNote This construtor is used by CDI runtimes that require a public, no-argument constructor.
     *          It should not be invoked directly in user code.
     */
    public WebAcFilter() {
        this(null);
    }

    /**
     * Create a new WebAc-based auth filter.
     *
     * @param accessService the access service
     */
    @Inject
    public WebAcFilter(final WebAcService accessService) {
        this(accessService, getConfig());
    }

    private WebAcFilter(final WebAcService accessService, final Config config) {
        this(accessService,
                asList(config.getOptionalValue(CONFIG_WEBAC_CHALLENGES, String.class).orElse("").split(",")),
                config.getOptionalValue(CONFIG_WEBAC_REALM, String.class).orElse("trellis"),
                config.getOptionalValue(CONFIG_WEBAC_SCOPE, String.class).orElse(""),
                config.getOptionalValue(CONFIG_HTTP_BASE_URL, String.class).orElse(null));
    }

    /**
     * Create a WebAc-based auth filter.
     *
     * @param accessService the access service
     * @param challengeTypes the WWW-Authenticate challenge types
     * @param realm the authentication realm
     * @param scope the authentication scope
     * @param baseUrl the base URL, may be null
     */
    public WebAcFilter(final WebAcService accessService, final List<String> challengeTypes,
            final String realm, final String scope, final String baseUrl) {
        requireNonNull(challengeTypes, "Challenges may not be null!");
        requireNonNull(realm, "Realm may not be null!");
        requireNonNull(scope, "Scope may not be null!");

        final String realmParam = " realm=\"" + realm + "\"";
        final String scopeParam = scope.isEmpty() ? "" : " scope=\"" + scope + "\"";

        this.accessService = accessService;
        this.challenges = challengeTypes.stream().map(String::trim).map(ch -> ch + realmParam + scopeParam)
            .collect(toList());
        this.baseUrl = baseUrl;
        final Config config = getConfig();
        config.getOptionalValue(CONFIG_WEBAC_READABLE_METHODS, String.class).ifPresent(r ->
                stream(r.split(",")).map(String::trim).map(String::toUpperCase).forEach(readable::add));
        config.getOptionalValue(CONFIG_WEBAC_WRITABLE_METHODS, String.class).ifPresent(w ->
                stream(w.split(",")).map(String::trim).map(String::toUpperCase).forEach(writable::add));
        config.getOptionalValue(CONFIG_WEBAC_APPENDABLE_METHODS, String.class).ifPresent(a ->
                stream(a.split(",")).map(String::trim).map(String::toUpperCase).forEach(appendable::add));
    }

    @Timed
    @Override
    public void filter(final ContainerRequestContext ctx) {
        final String path = ctx.getUriInfo().getPath();
        final Session s = buildSession(ctx, baseUrl);
        final String method = ctx.getMethod();

        final Set<IRI> modes = accessService.getAccessModes(buildTrellisIdentifier(path), s);
        ctx.setProperty(SESSION_WEBAC_MODES, unmodifiableSet(modes));

        final Prefer prefer = Prefer.valueOf(ctx.getHeaderString(PREFER));

        // Control-level access
        if (ctx.getUriInfo().getQueryParameters().getOrDefault(HttpConstants.EXT, emptyList())
                .contains(HttpConstants.ACL) || reqAudit(prefer)) {
            verifyCanControl(modes, s, path);
        // Everything else
        } else {
            if (readable.contains(method) || reqRepresentation(prefer)) {
                verifyCanRead(modes, s, path);
            }
            if (writable.contains(method)) {
                verifyCanWrite(modes, s, path);
            }
            if (appendable.contains(method)) {
                verifyCanAppend(modes, s, path);
            }
        }
    }

    @Override
    public void filter(final ContainerRequestContext req, final ContainerResponseContext res) {
        if (SUCCESSFUL.equals(res.getStatusInfo().getFamily()) && !DELETE.equals(req.getMethod())) {
            final boolean isAcl = req.getUriInfo().getQueryParameters()
                .getOrDefault(HttpConstants.EXT, emptyList()).contains(HttpConstants.ACL);
            final String rel = isAcl ? HttpConstants.ACL + " self" : HttpConstants.ACL;
            final String path = req.getUriInfo().getPath();
            res.getHeaders().add(LINK, fromUri(fromPath(path.startsWith("/") ? path : "/" + path)
                        .queryParam(HttpConstants.EXT, HttpConstants.ACL).build()).rel(rel).build());
        }
    }

    protected void verifyCanAppend(final Set<IRI> modes, final Session session, final String path) {
        if (!modes.contains(ACL.Append) && !modes.contains(ACL.Write)) {
            LOGGER.warn("User: {} cannot Append to {}", session.getAgent(), path);
            if (Trellis.AnonymousAgent.equals(session.getAgent())) {
                throw new NotAuthorizedException(challenges.get(0),
                        challenges.subList(1, challenges.size()).toArray());
            }
            throw new ForbiddenException();
        }
        LOGGER.debug("User: {} can append to {}", session.getAgent(), path);
    }

    protected void verifyCanControl(final Set<IRI> modes, final Session session, final String path) {
        if (!modes.contains(ACL.Control)) {
            LOGGER.warn("User: {} cannot Control {}", session.getAgent(), path);
            if (Trellis.AnonymousAgent.equals(session.getAgent())) {
                throw new NotAuthorizedException(challenges.get(0),
                        challenges.subList(1, challenges.size()).toArray());
            }
            throw new ForbiddenException();
        }
        LOGGER.debug("User: {} can control {}", session.getAgent(), path);
    }

    protected void verifyCanWrite(final Set<IRI> modes, final Session session, final String path) {
        if (!modes.contains(ACL.Write)) {
            LOGGER.warn("User: {} cannot Write to {}", session.getAgent(), path);
            if (Trellis.AnonymousAgent.equals(session.getAgent())) {
                throw new NotAuthorizedException(challenges.get(0),
                        challenges.subList(1, challenges.size()).toArray());
            }
            throw new ForbiddenException();
        }
        LOGGER.debug("User: {} can write to {}", session.getAgent(), path);
    }

    protected void verifyCanRead(final Set<IRI> modes, final Session session, final String path) {
        if (!modes.contains(ACL.Read)) {
            LOGGER.warn("User: {} cannot Read from {}", session.getAgent(), path);
            if (Trellis.AnonymousAgent.equals(session.getAgent())) {
                throw new NotAuthorizedException(challenges.get(0),
                        challenges.subList(1, challenges.size()).toArray());
            }
            throw new ForbiddenException();
        }
        LOGGER.debug("User: {} can read {}", session.getAgent(), path);
    }

    static boolean reqAudit(final Prefer prefer) {
        return prefer != null && prefer.getInclude().contains(Trellis.PreferAudit.getIRIString());
    }

    static boolean reqRepresentation(final Prefer prefer) {
        return prefer != null && prefer.getPreference().filter(isEqual(PREFER_REPRESENTATION)).isPresent();
    }

    static String getBaseUrl(final ContainerRequestContext ctx, final String baseUrl) {
        if (baseUrl != null) {
            return baseUrl;
        }
        return buildBaseUrl(ctx.getUriInfo().getBaseUri(), ctx.getHeaders());
    }

    static Session buildSession(final ContainerRequestContext ctx, final String baseUrl) {
        final Session session = HttpSession.from(ctx.getSecurityContext());
        final String context = getBaseUrl(ctx, baseUrl);
        if (session.getAgent().getIRIString().startsWith(context)) {
            final String path = session.getAgent().getIRIString().substring(context.length());
            if (path.startsWith("/")) {
                return new HttpSession(rdf.createIRI(TRELLIS_DATA_PREFIX + path.substring(1)));
            }
            return new HttpSession(rdf.createIRI(TRELLIS_DATA_PREFIX + path));
        }
        return session;
    }
}
