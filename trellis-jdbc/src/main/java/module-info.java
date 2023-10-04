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
module org.trellisldp.jdbc {
    exports org.trellisldp.jdbc;

    requires transitive org.trellisldp.api;
    requires transitive org.trellisldp.vocabulary;

    requires org.apache.commons.lang3;
    requires org.apache.commons.rdf.api;
    requires org.apache.jena.arq;
    requires org.apache.jena.core;
    requires org.apache.jena.commonsrdf;
    requires org.jdbi.v3.core;
    requires org.slf4j;

    requires jakarta.cdi;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires java.naming;
    requires java.sql;
    requires microprofile.config.api;
    requires microprofile.health.api;

    provides org.trellisldp.api.ResourceService
        with org.trellisldp.jdbc.DBResourceService;

    uses org.trellisldp.api.IdentifierService;
    uses org.trellisldp.api.MementoService;

    opens org.trellisldp.jdbc;
}
