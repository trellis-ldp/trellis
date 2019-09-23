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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class SlugTest {

    @Test
    void testSlug() {
        final Slug slug = Slug.valueOf("slugValue");
        assertEquals("slugValue", slug.getValue(), "Check slug value");
    }

    @Test
    void testEncodedInput() {
        final Slug slug = Slug.valueOf("slug%3Avalue");
        assertEquals("slug:value", slug.getValue(), "Check decoding slug value");
    }

    @Test
    void testSpaceNormalization() {
        final Slug slug = Slug.valueOf("slug  value");
        assertEquals("slug_value", slug.getValue(), "Check slug value");
    }

    @Test
    void testSlashNormalization() {
        final Slug slug = Slug.valueOf("slug/value");
        assertEquals("slug_value", slug.getValue(), "Check slug value");
    }

    @Test
    void testSpaceSlashNormalization() {
        final Slug slug = Slug.valueOf("slug\t/ value");
        assertEquals("slug_value", slug.getValue(), "Check slug value");
    }

    @Test
    void testHashUri() {
        final Slug slug = Slug.valueOf("slugValue#foo");
        assertEquals("slugValue", slug.getValue(), "Check slug value");
    }

    @Test
    void testQueryParam() {
        final Slug slug = Slug.valueOf("slugValue?bar=baz");
        assertEquals("slugValue", slug.getValue(), "Check slug value");
    }

    @Test
    void testBadInput() {
        assertNull(Slug.valueOf("An invalid % value"), "Check invalid input");
    }

    @Test
    void testNullInput() {
        assertNull(Slug.valueOf(null), "Check null input");
    }
}
