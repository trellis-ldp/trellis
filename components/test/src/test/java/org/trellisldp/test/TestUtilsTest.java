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
package org.trellisldp.test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

class TestUtilsTest {

    @Test
    void testReadEntityAsString() {
        final String text = "some text";
        final InputStream is = new ByteArrayInputStream(text.getBytes(UTF_8));
        assertEquals(text, TestUtils.readEntityAsString(is), "Verify that the input stream matches");
    }

    @Test
    void testReadEntityAsStringError() throws IOException {
        final InputStream mockInputStream = mock(InputStream.class);
        when(mockInputStream.read(any(), anyInt(), anyInt())).thenThrow(new IOException("Expected"));
        assertThrows(UncheckedIOException.class, () -> TestUtils.readEntityAsString(mockInputStream),
                "Check that an expected exception is thrown");
    }

    @Test
    void testGetResourceAsString() {
        final String resource = TestUtils.getResourceAsString("/annotation.ttl");
        assertNotNull(resource, "Resource is null!");
        assertTrue(resource.contains("<> a oa:Annotation"),"Check the resource loader works as expected");
    }

    @Test
    void testGetResourceAsStringNull() {
        assertNull(TestUtils.getResourceAsString("/non-existent-resource.ttl"),
                "Check loading a non-existent resource");
    }

    @Test
    void testReadEntityAsJson() {
        final Map<String, Object> data = singletonMap("foo", "bar");
        final InputStream is = new ByteArrayInputStream("{\"foo\" : \"bar\" }".getBytes(UTF_8));
        assertEquals(data, TestUtils.readEntityAsJson(is, new TypeReference<Map<String, Object>>(){}),
                "Check converting an InputStream to JSON");
    }

    @Test
    void testReadEntityAsJsonError() {
        final InputStream is = new ByteArrayInputStream("{\"invalid JSON\" }".getBytes(UTF_8));
        assertThrows(UncheckedIOException.class, () ->
                TestUtils.readEntityAsJson(is, new TypeReference<Map<String, Object>>(){}),
                "Check converting malformed JSON");
    }
}
