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
package org.trellisldp.websocket;

import static java.util.Objects.nonNull;
import static javax.ws.rs.HttpMethod.OPTIONS;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

/**
 * A {@link ContainerResponseFilter} that adds a header to OPTIONS responses.
 *
 * @see <a href="https://www.w3.org/TR/websub/">WebSub</a>
 */
@Provider
public class WebsocketFilter implements ContainerResponseFilter {

    /** The configuration key controlling the location of a websocket location. **/
    public static final String CONFIG_WEBSOCKET_URL = "trellis.websocket.url";

    private final String websocket;

    /**
     * Create a new Websocket header decorator.
     */
    @Inject
    public WebsocketFilter() {
        this(getConfig().getOptionalValue(CONFIG_WEBSOCKET_URL, String.class).orElse(null));
    }

    /**
     * Create a new WebSub Header Decorator.
     *
     * @param websocket the location of the websub hub
     */
    public WebsocketFilter(final String websocket) {
        this.websocket = websocket;
    }

    @Override
    public void filter(final ContainerRequestContext req, final ContainerResponseContext res) throws IOException {
        if (OPTIONS.equals(req.getMethod()) && SUCCESSFUL.equals(res.getStatusInfo().getFamily())
                && nonNull(websocket)) {
            res.getHeaders().add("Updates-VIA", websocket);
        }
    }
}
