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
module org.trellisldp.webac {
    exports org.trellisldp.webac;

    requires org.trellisldp.api;
    requires org.trellisldp.common;
    requires org.trellisldp.vocabulary;

    requires org.apache.commons.rdf.api;
    requires org.apache.jena.core;
    requires org.apache.jena.arq;
    requires org.apache.jena.commonsrdf;
    requires org.slf4j;

    requires jakarta.cdi;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires jakarta.xml.bind;
    requires jakarta.ws.rs;
    requires microprofile.config.api;
    requires microprofile.metrics.api;

    uses org.trellisldp.api.ResourceService;
    uses org.trellisldp.api.IOService;

    opens org.trellisldp.webac;
}
