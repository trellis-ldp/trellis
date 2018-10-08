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
package org.trellisldp.http;

import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.Priorities.USER;
import static javax.ws.rs.core.HttpHeaders.CACHE_CONTROL;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;

import java.io.IOException;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.CacheControl;

import org.apache.tamaya.Configuration;

/**
 * A {@link ContainerResponseFilter} that adds Cache-Control headers to all
 * {@code GET} responses.
 *
 * @author acoburn
 */
@Priority(USER)
public class CacheControlFilter implements ContainerResponseFilter {

    /** The configuration key for setting a cache-control max-age header. **/
    public static final String CONFIG_HTTP_CACHE_AGE = "trellis.http.cache.maxage";
    /** The configuration key for setting a cache-control must-revalidate header. **/
    public static final String CONFIG_HTTP_CACHE_REVALIDATE = "trellis.http.cache.revalidate";
    /** The configuration key for setting a cache-control no-cache header. **/
    public static final String CONFIG_HTTP_CACHE_NOCACHE = "trellis.http.cache.nocache";

    private static final Configuration config = getConfiguration();

    private final Integer cacheAge;
    private final Boolean revalidate;
    private final Boolean noCache;

    /**
     * Create a new CacheControl Decorator.
     */
    @Inject
    public CacheControlFilter() {
        this(config.getOrDefault(CONFIG_HTTP_CACHE_AGE, Integer.class, 86400),
             config.getOrDefault(CONFIG_HTTP_CACHE_REVALIDATE, Boolean.class, true),
             config.getOrDefault(CONFIG_HTTP_CACHE_NOCACHE, Boolean.class, false));
    }

    /**
     * Create a new CacheControl Decorator.
     *
     * @param cacheAge the length of time to cache resources
     * @param revalidate whether the cache must verify the status of stale resources
     * @param noCache whether to set the no-cache value
     */
    public CacheControlFilter(final Integer cacheAge, final Boolean revalidate, final Boolean noCache) {
        this.cacheAge = cacheAge;
        this.revalidate = revalidate;
        this.noCache = noCache;
    }

    @Override
    public void filter(final ContainerRequestContext req, final ContainerResponseContext res) throws IOException {
        if (req.getMethod().equals(GET) && SUCCESSFUL.equals(res.getStatusInfo().getFamily()) && cacheAge > 0) {
            final CacheControl cc = new CacheControl();
            cc.setMaxAge(cacheAge);
            cc.setMustRevalidate(revalidate);
            cc.setNoCache(noCache);
            res.getHeaders().add(CACHE_CONTROL, cc);
        }
    }
}
