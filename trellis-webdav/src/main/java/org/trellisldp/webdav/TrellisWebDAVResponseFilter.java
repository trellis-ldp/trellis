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
package org.trellisldp.webdav;

import static jakarta.ws.rs.HttpMethod.OPTIONS;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import org.trellisldp.common.LdpResource;

@Provider
@LdpResource
public class TrellisWebDAVResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(final ContainerRequestContext req, final ContainerResponseContext res) {
        if (OPTIONS.equals(req.getMethod())) {
            // WebDAV compliance classes 1 and 3 are supported.
            // See https://tools.ietf.org/html/rfc4918#section-18 for more information.
            res.getHeaders().add("DAV", "1,3");
        }
    }
}
