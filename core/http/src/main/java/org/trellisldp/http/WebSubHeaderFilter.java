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
package org.trellisldp.http;

import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.HttpMethod.HEAD;
import static javax.ws.rs.core.HttpHeaders.LINK;
import static javax.ws.rs.core.Link.fromUri;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 * A {@link ContainerResponseFilter} that adds WebSub headers to all
 * {@code GET} responses.
 *
 * @see <a href="https://www.w3.org/TR/websub/">WebSub</a>
 */
@Provider
public class WebSubHeaderFilter implements ContainerResponseFilter {

    /** The configuration key controlling the location of a web-sub-hub. */
    public static final String CONFIG_HTTP_WEB_SUB_HUB = "trellis.http.web-sub-hub";

    private final String hub;

    /**
     * Create a new WebSub header Decorator.
     */
    @Inject
    public WebSubHeaderFilter() {
        this(getConfig().getOptionalValue(CONFIG_HTTP_WEB_SUB_HUB, String.class).orElse(null));
    }

    /**
     * Create a new WebSub Header Decorator.
     *
     * @param hub the location of the websub hub
     */
    public WebSubHeaderFilter(final String hub) {
        this.hub = hub;
    }

    @Override
    public void filter(final ContainerRequestContext req, final ContainerResponseContext res) {
        if ((GET.equals(req.getMethod()) || HEAD.equals(req.getMethod()))
                && SUCCESSFUL.equals(res.getStatusInfo().getFamily()) && hub != null) {
            res.getHeaders().add(LINK, fromUri(hub).rel("hub").build());
        }
    }
}
