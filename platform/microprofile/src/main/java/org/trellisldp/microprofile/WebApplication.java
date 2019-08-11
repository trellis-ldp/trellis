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
package org.trellisldp.microprofile;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.trellisldp.http.*;

/**
 * Web Application wrapper.
 */
@ApplicationPath("/")
@ApplicationScoped
public class WebApplication extends Application {

    private static final Logger LOGGER = getLogger(WebApplication.class);

    @Inject
    private TrellisHttpResource httpResource;
    private TrellisHttpFilter httpFilter = new TrellisHttpFilter();
    private CrossOriginResourceSharingFilter corsFilter = new CrossOriginResourceSharingFilter();
    private CacheControlFilter cacheFilter = new CacheControlFilter();

    @Override
    public Set<Object> getSingletons() {
        LOGGER.info("Adding application JAX-RS resources: {}", httpResource);
        return asList(httpResource, httpFilter, corsFilter, cacheFilter).stream().collect(toSet());
    }
}
