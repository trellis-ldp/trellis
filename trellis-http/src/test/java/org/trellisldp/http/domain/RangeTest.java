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
package org.trellisldp.http.domain;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public class RangeTest {

    @Test
    public void testRange() {
        final Range range = Range.valueOf("bytes=1-10");
        assertTrue(range.getFrom().equals(1));
        assertTrue(range.getTo().equals(10));
    }

    @Test
    public void testInvalidRange() {
        assertNull(Range.valueOf("bytes=10-1"));
    }

    @Test
    public void testInvalidNumbers() {
        assertNull(Range.valueOf("bytes=1-15.5"));
    }

    @Test
    public void testInvalidRange2() {
        assertNull(Range.valueOf("bytes=1-15, 20-24"));
    }

    @Test
    public void testInvalidNumbers3() {
        assertNull(Range.valueOf("bytes=1-foo"));
    }

    @Test
    public void testBadInput() {
        assertNull(Range.valueOf("blahblahblah"));
    }

    @Test
    public void testNullInput() {
        assertNull(Range.valueOf(null));
    }
}
