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
package org.trellisldp.webdav;

import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.webdav.Depth.DEPTH.INFINITY;
import static org.trellisldp.webdav.Depth.DEPTH.ONE;
import static org.trellisldp.webdav.Depth.DEPTH.ZERO;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class DepthTest {

    @Test
    void testDepthInfinity() {
        final Depth depth = Depth.valueOf("infinity");
        assertEquals(INFINITY, depth.getDepth());
    }

    @Test
    void testDepthInfinity2() {
        final Depth depth = Depth.valueOf("INFINITY");
        assertEquals(INFINITY, depth.getDepth());
    }

    @Test
    void testDepthZero() {
        final Depth depth = Depth.valueOf("0");
        assertEquals(ZERO, depth.getDepth());
    }

    @Test
    void testDepthOne() {
        final Depth depth = Depth.valueOf("1");
        assertEquals(ONE, depth.getDepth());
    }

    @Test
    void testDepthOther() {
        final Depth depth = new Depth("blah");
        assertEquals(ZERO, depth.getDepth());
    }

    @Test
    void testDepthOther2() {
        final Depth depth = Depth.valueOf("blah");
        assertNull(depth);
    }

    @Test
    void testDepthNull() {
        assertNull(Depth.valueOf(null));
    }
}
