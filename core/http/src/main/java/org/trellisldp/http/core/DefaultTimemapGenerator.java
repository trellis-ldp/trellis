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

import static java.time.ZonedDateTime.parse;
import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;
import static org.trellisldp.http.core.HttpConstants.DATETIME;
import static org.trellisldp.http.core.HttpConstants.FROM;
import static org.trellisldp.http.core.HttpConstants.MEMENTO;
import static org.trellisldp.http.core.HttpConstants.UNTIL;
import static org.trellisldp.vocabulary.RDF.type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Link;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.rdf.api.Triple;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.vocabulary.Memento;
import org.trellisldp.vocabulary.Time;
import org.trellisldp.vocabulary.XSD;



/**
 * A default TimemapGenerator.
 */
@ApplicationScoped
public class DefaultTimemapGenerator implements TimemapGenerator {

    private static final String TIME_IRI_PREFIX = "http://reference.data.gov.uk/id/gregorian-instant/";
    private static final RDF rdf = RDFFactory.getInstance();

    @Override
    public Stream<Triple> asRdf(final String identifier, final List<Link> mementos) {
        final IRI originalResource = rdf.createIRI(identifier);
        final List<Triple> descriptions = new ArrayList<>();

        descriptions.add(rdf.createTriple(originalResource, type, Memento.OriginalResource));
        descriptions.add(rdf.createTriple(originalResource, type, Memento.TimeGate));
        descriptions.add(rdf.createTriple(originalResource, Memento.timegate, originalResource));
        descriptions.add(rdf.createTriple(originalResource, Memento.timemap,
                    rdf.createIRI(identifier + "?ext=timemap")));

        mementos.stream().filter(link -> link.getRels().contains(MEMENTO))
             .map(link -> rdf.createTriple(originalResource, Memento.memento, rdf.createIRI(link.getUri().toString())))
             .forEach(descriptions::add);

        return Stream.concat(descriptions.stream(), mementos.stream().flatMap(link -> {
            final String linkUri = link.getUri().toString();
            final IRI iri = rdf.createIRI(linkUri);
            final Stream.Builder<Triple> buffer = Stream.builder();

            // TimeMap triples
            if (link.getParams().containsKey(FROM)) {
                buffer.add(rdf.createTriple(iri, type, Memento.TimeMap));
                buffer.add(rdf.createTriple(iri, Time.hasBeginning, rdf.createIRI(TIME_IRI_PREFIX +
                                parse(link.getParams().get(FROM), RFC_1123_DATE_TIME).toString())));
            }
            if (link.getParams().containsKey(UNTIL)) {
                buffer.add(rdf.createTriple(iri, Time.hasEnd, rdf.createIRI(TIME_IRI_PREFIX +
                                parse(link.getParams().get(UNTIL), RFC_1123_DATE_TIME).toString())));
            }

            // Memento triples
            if (isMementoLink(link)) {
                final IRI original = rdf.createIRI(linkUri.split("\\?")[0]);
                final IRI timemapUrl = rdf.createIRI(linkUri.split("\\?")[0] + "?ext=timemap");
                buffer.add(rdf.createTriple(iri, type, Memento.Memento));
                buffer.add(rdf.createTriple(iri, Memento.original, original));
                buffer.add(rdf.createTriple(iri, Memento.timegate, original));
                buffer.add(rdf.createTriple(iri, Memento.timemap, timemapUrl));
                buffer.add(rdf.createTriple(iri, Time.hasTime, rdf.createIRI(TIME_IRI_PREFIX +
                                parse(link.getParams().get(DATETIME), RFC_1123_DATE_TIME).toString())));
                buffer.add(rdf.createTriple(iri, Memento.mementoDatetime, rdf.createLiteral(parse(
                                    link.getParams().get(DATETIME), RFC_1123_DATE_TIME).toString(), XSD.dateTime)));
            }
            return buffer.build();
        }));
    }

    /**
     * Check whether the provided link is to be accepted as a memento link.
     * @param link the link
     * @return true if this is a valid link; false otherwise
     */
    private boolean isMementoLink(final Link link) {
        return link.getRels().contains(MEMENTO) && link.getParams().containsKey(DATETIME);
    }
}
