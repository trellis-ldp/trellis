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
import static org.trellisldp.http.core.Prefer.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

/**
 * @author acoburn
 */
class PreferTest {

    private static final String CHECK_ASYNC = "Check respond async";
    private static final String CHECK_HANDLING = "Check handling";
    private static final String CHECK_NO_OMIT = "Check for no omits";
    private static final String CHECK_NO_INCLUDE = "Check for no includes";
    private static final String CHECK_PREF_TYPE = "Check preference type";
    private static final String CHECK_SIMPLE_PARSING = "Check simple Prefer parsing";
    private static final String URL = "http://example.org/test";

    @Test
    void testPreferNullValues() {
        final Prefer prefer = new Prefer(null, null, null, null, null);
        assertFalse(prefer.getPreference().isPresent());
        assertFalse(prefer.getHandling().isPresent());
        assertFalse(prefer.getRespondAsync());
        assertTrue(prefer.getInclude().isEmpty());
        assertTrue(prefer.getOmit().isEmpty());
    }

    @Test
    void testPrefer1() {
        final Prefer prefer = valueOf("return=representation; include=\"http://example.org/test\"");
        assertAll(CHECK_SIMPLE_PARSING, checkPreferInclude(prefer, URL));
    }

    @Test
    void testPrefer1b() {
        final Prefer prefer = ofInclude(URL);
        assertAll(CHECK_SIMPLE_PARSING, checkPreferInclude(prefer, URL));
    }

    @Test
    void testPrefer1c() {
        final Prefer prefer = valueOf("return=representation; include=http://example.org/test");
        assertAll(CHECK_SIMPLE_PARSING, checkPreferInclude(prefer, URL));
    }

    @Test
    void testPrefer2() {
        final Prefer prefer = valueOf("return  =  representation;   include =  \"http://example.org/test\"");
        assertAll(CHECK_SIMPLE_PARSING, checkPreferInclude(prefer, URL));
    }

    @Test
    void testPrefer3() {
        final Prefer prefer = valueOf("return=minimal");
        assertEquals(of(PREFER_MINIMAL), prefer.getPreference(), CHECK_PREF_TYPE);
        assertTrue(prefer.getInclude().isEmpty(), "Check that there are no includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check omits count is zero");
        assertFalse(prefer.getHandling().isPresent(), CHECK_HANDLING);
        assertFalse(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testPrefer4() {
        final Prefer prefer = valueOf("return=other");
        assertTrue(prefer.getInclude().isEmpty(), "Check includes is empty");
        assertTrue(prefer.getOmit().isEmpty(), "Check omits is empty");
        assertFalse(prefer.getPreference().isPresent(), CHECK_PREF_TYPE);
        assertFalse(prefer.getHandling().isPresent(), CHECK_HANDLING);
        assertFalse(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testPrefer5() {
        final Prefer prefer = valueOf("return=representation; omit=\"http://example.org/test\"");
        assertEquals(of(PREFER_REPRESENTATION), prefer.getPreference(), CHECK_PREF_TYPE);
        assertTrue(prefer.getInclude().isEmpty(), CHECK_NO_INCLUDE);
        assertFalse(prefer.getOmit().isEmpty(), "Check for omits");
        assertTrue(prefer.getOmit().contains(URL), "Check omit value");
        assertFalse(prefer.getHandling().isPresent(), CHECK_HANDLING);
        assertFalse(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testPrefer5b() {
        final Prefer prefer = ofOmit(URL);
        assertEquals(of(PREFER_REPRESENTATION), prefer.getPreference(), CHECK_PREF_TYPE);
        assertTrue(prefer.getInclude().isEmpty(), CHECK_NO_INCLUDE);
        assertFalse(prefer.getOmit().isEmpty(), "Check for omit values");
        assertTrue(prefer.getOmit().contains(URL), "Check omit value");
        assertFalse(prefer.getHandling().isPresent(), CHECK_HANDLING);
        assertFalse(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testPrefer6() {
        final Prefer prefer = valueOf("handling=lenient; return=minimal");
        assertTrue(prefer.getInclude().isEmpty(), CHECK_NO_INCLUDE);
        assertTrue(prefer.getOmit().isEmpty(), CHECK_NO_OMIT);
        assertEquals(of(PREFER_MINIMAL), prefer.getPreference(), CHECK_PREF_TYPE);
        assertEquals(of(PREFER_LENIENT), prefer.getHandling(), CHECK_HANDLING);
        assertFalse(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testPrefer7() {
        final Prefer prefer = valueOf("respond-async; random-param");
        assertTrue(prefer.getInclude().isEmpty(), CHECK_NO_INCLUDE);
        assertTrue(prefer.getOmit().isEmpty(), CHECK_NO_OMIT);
        assertFalse(prefer.getPreference().isPresent(), CHECK_PREF_TYPE);
        assertFalse(prefer.getHandling().isPresent(), CHECK_HANDLING);
        assertTrue(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testPrefer8() {
        final Prefer prefer = valueOf("handling=strict; return=minimal");
        assertTrue(prefer.getInclude().isEmpty(), CHECK_NO_INCLUDE);
        assertTrue(prefer.getOmit().isEmpty(), CHECK_NO_OMIT);
        assertEquals(of(PREFER_MINIMAL), prefer.getPreference(), CHECK_PREF_TYPE);
        assertEquals(of(PREFER_STRICT), prefer.getHandling(), CHECK_HANDLING);
        assertFalse(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testPrefer9() {
        final Prefer prefer = valueOf("handling=blah; return=minimal");
        assertTrue(prefer.getInclude().isEmpty(), CHECK_NO_INCLUDE);
        assertTrue(prefer.getOmit().isEmpty(), CHECK_NO_OMIT);
        assertEquals(of(PREFER_MINIMAL), prefer.getPreference(), CHECK_PREF_TYPE);
        assertFalse(prefer.getHandling().isPresent(), CHECK_HANDLING);
        assertFalse(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testRoundTrip1() {
        final Prefer prefer = valueOf("return=minimal; handling=lenient; respond-async");
        final String prefString = prefer.toString();
        final Prefer prefer2 = valueOf(prefString);
        assertEquals(prefer.getInclude(), prefer2.getInclude());
        assertEquals(prefer.getOmit(), prefer2.getOmit());
        assertEquals(prefer.getPreference(), prefer2.getPreference());
        assertEquals(prefer.getHandling(), prefer2.getHandling());
        assertEquals(prefer.getRespondAsync(), prefer2.getRespondAsync());
    }

    @Test
    void testRoundTrip2() {
        final Prefer prefer = valueOf("return=representation; include=\"https://example.com/Prefer\"");
        final String prefString = prefer.toString();
        final Prefer prefer2 = valueOf(prefString);
        assertEquals(prefer.getInclude(), prefer2.getInclude());
        assertEquals(prefer.getOmit(), prefer2.getOmit());
        assertEquals(prefer.getPreference(), prefer2.getPreference());
        assertEquals(prefer.getHandling(), prefer2.getHandling());
        assertEquals(prefer.getRespondAsync(), prefer2.getRespondAsync());
    }

    @Test
    void testRoundTrip3() {
        final Prefer prefer = valueOf("return=representation; omit=\"https://example.com/Prefer\"");
        final String prefString = prefer.toString();
        final Prefer prefer2 = valueOf(prefString);
        assertEquals(prefer.getInclude(), prefer2.getInclude());
        assertEquals(prefer.getOmit(), prefer2.getOmit());
        assertEquals(prefer.getPreference(), prefer2.getPreference());
        assertEquals(prefer.getHandling(), prefer2.getHandling());
        assertEquals(prefer.getRespondAsync(), prefer2.getRespondAsync());
    }

    @Test
    void testStaticInclude() {
        final Prefer prefer = ofInclude();
        assertEquals(of(PREFER_REPRESENTATION), prefer.getPreference(), CHECK_PREF_TYPE);
        assertTrue(prefer.getInclude().isEmpty(), CHECK_NO_INCLUDE);
        assertTrue(prefer.getOmit().isEmpty(), CHECK_NO_OMIT);
    }

    @Test
    void testStaticOmit() {
        final Prefer prefer = ofOmit();
        assertEquals(of(PREFER_REPRESENTATION), prefer.getPreference(), CHECK_PREF_TYPE);
        assertTrue(prefer.getInclude().isEmpty(), CHECK_NO_INCLUDE);
        assertTrue(prefer.getOmit().isEmpty(), CHECK_NO_OMIT);
    }

    @Test
    void testPreferBadQuotes() {
        final Prefer prefer = valueOf("return=representation; include=\"http://example.org/test");
        assertEquals(of(PREFER_REPRESENTATION), prefer.getPreference(), CHECK_PREF_TYPE);
        assertEquals(1L, prefer.getInclude().size(), "Check includes count");
        assertTrue(prefer.getInclude().contains("\"http://example.org/test"));
        assertTrue(prefer.getOmit().isEmpty(), "Check omits count is zero");
        assertFalse(prefer.getHandling().isPresent(), CHECK_HANDLING);
        assertFalse(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testPreferBadQuotes2() {
        final Prefer prefer = valueOf("return=representation; include=\"");
        assertEquals(of(PREFER_REPRESENTATION), prefer.getPreference(), CHECK_PREF_TYPE);
        assertEquals(1L, prefer.getInclude().size(), "Check for includes size");
        assertTrue(prefer.getInclude().contains("\""), "Check for weird quote in includes");
        assertTrue(prefer.getOmit().isEmpty(), "Check omits count is zero");
        assertFalse(prefer.getHandling().isPresent(), CHECK_HANDLING);
        assertFalse(prefer.getRespondAsync(), CHECK_ASYNC);
    }

    @Test
    void testNullPrefer() {
        assertNull(valueOf(null), "Check null value");
    }

    private Stream<Executable> checkPreferInclude(final Prefer prefer, final String url) {
        return Stream.of(
                () -> assertEquals(of(PREFER_REPRESENTATION), prefer.getPreference(), CHECK_PREF_TYPE),
                () -> assertEquals(1L, prefer.getInclude().size(), "Check includes count"),
                () -> assertTrue(prefer.getInclude().contains(url), "Check includes value"),
                () -> assertTrue(prefer.getOmit().isEmpty(), "Check omits count"),
                () -> assertFalse(prefer.getHandling().isPresent(), CHECK_HANDLING),
                () -> assertFalse(prefer.getRespondAsync(), CHECK_ASYNC));
    }
}
