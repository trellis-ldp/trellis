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
package org.trellisldp.vocabulary;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.apache.jena.graph.Factory.createDefaultGraph;
import static org.apache.jena.graph.Node.ANY;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RiotException;
import org.apache.jena.riot.RiotNotFoundException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

/**
 * @author acoburn
 */
abstract class AbstractVocabularyTest {

    static final Logger LOGGER = getLogger(AbstractVocabularyTest.class);

    abstract String namespace();

    abstract Class<?> vocabulary();

    boolean isStrict() {
        return true;
    }

    Graph getVocabulary(final String url) {
        final String accept = "text/turtle, application/rdf+xml, application/ld+json";
        final Graph graph = createDefaultGraph();
        try {
            RDFParser.source(url).httpAccept(accept).parse(graph);
        } catch (final HttpException | RiotNotFoundException ex) {
            LOGGER.warn("Could not fetch {}: {}", url, ex.getMessage());
        } catch (final RiotException ex) {
            LOGGER.warn("Error fetching {}: {}", url, ex.getMessage());
        }
        assumeTrue(graph.size() > 0, "Remote vocabulary has no terms! Skip the test for " + url);
        return graph;
    }

    @Test
    void testVocabulary() {
        final Graph graph = getVocabulary(namespace());

        final Set<String> subjects = graph.find(ANY, ANY, ANY).mapWith(Triple::getSubject)
                .filterKeep(Node::isURI).mapWith(Node::getURI).filterKeep(Objects::nonNull).toSet();

        assertTrue(fields().count() > 0, "No fields found in the vocabulary class!");
        fields().forEach(field -> {
            if (isStrict()) {
                assertTrue(subjects.contains(namespace() + field),
                        "Field definition is not in published ontology! " + field);
            } else if (!subjects.contains(namespace() + field)) {
                LOGGER.debug("Field definition is not in published ontology! {}", field);
            }
        });
    }

    @Test
    void testVocabularyRev() {
        final Graph graph = getVocabulary(namespace());

        final Set<String> subjects = fields().map(namespace()::concat).collect(toSet());

        assertFalse(subjects.isEmpty(), "Unable to extract field definitions!");

        graph.find(ANY, ANY, ANY).mapWith(Triple::getSubject).filterKeep(Node::isURI).mapWith(Node::getURI)
                .filterKeep(Objects::nonNull)
                .filterKeep(uri -> uri.startsWith(namespace())).filterDrop(namespace()::equals)
                .filterDrop(subjects::contains)
                .forEachRemaining(uri -> LOGGER.debug("{} not defined in {} class", uri, vocabulary().getName()));
    }

    @Test
    void testNamespace() throws Exception {
        final Method m = vocabulary().getMethod("getNamespace");
        assertEquals(namespace(), m.invoke(null), "Namespaces do not match!");
    }

    private Stream<String> fields() {
        return stream(vocabulary().getFields()).map(Field::getName).map(name ->
                name.endsWith("_") ? name.substring(0, name.length() - 1) : name)
            .map(name -> name.replaceAll("_", "-"));
    }
}
