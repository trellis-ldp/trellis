/*
 * Copyright (c) Aaron Coburn and individual contributors
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

import static org.apache.jena.graph.Factory.createDefaultGraph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.riot.RDFParserRegistry;
import org.apache.jena.riot.ReaderRIOT;
import org.apache.jena.riot.ReaderRIOTFactory;
import org.apache.jena.riot.lang.LangJSONLD10;
import org.apache.jena.riot.system.ParserProfile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
class ASTest extends AbstractVocabularyTest {

    @BeforeAll
    static void setup() {
        final var jsonldParser = new ReaderRIOTFactoryJSONLD10();
        RDFParserRegistry.registerLangTriples(Lang.JSONLD, jsonldParser);
        RDFParserRegistry.registerLangQuads(Lang.JSONLD, jsonldParser);
    }

    @Override
    String namespace() {
        return "https://www.w3.org/ns/activitystreams#";
    }

    @Override
    Class<AS> vocabulary() {
        return AS.class;
    }

    @Override
    Graph getVocabulary(final String url) {
        final Graph graph = createDefaultGraph();
        try {
            RDFParser.source(url).httpAccept("application/ld+json").parse(graph);
        } catch (final HttpException ex) {
            LOGGER.warn("Could not fetch {}: {}", url, ex.getMessage());
            assumeTrue(false, "Error fetching the URL (" + url + "): skip the test");
        }
        return graph;
    }

    @Test
    @Override
    void testVocabularyRev() {
        assertEquals(namespace() + "Create", AS.Create.getIRIString(), "as:Create IRIs don't match!");
        assertEquals(namespace() + "Delete", AS.Delete.getIRIString(), "as:Delete IRIs don't match!");
        assertEquals(namespace() + "Update", AS.Update.getIRIString(), "as:Update IRIs don't match!");
    }

    @Test
    @Override
    void testVocabulary() {
        assertEquals(namespace() + "Activity", AS.Activity.getIRIString(), "as:Activity IRIs don't match!");
    }

    @Test
    void checkUri() {
        getVocabulary(namespace());
        assertEquals(namespace(), AS.getNamespace(), "AS namespace doesn't match expected value!");
    }

    private static final class ReaderRIOTFactoryJSONLD10 implements ReaderRIOTFactory {
        @Override
        public ReaderRIOT create(final Lang language, final ParserProfile profile) {
            // force the use of jsonld-java (i.e., JSON-LD 1.0)
            return new LangJSONLD10(language, profile, profile.getErrorHandler());
        }
    }
}
