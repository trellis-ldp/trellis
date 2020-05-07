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

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.*;

import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class ForwardedTest {

    @Test
    void testForwarded() {
        final Forwarded forwarded = Forwarded.valueOf("by=1.2.3.4;for=9.8.7.6;host=example.com;proto=https");
        assertEquals(of("1.2.3.4"), forwarded.getBy());
        assertEquals(of("9.8.7.6"), forwarded.getFor());
        assertEquals(of("example.com"), forwarded.getHost());
        assertEquals(of("https"), forwarded.getProto());
    }

    @Test
    void testForwardedWithQuotes() {
        final Forwarded forwarded =
            Forwarded.valueOf("BY=\"1.2.3.4\"; For=\"9.8.7.6\"; Host=\"example.com\"; Proto=\"http\"");
        assertEquals(of("1.2.3.4"), forwarded.getBy());
        assertEquals(of("9.8.7.6"), forwarded.getFor());
        assertEquals(of("example.com"), forwarded.getHost());
        assertEquals(of("http"), forwarded.getProto());
    }

    @Test
    void testForwardedMissingFields() {
        final Forwarded forwarded = Forwarded.valueOf("for=9.8.7.6");
        assertFalse(forwarded.getBy().isPresent());
        assertEquals(of("9.8.7.6"), forwarded.getFor());
        assertFalse(forwarded.getHost().isPresent());
        assertFalse(forwarded.getProto().isPresent());
    }

    @Test
    void testNullForwarded() {
        assertNull(Forwarded.valueOf(null));
    }

    @Test
    void testInvalidProto() {
        final Forwarded forwarded = Forwarded.valueOf("proto=foo");
        assertFalse(forwarded.getProto().isPresent());
    }

    @Test
    void testEmptyPort() {
        final Forwarded forwarded = Forwarded.valueOf("host=example.com:");
        assertEquals(of("example.com"), forwarded.getHostname());
        assertEquals(OptionalInt.of(-1), forwarded.getPort());
    }

    @Test
    void testInvalidPort() {
        final Forwarded forwarded = Forwarded.valueOf("host=example.com:foo");
        assertEquals(of("example.com"), forwarded.getHostname());
        assertFalse(forwarded.getPort().isPresent());
    }

    @Test
    void testInvalidFields() {
        final Forwarded forwarded = Forwarded.valueOf("foo= ; ; =bar ;=; baz; one = two ");
        assertFalse(forwarded.getBy().isPresent());
        assertFalse(forwarded.getFor().isPresent());
        assertFalse(forwarded.getHost().isPresent());
        assertFalse(forwarded.getProto().isPresent());
    }
}
