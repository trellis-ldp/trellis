/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
module org.trellisldp.test {
    exports org.trellisldp.test;

    requires org.trellisldp.api;
    requires org.trellisldp.common;
    requires org.trellisldp.jena;
    requires org.trellisldp.vocabulary;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.codec;
    requires org.apache.commons.io;
    requires org.apache.commons.rdf.api;
    requires org.apache.jena.arq;
    requires org.apache.jena.commonsrdf;
    requires org.junit.jupiter.api;
    requires org.slf4j;

    requires java.annotation;
    requires java.ws.rs;
    requires java.xml.bind;
    requires jakarta.inject;

    requires awaitility;
    requires smallrye.jwt.build;

    opens org.trellisldp.test;
}
