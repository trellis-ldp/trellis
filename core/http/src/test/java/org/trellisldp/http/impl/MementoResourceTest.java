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
package org.trellisldp.http.impl;

import static java.time.Instant.now;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.ofInstant;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static javax.ws.rs.core.Link.TYPE;
import static javax.ws.rs.core.Link.fromUri;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.http.core.HttpConstants.CONFIG_HTTP_MEMENTO_HEADER_DATES;
import static org.trellisldp.http.core.HttpConstants.DATETIME;
import static org.trellisldp.http.core.HttpConstants.FROM;
import static org.trellisldp.http.core.HttpConstants.MEMENTO;
import static org.trellisldp.http.core.HttpConstants.TIMEMAP;
import static org.trellisldp.http.core.HttpConstants.UNTIL;

import javax.ws.rs.core.Link;

import org.junit.jupiter.api.Test;

public class MementoResourceTest {

    @Test
    public void testFilteredMementoLink() {
        final Link link = fromUri("http://example.com/resource/memento/1").rel(MEMENTO)
            .param(DATETIME, ofInstant(now(), UTC).format(RFC_1123_DATE_TIME)).build();
        final boolean filter = true;
        assertTrue(MementoResource.filterLinkParams(link, !filter).getParams().containsKey(DATETIME));
        assertFalse(MementoResource.filterLinkParams(link, filter).getParams().containsKey(DATETIME));
    }

    @Test
    public void testFilteredTimemapLink() {
        final Link link = fromUri("http://example.com/resource/timemap").rel(TIMEMAP)
            .param(FROM, ofInstant(now().minusSeconds(1000), UTC).format(RFC_1123_DATE_TIME))
            .param(UNTIL, ofInstant(now(), UTC).format(RFC_1123_DATE_TIME)).build();
        final boolean filter = true;
        assertTrue(MementoResource.filterLinkParams(link, !filter).getParams().containsKey(FROM));
        assertFalse(MementoResource.filterLinkParams(link, filter).getParams().containsKey(FROM));
        assertTrue(MementoResource.filterLinkParams(link, !filter).getParams().containsKey(UNTIL));
        assertFalse(MementoResource.filterLinkParams(link, filter).getParams().containsKey(UNTIL));
    }

    @Test
    public void testFilteredOtherLink() {
        final Link link = fromUri("http://example.com/resource").rel(TYPE)
            .param(FROM, ofInstant(now().minusSeconds(1000), UTC).format(RFC_1123_DATE_TIME))
            .param(UNTIL, ofInstant(now(), UTC).format(RFC_1123_DATE_TIME)).build();
        final boolean filter = true;
        assertTrue(MementoResource.filterLinkParams(link, !filter).getParams().containsKey(FROM));
        assertTrue(MementoResource.filterLinkParams(link, filter).getParams().containsKey(FROM));
        assertTrue(MementoResource.filterLinkParams(link, !filter).getParams().containsKey(UNTIL));
        assertTrue(MementoResource.filterLinkParams(link, filter).getParams().containsKey(UNTIL));
    }

    @Test
    public void testFilterLinkFromConfiguration() {
        try {
            System.setProperty(CONFIG_HTTP_MEMENTO_HEADER_DATES, "false");
            final MementoResource mr = new MementoResource(null);
            final Link link = fromUri("http://example.com/resource/memento/1").rel(MEMENTO)
                .param(DATETIME, ofInstant(now(), UTC).format(RFC_1123_DATE_TIME)).build();
            assertFalse(mr.filterLinkParams(link).getParams().containsKey(DATETIME));
        } finally {
            System.clearProperty(CONFIG_HTTP_MEMENTO_HEADER_DATES);
        }
    }
}
