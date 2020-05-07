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
package org.trellisldp.api;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;

/**
 * The IOService defines methods for reading, writing and updating RDF streams
 * to/from a concrete RDF 1.1 syntax.
 *
 * @author acoburn
 */
public interface IOService {

    /**
     * Serialize the triple stream in a concrete RDF syntax.
     *
     * @param triples the stream of triples
     * @param output the output stream
     * @param syntax the output format
     * @param context the context to resolve relative IRIs
     * @param profiles additional profile information used for output
     */
    void write(Stream<Triple> triples, OutputStream output, RDFSyntax syntax, String context, IRI... profiles);

    /**
     * Read an input stream into a stream of triples.
     *
     * @param input the input stream
     * @param syntax the RDF syntax
     * @param context the RDF context
     * @return a stream of triples
     */
    Stream<Triple> read(InputStream input, RDFSyntax syntax, String context);

    /**
     * Apply a Sparql-Update operation over a Graph.
     *
     * @param graph the input graph
     * @param update the sparql-update request
     * @param syntax the RDF syntax
     * @param context the context to resolve relative IRIs
     */
    void update(Graph graph, String update, RDFSyntax syntax, String context);

    /**
     * Retrieve the set of valid syntaxes for read operations.
     *
     * @return the syntaxes for reading resources.
     */
    List<RDFSyntax> supportedReadSyntaxes();

    /**
     * Retrieve the set of valid syntaxes for write operations.
     *
     * @return the syntaxes for writing resources.
     */
    List<RDFSyntax> supportedWriteSyntaxes();

    /**
     * Retrieve the set of valid syntaxes for update operations.
     *
     * @return the syntaxes for updating resources.
     */
    List<RDFSyntax> supportedUpdateSyntaxes();
}
