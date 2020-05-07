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
package org.trellisldp.http.core;

import static java.util.Arrays.asList;
import static javax.ws.rs.core.Link.fromUri;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import javax.ws.rs.core.Link;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class DefaultTimemapGeneratorTest {

    @Test
    void testIsMementoLink() {
        final String url = "http://example.com/resource/memento";
        final TimemapGenerator svc = new DefaultTimemapGenerator();
        final List<Link> links = asList(
            fromUri(url).rel("memento").param("datetime", "Fri, 11 May 2018 15:29:25 GMT").build(),
            fromUri(url).rel("foo").param("datetime", "Fri, 11 May 2018 15:39:25 GMT").build(),
            fromUri(url).rel("memento").param("bar", "Fri, 11 May 2018 15:49:25 GMT").build());
        // 4 standard links + 2 memento links + 6 for each memento link w/ datetime
        assertEquals(12L, svc.asRdf(url, links).count());
    }
}
