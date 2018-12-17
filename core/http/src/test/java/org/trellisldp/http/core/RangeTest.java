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
package org.trellisldp.http.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class RangeTest {

    @Test
    public void testRange() {
        final Range range = Range.valueOf("bytes=1-10");
        assertEquals(1, range.getFrom(), "Check 'from' value");
        assertEquals(10, range.getTo(), "Check 'to' value");
    }

    @Test
    public void testInvalidRange() {
        assertNull(Range.valueOf("bytes=10-1"), "Check invalid range");
    }

    @Test
    public void testInvalidNumbers() {
        assertNull(Range.valueOf("bytes=1-15.5"), "Check invalid numbers");
    }

    @Test
    public void testInvalidRange2() {
        assertNull(Range.valueOf("bytes=1-15, 20-24"), "Check invalid multiple ranges");
    }

    @Test
    public void testInvalidNumbers3() {
        assertNull(Range.valueOf("bytes=1-foo"), "Check invalid values");
    }

    @Test
    public void testBadInput() {
        assertNull(Range.valueOf("blahblahblah"), "Check invalid input");
    }

    @Test
    public void testNullInput() {
        assertNull(Range.valueOf(null), "Check null input");
    }
}
