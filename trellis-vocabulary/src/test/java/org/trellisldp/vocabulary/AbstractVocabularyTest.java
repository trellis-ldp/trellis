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
package org.trellisldp.vocabulary;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.apache.jena.graph.Factory.createDefaultGraph;
import static org.apache.jena.graph.Node.ANY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.lang.reflect.Field;
import java.util.stream.Stream;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.slf4j.Logger;

/**
 * @author acoburn
 */
@RunWith(JUnitPlatform.class)
public abstract class AbstractVocabularyTest {

    private static final Logger LOGGER = getLogger(AbstractVocabularyTest.class);

    private static final String ACCEPT = "text/turtle, application/rdf+xml, application/ld+json";

    public abstract String namespace();

    public abstract Class vocabulary();

    public Boolean isStrict() {
        return true;
    }

    protected Graph getVocabulary(final String url) {
        final Graph graph = createDefaultGraph();
        RDFParser.source(url).httpAccept(ACCEPT).parse(graph);
        return graph;
    }

    @Test
    public void testVocabulary() {
        final Graph graph = getVocabulary(namespace());

        final Set<String> subjects = graph.find(ANY, ANY, ANY).mapWith(Triple::getSubject)
                .filterKeep(Node::isURI).mapWith(Node::getURI).filterKeep(Objects::nonNull).toSet();

        fields().forEach(field -> {
            if (isStrict()) {
                assertTrue(subjects.contains(namespace() + field),
                        "Field definition is not in published ontology! " + field);
            } else if (!subjects.contains(namespace() + field)) {
                LOGGER.warn("Field definition is not in published ontology! {}", field);
            }
        });
    }

    @Test
    public void testVocabularyRev() {
        final Graph graph = getVocabulary(namespace());

        final Set<String> subjects = fields().map(namespace()::concat).collect(toSet());

        assertTrue(subjects.size() > 0, "Unable to extract field definitions!");

        graph.find(ANY, ANY, ANY).mapWith(Triple::getSubject).filterKeep(Node::isURI).mapWith(Node::getURI)
                .filterKeep(Objects::nonNull)
                .filterKeep(uri -> uri.startsWith(namespace())).filterDrop(namespace()::equals)
                .filterDrop(subjects::contains).forEachRemaining(uri -> {
            LOGGER.warn("{} not defined in {} class", uri, vocabulary().getName());
        });
    }

    @Test
    public void testNamespace() throws Exception {
        final Optional<Field> uri = stream(vocabulary().getFields()).filter(field -> field.getName().equals("URI"))
                .findFirst();

        assertTrue(uri.isPresent(), vocabulary().getName() + " does not contain a 'URI' field!");
        assertEquals(namespace(), uri.get().get(null), "Namespaces do not match!");
    }

    private Stream<String> fields() {
        return stream(vocabulary().getFields()).map(Field::getName).map(name ->
                name.endsWith("_") ? name.substring(0, name.length() - 1) : name)
            .map(name -> name.replaceAll("_", "-")).filter(field -> !field.equals("URI"));
    }
}
