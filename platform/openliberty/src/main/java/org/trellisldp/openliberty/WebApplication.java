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
package org.trellisldp.openliberty;

import static java.util.Arrays.asList;
import static org.trellisldp.app.AppUtils.printBanner;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.trellisldp.auth.oauth.OAuthFilter;
import org.trellisldp.http.*;

/**
 * Web Application wrapper.
 */
@ApplicationPath("/")
@ApplicationScoped
public class WebApplication extends Application {

    @Inject
    private TrellisHttpResource httpResource;

    @Inject
    private TrellisHttpFilter httpFilter;

    @Inject
    private CacheControlFilter cacheFilter;

    @Inject
    private OAuthFilter oauthFilter;

    @PostConstruct
    void init() {
        printBanner("Trellis Triplestore Application", "org/trellisldp/app/banner.txt");
    }

    @Override
    public Set<Object> getSingletons() {
        return new HashSet<>(asList(httpResource, httpFilter, cacheFilter, oauthFilter));
    }
}
