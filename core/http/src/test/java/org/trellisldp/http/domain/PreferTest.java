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

import static java.util.Optional.of;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * @author acoburn
 */
public class PreferTest {

    @Test
    public void testPrefer1() {
        final Prefer prefer = Prefer.valueOf("return=representation; include=\"http://example.org/test\"");
        assertAll("Check simple Prefer parsing", checkPreferInclude(prefer, "http://example.org/test"));
    }

    @Test
    public void testPrefer1b() {
        final Prefer prefer = Prefer.ofInclude("http://example.org/test");
        assertAll("Check simple Prefer parsing", checkPreferInclude(prefer, "http://example.org/test"));
    }

    @Test
    public void testPrefer1c() {
        final Prefer prefer = Prefer.valueOf("return=representation; include=http://example.org/test");
        assertAll("Check simple Prefer parsing", checkPreferInclude(prefer, "http://example.org/test"));
    }

    @Test
    public void testPrefer2() {
        final Prefer prefer = Prefer.valueOf("return  =  representation;   include =  \"http://example.org/test\"");
        assertAll("Check simple Prefer parsing", checkPreferInclude(prefer, "http://example.org/test"));
    }

    @Test
    public void testPrefer3() {
        final Prefer prefer = Prefer.valueOf("return=minimal");
        assertEquals(of("minimal"), prefer.getPreference(), "Check preference value");
        assertTrue(prefer.getInclude().isEmpty(), "Check that there are no includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check omits count is zero");
        assertFalse(prefer.getHandling().isPresent(), "Check handling");
        assertFalse(prefer.getWait().isPresent(), "Check wait");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testPrefer4() {
        final Prefer prefer = Prefer.valueOf("return=other");
        assertTrue(prefer.getInclude().isEmpty(), "Check includes is empty");
        assertTrue(prefer.getOmit().isEmpty(), "Check omits is empty");
        assertFalse(prefer.getPreference().isPresent(), "Check preference type");
        assertFalse(prefer.getHandling().isPresent(), "Check handling type");
        assertFalse(prefer.getWait().isPresent(), "Check wait value");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testPrefer5() {
        final Prefer prefer = Prefer.valueOf("return=representation; omit=\"http://example.org/test\"");
        assertEquals(of("representation"), prefer.getPreference(), "Check preference value");
        assertTrue(prefer.getInclude().isEmpty(), "Check for no includes");
        assertFalse(prefer.getOmit().isEmpty(), "Check for omits");
        assertTrue(prefer.getOmit().contains("http://example.org/test"), "Check omit value");
        assertFalse(prefer.getHandling().isPresent(), "Check handling");
        assertFalse(prefer.getWait().isPresent(), "Check wait value");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testPrefer5b() {
        final Prefer prefer = Prefer.ofOmit("http://example.org/test");
        assertEquals(of("representation"), prefer.getPreference(), "Check preference value");
        assertTrue(prefer.getInclude().isEmpty(), "Check for no includes");
        assertFalse(prefer.getOmit().isEmpty(), "Check for omit values");
        assertTrue(prefer.getOmit().contains("http://example.org/test"), "Check omit value");
        assertFalse(prefer.getHandling().isPresent(), "Check handling");
        assertFalse(prefer.getWait().isPresent(), "Check wait value");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testPrefer6() {
        final Prefer prefer = Prefer.valueOf("handling=lenient; return=minimal");
        assertTrue(prefer.getInclude().isEmpty(), "Check for no includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check for no omits");
        assertEquals(of("minimal"), prefer.getPreference(), "Check preference value");
        assertEquals(of("lenient"), prefer.getHandling(), "Check handling value");
        assertFalse(prefer.getWait().isPresent(), "Check wait value");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testPrefer7() {
        final Prefer prefer = Prefer.valueOf("respond-async; random-param");
        assertTrue(prefer.getInclude().isEmpty(), "Check for no includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check for no omits");
        assertFalse(prefer.getPreference().isPresent(), "Check preference type");
        assertFalse(prefer.getHandling().isPresent(), "Check handling value");
        assertFalse(prefer.getWait().isPresent(), "Check wait value");
        assertTrue(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testPrefer8() {
        final Prefer prefer = Prefer.valueOf("handling=strict; return=minimal");
        assertTrue(prefer.getInclude().isEmpty(), "Check for no includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check for no omits");
        assertEquals(of("minimal"), prefer.getPreference(), "Check preference value");
        assertEquals(of("strict"), prefer.getHandling(), "Check handling value");
        assertFalse(prefer.getWait().isPresent(), "Check wait value");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testPrefer9() {
        final Prefer prefer = Prefer.valueOf("handling=blah; return=minimal");
        assertTrue(prefer.getInclude().isEmpty(), "Check for no includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check for no omits");
        assertEquals(of("minimal"), prefer.getPreference(), "Check preference type");
        assertFalse(prefer.getHandling().isPresent(), "Check handling");
        assertFalse(prefer.getWait().isPresent(), "Check wait value");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testPrefer10() {
        final Prefer prefer = Prefer.valueOf("wait=4");
        assertTrue(prefer.getInclude().isEmpty(), "Check for no includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check for no omits");
        assertFalse(prefer.getPreference().isPresent(), "Check preference value");
        assertFalse(prefer.getHandling().isPresent(), "Check handling");
        assertEquals((Integer)4, prefer.getWait().get(), "Check wait value");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testStaticInclude() {
        final Prefer prefer = Prefer.ofInclude();
        assertEquals(of("representation"), prefer.getPreference(), "Check preference type");
        assertTrue(prefer.getInclude().isEmpty(), "Check for no includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check for no omits");
    }

    @Test
    public void testStaticOmit() {
        final Prefer prefer = Prefer.ofOmit();
        assertEquals(of("representation"), prefer.getPreference(), "Check preference type");
        assertTrue(prefer.getInclude().isEmpty(), "Check for no includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check for no omits");
    }

    @Test
    public void testPreferBadQuotes() {
        final Prefer prefer = Prefer.valueOf("return=representation; include=\"http://example.org/test");
        assertEquals(of("representation"), prefer.getPreference(), "Check preference type");
        assertEquals(1L, prefer.getInclude().size(), "Check includes count");
        assertTrue(prefer.getInclude().contains("\"http://example.org/test"));
        assertTrue(prefer.getOmit().isEmpty(), "Check omits count is zero");
        assertFalse(prefer.getHandling().isPresent(), "Check handling");
        assertFalse(prefer.getWait().isPresent(), "Check wait");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testPreferBadQuotes2() {
        final Prefer prefer = Prefer.valueOf("return=representation; include=\"");
        assertEquals(of("representation"), prefer.getPreference(), "Check preference type");
        assertEquals(1L, prefer.getInclude().size(), "Check for includes size");
        assertTrue(prefer.getInclude().contains("\""), "Check for weird quote in includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check omits count is zero");
        assertFalse(prefer.getHandling().isPresent(), "Check handling");
        assertFalse(prefer.getWait().isPresent(), "Check wait");
        assertFalse(prefer.getRespondAsync(), "Check respond async");
    }

    @Test
    public void testNullPrefer() {
        assertNull(Prefer.valueOf(null), "Check null value");
    }

    private Stream<Executable> checkPreferInclude(final Prefer prefer, final String url) {
        return Stream.of(
                () -> assertEquals(of("representation"), prefer.getPreference(), "Check preference"),
                () -> assertEquals(1L, prefer.getInclude().size(), "Check includes count"),
                () -> assertTrue(prefer.getInclude().contains(url), "Check includes value"),
                () -> assertTrue(prefer.getOmit().isEmpty(), "Check omits count"),
                () -> assertFalse(prefer.getHandling().isPresent(), "Check handling"),
                () -> assertFalse(prefer.getWait().isPresent(), "Check wait"),
                () -> assertFalse(prefer.getRespondAsync(), "Check respond async"));
    }
}
