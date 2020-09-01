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

import static java.util.ServiceLoader.load;

import org.apache.commons.rdf.api.RDF;

/**
 * A factory method for loading and providing the RDF Commons implementation object.
 */
public final class RDFFactory {

    private static final RDF rdf = load(RDF.class).findFirst()
                    .orElseThrow(() -> new TrellisRuntimeException("No RDF Commons implementation available!"));

    /**
     * Get the Commons RDF instance in use.
     *
     * @return the RDF instance
     */
    public static RDF getInstance() {
        return rdf;
    }

    private RDFFactory() {
        // Prevent instantiation.
    }
}
