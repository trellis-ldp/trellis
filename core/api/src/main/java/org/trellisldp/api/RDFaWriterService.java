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

import java.io.OutputStream;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.Triple;

/**
 * A service for generating HTML output from a stream of triples.
 */
public interface RDFaWriterService {

    /**
     * Produce RDFa (HTML) output from a given stream of triples.
     * @param triples the triples
     * @param output the output stream
     * @param subject the subject of the resource, may be {@code null}
     */
    void write(Stream<Triple> triples, OutputStream output, String subject);
}
