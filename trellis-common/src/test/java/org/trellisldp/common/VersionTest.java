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
package org.trellisldp.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class VersionTest {

    @Test
    void testVersion() {
        final Version v = Version.valueOf("1493646202");
        assertEquals("2017-05-01T13:43:22Z", v.getInstant().toString(), "Check datetime string");
        assertEquals("2017-05-01T13:43:22Z", v.toString(), "Check stringified version");
    }

    @Test
    void testInvalidVersion() {
        assertNull(Version.valueOf("blah"), "Check parsing an invalid version");
    }

    @Test
    void testBadValue() {
        assertNull(Version.valueOf("-13.12"), "Check parsing an invalid date");
    }

    @Test
    void testNullValue() {
        assertNull(Version.valueOf(null), "Check parsing a null value");
    }
}
