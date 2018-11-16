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
import static java.util.Objects.isNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.ServiceLoader.load;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Supplier;

import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.http.CacheControlFilter;
import org.trellisldp.http.CrossOriginResourceSharingFilter;

final class AppUtils {

    public static final String CONFIG_WEBAPP_CACHE_ENABLED = "trellis.webapp.cache.enabled";
    public static final String CONFIG_WEBAPP_CORS_ENABLED = "trellis.webapp.cors.enabled";

    private static final Configuration config = ConfigurationProvider.getConfiguration();

    public static <T> T loadFirst(final Class<T> service) {
        return of(load(service).iterator()).filter(Iterator::hasNext).map(Iterator::next)
            .orElseThrow(() -> new RuntimeTrellisException("No loadable " + service.getName() + " on the classpath"));
    }

    public static <T> T loadWithDefault(final Class<T> service, final Supplier<T> other) {
        return of(load(service).iterator()).filter(Iterator::hasNext).map(Iterator::next).orElseGet(other);
    }

    public static Collection<String> asCollection(final String value) {
        return isNull(value) ? emptyList() :  asList(value.trim().split("\\s*,\\s*"));
    }

    public static Optional<CacheControlFilter> getCacheControlFilter() {
        if (config.getOrDefault(CONFIG_WEBAPP_CACHE_ENABLED, Boolean.class, false)) {
            return of(new CacheControlFilter());
        }
        return empty();
    }

    public static Optional<CrossOriginResourceSharingFilter> getCORSFilter() {
        if (config.getOrDefault(CONFIG_WEBAPP_CORS_ENABLED, Boolean.class, false)) {
            return of(new CrossOriginResourceSharingFilter());
        }
        return empty();
    }

    private AppUtils() {
        // prevent instantiation
    }
}
