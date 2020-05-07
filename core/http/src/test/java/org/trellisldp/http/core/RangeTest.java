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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class RangeTest {

    @Test
    void testRange() {
        final Range range = Range.valueOf("bytes=1-10");
        assertNotNull(range, "Range is not null!");
        assertEquals(1, range.getFrom(), "Check 'from' value");
        assertEquals(10, range.getTo(), "Check 'to' value");
    }

    @Test
    void testInvalidRange() {
        assertNull(Range.valueOf("bytes=10-1"), "Check invalid range");
    }

    @Test
    void testInvalidNumbers() {
        assertNull(Range.valueOf("bytes=1-15.5"), "Check invalid numbers");
    }

    @Test
    void testInvalidRange2() {
        assertNull(Range.valueOf("bytes=1-15, 20-24"), "Check invalid multiple ranges");
    }

    @Test
    void testInvalidNumbers3() {
        assertNull(Range.valueOf("bytes=1-foo"), "Check invalid values");
    }

    @Test
    void testBadInput() {
        assertNull(Range.valueOf("blahblahblah"), "Check invalid input");
    }

    @Test
    void testNullInput() {
        assertNull(Range.valueOf(null), "Check null input");
    }
}
