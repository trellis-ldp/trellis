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
package org.trellisldp.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author acoburn
 */
class SlugTest {

    private static final String SLUG_VALUE = "slugValue";
    private static final String SLUG_UNDERSCORE_VALUE = "slugvalue";
    private static final String CHECK_SLUG_VALUE = "Check slug value";

    @Test
    void testSlug() {
        final Slug slug = Slug.valueOf(SLUG_VALUE);
        assertEquals(SLUG_VALUE, slug.getValue(), CHECK_SLUG_VALUE);
    }

    @Test
    void testEncodedInput() {
        final Slug slug = Slug.valueOf("slug%3Avalue");
        assertEquals("slugvalue", slug.getValue(), "Check decoding slug value");
    }

    @ParameterizedTest
    @ValueSource(strings = {"slug  value", "slug/value", "slug\t/ value", "slug|value", "sl[ug^val]ue"})
    void testNormalization(final String value) {
        final Slug slug = Slug.valueOf(value);
        assertEquals(SLUG_UNDERSCORE_VALUE, slug.getValue(), CHECK_SLUG_VALUE);
    }

    @Test
    void testHashUri() {
        final Slug slug = Slug.valueOf("slugValue#foo");
        assertEquals(SLUG_VALUE, slug.getValue(), CHECK_SLUG_VALUE);
    }

    @Test
    void testQueryParam() {
        final Slug slug = Slug.valueOf("slugValue?bar=baz");
        assertEquals(SLUG_VALUE, slug.getValue(), CHECK_SLUG_VALUE);
    }

    @Test
    void testUnwiseCharacters() {
        final Slug slug = Slug.valueOf("a|b^c\"d\\e`f^g\"h<i>j{k}l");
        assertEquals("abcdefghijkl", slug.getValue(), CHECK_SLUG_VALUE);
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
