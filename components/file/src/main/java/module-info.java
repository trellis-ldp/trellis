/*
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

    requires org.apache.commons.io;
    requires org.apache.commons.rdf.api;
    requires org.apache.commons.rdf.jena;
    requires org.apache.jena.arq;
    requires javax.inject;
    requires tamaya.api;
    requires slf4j.api;

    provides org.trellisldp.api.BinaryService
        with org.trellisldp.file.FileBinaryService;
    provides org.trellisldp.api.MementoService
        with org.trellisldp.file.FileMementoService;

    uses org.trellisldp.api.IdentifierService;
    uses org.apache.tamaya.spi.ConfigurationProviderSpi;
}
