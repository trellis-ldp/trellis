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
module org.trellisldp.file {
    exports org.trellisldp.file;

    requires transitive org.trellisldp.api;
    requires transitive org.trellisldp.vocabulary;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.apache.commons.io;
    requires org.apache.commons.rdf.api;
    requires org.apache.jena.commonsrdf;
    requires org.apache.jena.arq;
    requires org.slf4j;
    requires jakarta.cdi;
    requires jakarta.inject;
    requires jakarta.annotation;
    requires microprofile.config.api;
    requires org.apache.commons.codec;
    requires org.apache.jena.core;

    opens org.trellisldp.file;

    provides org.trellisldp.api.BinaryService
        with org.trellisldp.file.FileBinaryService;
    provides org.trellisldp.api.MementoService
        with org.trellisldp.file.FileMementoService;
    provides org.trellisldp.api.NamespaceService
        with org.trellisldp.file.FileNamespaceService;
}
