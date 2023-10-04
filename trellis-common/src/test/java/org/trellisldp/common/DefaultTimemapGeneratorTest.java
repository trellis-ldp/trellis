/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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

import static jakarta.ws.rs.core.Link.fromUri;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;
import static org.trellisldp.vocabulary.RDF.type;

import jakarta.ws.rs.core.Link;

import java.util.List;

import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.Memento;
import org.trellisldp.vocabulary.Time;

/**
 * @author acoburn
 */
class DefaultTimemapGeneratorTest {

    private static final RDF rdf = RDFFactory.getInstance();

    @Test
    void testIsMementoLink() {
        final String url = "http://example.com/resource/memento";
        final TimemapGenerator svc = new DefaultTimemapGenerator();
        final List<Link> links = asList(
            fromUri(url).rel("memento").param("datetime", "Fri, 11 May 2018 15:29:25 GMT").build(),
            fromUri(url).rel("foo").param("datetime", "Fri, 11 May 2018 15:39:25 GMT").build(),
            fromUri(url).rel("memento").param("bar", "Fri, 11 May 2018 15:49:25 GMT").build());
        // 4 standard links + 2 memento links + 6 for each memento link w/ datetime
        assertEquals(12L, svc.asRdf(url, links).count());
    }

    @Test
    void testTimeMapLink() {
        final String url = "http://example.com/resource/memento";
        final Link link = fromUri(url).rel("timemap").param("from", "Fri, 11 May 2018 15:29:25 GMT")
            .param("until", "Fri, 11 May 2018 20:12:47 GMT").build();
        final List<Triple> triples = DefaultTimemapGenerator.buildTriplesFromLink(link).collect(toList());
        assertEquals(3L, triples.size());
        assertTrue(triples.stream().anyMatch(triple -> rdf.createIRI(url).equals(triple.getSubject()) &&
                    type.equals(triple.getPredicate()) && Memento.TimeMap.equals(triple.getObject())));
        assertTrue(triples.stream().anyMatch(triple -> rdf.createIRI(url).equals(triple.getSubject()) &&
                    Time.hasBeginning.equals(triple.getPredicate())));
        assertTrue(triples.stream().anyMatch(triple -> rdf.createIRI(url).equals(triple.getSubject()) &&
                    Time.hasEnd.equals(triple.getPredicate())));
    }
}
