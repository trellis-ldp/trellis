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

import javax.ws.rs.core.MediaType;

/**
 * RDF-based {@link MediaType} definitions.
 *
 * @author acoburn
 */
public final class RdfMediaType {

    private static final String APPLICATION = "application";

    public static final String APPLICATION_LD_JSON = APPLICATION + "/ld+json";

    public static final MediaType APPLICATION_LD_JSON_TYPE = new MediaType(APPLICATION, "ld+json");

    public static final String APPLICATION_N_TRIPLES = APPLICATION + "/n-triples";

    public static final MediaType APPLICATION_N_TRIPLES_TYPE = new MediaType(APPLICATION, "n-triples");

    public static final String APPLICATION_SPARQL_UPDATE = APPLICATION + "/sparql-update";

    public static final MediaType APPLICATION_SPARQL_UPDATE_TYPE = new MediaType(APPLICATION, "sparql-update");

    public static final String TEXT_TURTLE = "text/turtle;charset=utf-8";

    public static final MediaType TEXT_TURTLE_TYPE = new MediaType("text", "turtle", "utf-8");

    private RdfMediaType() {
        // prevent instantiation
    }
}
