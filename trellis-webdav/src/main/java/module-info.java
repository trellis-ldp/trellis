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
module org.trellisldp.webdav {
    exports org.trellisldp.webdav;
    exports org.trellisldp.webdav.xml;

    requires org.trellisldp.api;
    requires org.trellisldp.vocabulary;
    requires org.trellisldp.common;

    requires org.apache.commons.rdf.api;
    requires org.apache.commons.lang3;
    requires org.apache.jena.arq;
    requires org.apache.jena.core;
    requires org.slf4j;
    requires microprofile.metrics.api;

    requires microprofile.config.api;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires jakarta.ws.rs;
    requires jakarta.xml.bind;

    opens org.trellisldp.webdav.xml to jakarta.xml.bind;
}
