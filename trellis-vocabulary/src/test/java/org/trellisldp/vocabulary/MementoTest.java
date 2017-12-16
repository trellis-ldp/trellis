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

import static org.apache.jena.graph.Factory.createDefaultGraph;

import org.apache.jena.graph.Graph;
import org.apache.jena.riot.RDFParser;

/**
 * Test the Memento Vocabulary Class
 * @author acoburn
 */
public class MementoTest extends AbstractVocabularyTest {

    @Override
    public String namespace() {
        return "http://mementoweb.org/ns#";
    }

    @Override
    protected Graph getVocabulary(final String url) {
        final Graph graph = createDefaultGraph();
        // TODO - once the Memento vocabulary supports conneg, this will be unnecessary
        RDFParser.source("http://mementoweb.org/ns.jsonld").parse(graph);
        return graph;
    }

    @Override
    public Class vocabulary() {
        return Memento.class;
    }
}
