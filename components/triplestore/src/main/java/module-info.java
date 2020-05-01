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
module org.trellisldp.triplestore {
    exports org.trellisldp.triplestore;

    requires transitive org.trellisldp.api;
    requires transitive org.trellisldp.vocabulary;

    requires org.apache.commons.io;
    requires org.apache.commons.rdf.api;
    requires org.apache.commons.rdf.jena;
    requires org.apache.jena.arq;
    requires org.slf4j;

    requires jakarta.enterprise.cdi.api;
    requires jakarta.inject;
    requires java.annotation;
    requires microprofile.config.api;
    requires microprofile.health.api;
    requires org.apache.jena.rdfconnection;
    requires org.apache.jena.core;
    requires org.apache.jena.tdb2;

    provides org.trellisldp.api.ResourceService
        with org.trellisldp.triplestore.TriplestoreResourceService;

    uses org.trellisldp.api.IdentifierService;
}
