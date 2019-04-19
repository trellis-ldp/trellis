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
package org.trellisldp.webapp;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.ServiceLoader.load;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.http.CacheControlFilter;
import org.trellisldp.http.CrossOriginResourceSharingFilter;

final class AppUtils {

    public static final String CONFIG_WEBAPP_CACHE_ENABLED = "trellis.webapp.cache.enabled";
    public static final String CONFIG_WEBAPP_CORS_ENABLED = "trellis.webapp.cors.enabled";

    private static final Config config = ConfigProvider.getConfig();

    public static <T> T loadFirst(final Class<T> service) {
        return of(load(service).iterator()).filter(Iterator::hasNext).map(Iterator::next)
            .orElseThrow(() -> new RuntimeTrellisException("No loadable " + service.getName() + " on the classpath"));
    }

    public static <T> T loadWithDefault(final Class<T> service, final Supplier<T> other) {
        return of(load(service).iterator()).filter(Iterator::hasNext).map(Iterator::next).orElseGet(other);
    }

    public static Collection<String> asCollection(final String value) {
        return value != null ? asList(value.trim().split("\\s*,\\s*")) : emptyList();
    }

    public static Optional<CacheControlFilter> getCacheControlFilter() {
        return config.getOptionalValue(CONFIG_WEBAPP_CACHE_ENABLED, Boolean.class).filter(Boolean.TRUE::equals)
            .map(x -> new CacheControlFilter());
    }

    public static Optional<CrossOriginResourceSharingFilter> getCORSFilter() {
        return config.getOptionalValue(CONFIG_WEBAPP_CORS_ENABLED, Boolean.class).filter(Boolean.TRUE::equals)
            .map(x -> new CrossOriginResourceSharingFilter());
    }

    private AppUtils() {
        // prevent instantiation
    }
}
