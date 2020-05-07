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
package org.trellisldp.http.impl;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.http.core.HttpConstants.DATETIME;
import static org.trellisldp.http.core.HttpConstants.FROM;
import static org.trellisldp.http.core.HttpConstants.MEMENTO;
import static org.trellisldp.http.core.HttpConstants.TIMEMAP;
import static org.trellisldp.http.core.HttpConstants.UNTIL;

import java.time.Instant;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.core.Link;

import org.junit.jupiter.api.Test;

class MementoResourceTest {

    private static final String URL = "http://example.com/resource";

    @Test
    void testFilteredMementoLink() {
        final Link link = fromUri("http://example.com/resource/memento/1").rel(MEMENTO)
            .param(DATETIME, ofInstant(now(), UTC).format(RFC_1123_DATE_TIME)).build();
        assertTrue(MementoResource.filterLinkParams(link, false).getParams().containsKey(DATETIME));
        assertFalse(MementoResource.filterLinkParams(link, true).getParams().containsKey(DATETIME));
    }

    @Test
    void testFilteredTimemapLink() {
        final Link link = fromUri("http://example.com/resource/timemap").rel(TIMEMAP)
            .param(FROM, ofInstant(now().minusSeconds(1000), UTC).format(RFC_1123_DATE_TIME))
            .param(UNTIL, ofInstant(now(), UTC).format(RFC_1123_DATE_TIME)).build();
        assertTrue(MementoResource.filterLinkParams(link, false).getParams().containsKey(FROM));
        assertFalse(MementoResource.filterLinkParams(link, true).getParams().containsKey(FROM));
        assertTrue(MementoResource.filterLinkParams(link, false).getParams().containsKey(UNTIL));
        assertFalse(MementoResource.filterLinkParams(link, true).getParams().containsKey(UNTIL));
    }

    @Test
    void testFilteredOtherLink() {
        final Link link = fromUri(URL).rel(TYPE)
            .param(FROM, ofInstant(now().minusSeconds(1000), UTC).format(RFC_1123_DATE_TIME))
            .param(UNTIL, ofInstant(now(), UTC).format(RFC_1123_DATE_TIME)).build();
        assertTrue(MementoResource.filterLinkParams(link, false).getParams().containsKey(FROM));
        assertTrue(MementoResource.filterLinkParams(link, true).getParams().containsKey(FROM));
        assertTrue(MementoResource.filterLinkParams(link, false).getParams().containsKey(UNTIL));
        assertTrue(MementoResource.filterLinkParams(link, true).getParams().containsKey(UNTIL));
    }

    @Test
    void testFilterLinkFromConfiguration() {
        final MementoResource mr = new MementoResource(null, false);
        final Link link = fromUri("http://example.com/resource/memento/1").rel(MEMENTO)
            .param(DATETIME, ofInstant(now(), UTC).format(RFC_1123_DATE_TIME)).build();
        assertFalse(mr.filterLinkParams(link).getParams().containsKey(DATETIME));
    }

    @Test
    void testMementoHeadersSingle() {
        final SortedSet<Instant> mementos = new TreeSet<>();
        final Instant time = now();
        mementos.add(time);

        final List<Link> links = MementoResource.getMementoHeaders(URL, mementos, time).collect(toList());
        assertEquals(2L, links.size());
        checkMementoHeaders(links, 0L, 0L, 1L);
    }

    @Test
    void testMementoHeadersMultipleFirst() {
        final SortedSet<Instant> mementos = new TreeSet<>();
        final Instant time = now();
        mementos.add(time);
        mementos.add(time.plusSeconds(1L));
        mementos.add(time.plusSeconds(2L));

        final List<Link> links = MementoResource.getMementoHeaders(URL, mementos, time).collect(toList());
        assertEquals(4L, links.size());
        checkMementoHeaders(links, 0L, 1L, 3L);
    }


    @Test
    void testMementoHeadersMultipleMiddle() {
        final SortedSet<Instant> mementos = new TreeSet<>();
        final Instant time = now();
        mementos.add(time);
        mementos.add(time.plusSeconds(1L));
        mementos.add(time.plusSeconds(2L));

        final List<Link> links = MementoResource.getMementoHeaders(URL, mementos, time.plusSeconds(1L))
            .collect(toList());
        assertEquals(3L, links.size());
        checkMementoHeaders(links, 1L, 1L, 2L);
    }

    @Test
    void testMementoHeadersMultipleLast() {
        final SortedSet<Instant> mementos = new TreeSet<>();
        final Instant time = now();
        mementos.add(time);
        mementos.add(time.plusSeconds(1L));
        mementos.add(time.plusSeconds(2L));

        final List<Link> links = MementoResource.getMementoHeaders(URL, mementos, time.plusSeconds(2L))
            .collect(toList());
        assertEquals(4L, links.size());
        checkMementoHeaders(links, 1L, 0L, 3L);
    }

    @Test
    void testMementoHeadersMultipleBeyond() {
        final SortedSet<Instant> mementos = new TreeSet<>();
        final Instant time = now();
        mementos.add(time);
        mementos.add(time.plusSeconds(1L));
        mementos.add(time.plusSeconds(2L));

        final List<Link> links = MementoResource.getMementoHeaders(URL, mementos, time.plusSeconds(3L))
            .collect(toList());
        assertEquals(3L, links.size());
        checkMementoHeaders(links, 0L, 0L, 2L);
    }

    @Test
    void testMementoHeadersMultiplePrecede() {
        final SortedSet<Instant> mementos = new TreeSet<>();
        final Instant time = now();
        mementos.add(time);
        mementos.add(time.plusSeconds(1L));
        mementos.add(time.plusSeconds(2L));

        final List<Link> links = MementoResource.getMementoHeaders(URL, mementos, time.minusSeconds(1L))
            .collect(toList());
        assertEquals(3L, links.size());
        checkMementoHeaders(links, 0L, 0L, 2L);
}

    @Test
    void testMementoLinksEmpty() {
        final SortedSet<Instant> mementos = new TreeSet<>();
        final List<Link> links = MementoResource.getMementoLinks(URL, mementos).collect(toList());
        assertTrue(links.isEmpty());
    }

    @Test
    void testMementoLinksSingle() {
        final SortedSet<Instant> mementos = new TreeSet<>();
        final Instant time = now();
        mementos.add(time);
        final List<Link> links = MementoResource.getMementoLinks(URL, mementos).collect(toList());
        assertEquals(3L, links.size());
        checkMementoHeaders(links, 0L, 0L, 1L);
        assertEquals(1L, links.stream().filter(l -> l.getRels().contains("timegate")).count());
        assertEquals(1L, links.stream().filter(l -> l.getRels().contains("original")).count());
    }

    private void checkMementoHeaders(final List<Link> links, final long prev, final long next, final long mementos) {
        assertEquals(1L, links.stream().filter(l -> l.getRels().contains("timemap")).count());
        assertEquals(1L, links.stream().filter(l -> l.getRels().contains("first")).count());
        assertEquals(1L, links.stream().filter(l -> l.getRels().contains("last")).count());
        assertEquals(prev, links.stream().filter(l -> l.getRels().contains("prev")).count());
        assertEquals(next, links.stream().filter(l -> l.getRels().contains("next")).count());
        assertEquals(mementos, links.stream().filter(l -> l.getRels().contains("memento")).count());
    }
}
