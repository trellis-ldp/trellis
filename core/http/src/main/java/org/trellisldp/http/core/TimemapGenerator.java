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

import java.util.List;
import java.util.stream.Stream;

import javax.ws.rs.core.Link;

import org.apache.commons.rdf.api.Triple;

/**
 * A service to generate a stream of Triples from a list of mementos.
 */
public interface TimemapGenerator {

    /**
     * Generate RDF triples from mementos.
     * @param identifier the identifier
     * @param mementos the mementos
     * @return a stream of triples
     */
    Stream<Triple> asRdf(String identifier, List<Link> mementos);

}
