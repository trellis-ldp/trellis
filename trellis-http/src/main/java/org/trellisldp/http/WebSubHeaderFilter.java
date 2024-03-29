/*
 * Copyright (c) Aaron Coburn and individual contributors
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

import static jakarta.ws.rs.HttpMethod.GET;
import static jakarta.ws.rs.HttpMethod.HEAD;
import static jakarta.ws.rs.core.HttpHeaders.LINK;
import static jakarta.ws.rs.core.Link.fromUri;
import static jakarta.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import org.trellisldp.common.LdpResource;

/**
 * A {@link ContainerResponseFilter} that adds WebSub headers to all
 * {@code GET} responses.
 *
 * @see <a href="https://www.w3.org/TR/websub/">WebSub</a>
 */
@Provider
@LdpResource
public class WebSubHeaderFilter implements ContainerResponseFilter {

    /** The configuration key controlling the location of a web-sub-hub. */
    public static final String CONFIG_HTTP_WEB_SUB_HUB = "trellis.http.web-sub-hub";

    private String hub;

    /**
     * Create a new WebSub header Decorator.
     */
    public WebSubHeaderFilter() {
        this.hub = getConfig().getOptionalValue(CONFIG_HTTP_WEB_SUB_HUB, String.class).orElse(null);
    }

    /**
     * Set the hub value.
     * @param hub the WebSubHub location
     */
    public void setHub(final String hub) {
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
