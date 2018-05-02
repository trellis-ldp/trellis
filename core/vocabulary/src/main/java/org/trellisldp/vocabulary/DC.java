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
package org.trellisldp.vocabulary;

import static org.trellisldp.vocabulary.VocabUtils.createIRI;

import org.apache.commons.rdf.api.IRI;

/**
 * RDF Terms from the Dublin Core Vocabulary.
 *
 * @see <a href="http://dublincore.org/documents/dcmi-terms/">DCMI Metadata Terms</a>
 *
 * @author acoburn
 */
public final class DC {

    /* Namespace */
    public static final String NS = "http://purl.org/dc/terms/";

    /* Classes */
    public static final IRI Agent = createIRI(NS + "Agent");
    public static final IRI AgentClass = createIRI(NS + "AgentClass");
    public static final IRI BibliographicResource = createIRI(NS + "BibliographicResource");
    public static final IRI Box = createIRI(NS + "Box");
    public static final IRI DCMIType = createIRI(NS + "DCMIType");
    public static final IRI DDC = createIRI(NS + "DDC");
    public static final IRI FileFormat = createIRI(NS + "FileFormat");
    public static final IRI Frequency = createIRI(NS + "Frequency");
    public static final IRI IMT = createIRI(NS + "IMT");
    public static final IRI ISO3166 = createIRI(NS + "ISO3166");
    public static final IRI Jurisdiction = createIRI(NS + "Jurisdiction");
    public static final IRI LCC = createIRI(NS + "LCC");
    public static final IRI LCSH = createIRI(NS + "LCSH");
    public static final IRI LicenseDocument = createIRI(NS + "LicenseDocument");
    public static final IRI LinguisticSystem = createIRI(NS + "LinguisticSystem");
    public static final IRI Location = createIRI(NS + "Location");
    public static final IRI LocationPeriodOrJurisdiction = createIRI(NS + "LocationPeriodOrJurisdiction");
    public static final IRI MESH = createIRI(NS + "MESH");
    public static final IRI MediaType = createIRI(NS + "MediaType");
    public static final IRI MediaTypeOrExtent = createIRI(NS + "MediaTypeOrExtent");
    public static final IRI MethodOfAccrual = createIRI(NS + "MethodOfAccrual");
    public static final IRI MethodOfInstruction = createIRI(NS + "MethodOfInstruction");
    public static final IRI NLM = createIRI(NS + "NLM");
    public static final IRI Period = createIRI(NS + "Period");
    public static final IRI PeriodOfTime = createIRI(NS + "PeriodOfTime");
    public static final IRI PhysicalMedium = createIRI(NS + "PhysicalMedium");
    public static final IRI PhysicalResource = createIRI(NS + "PhysicalResource");
    public static final IRI Point = createIRI(NS + "Point");
    public static final IRI Policy = createIRI(NS + "Policy");
    public static final IRI ProvenanceStatement = createIRI(NS + "ProvenanceStatement");
    public static final IRI RFC1766 = createIRI(NS + "RFC1766");
    public static final IRI RFC3066 = createIRI(NS + "RFC3066");
    public static final IRI RFC4646 = createIRI(NS + "RFC4646");
    public static final IRI RFC5646 = createIRI(NS + "RFC5646");
    public static final IRI RightsStatement = createIRI(NS + "RightsStatement");
    public static final IRI SizeOrDuration = createIRI(NS + "SizeOrDuration");
    public static final IRI Standard = createIRI(NS + "Standard");
    public static final IRI TGN = createIRI(NS + "TGN");
    public static final IRI UDC = createIRI(NS + "UDC");
    public static final IRI URI = createIRI(NS + "URI");
    public static final IRI W3CDTF = createIRI(NS + "W3CDTF");

    /* Properties */
    public static final IRI abstract_ = createIRI(NS + "abstract");
    public static final IRI accessRights = createIRI(NS + "accessRights");
    public static final IRI accrualMethod = createIRI(NS + "accrualMethod");
    public static final IRI accrualPeriodicity = createIRI(NS + "accrualPeriodicity");
    public static final IRI accrualPolicy = createIRI(NS + "accrualPolicy");
    public static final IRI alternative = createIRI(NS + "alternative");
    public static final IRI audience = createIRI(NS + "audience");
    public static final IRI available = createIRI(NS + "available");
    public static final IRI bibliographicCitation = createIRI(NS + "bibliographicCitation");
    public static final IRI conformsTo = createIRI(NS + "conformsTo");
    public static final IRI contributor = createIRI(NS + "contributor");
    public static final IRI coverage = createIRI(NS + "coverage");
    public static final IRI created = createIRI(NS + "created");
    public static final IRI creator = createIRI(NS + "creator");
    public static final IRI date = createIRI(NS + "date");
    public static final IRI dateAccepted = createIRI(NS + "dateAccepted");
    public static final IRI dateCopyrighted = createIRI(NS + "dateCopyrighted");
    public static final IRI dateSubmitted = createIRI(NS + "dateSubmitted");
    public static final IRI description = createIRI(NS + "description");
    public static final IRI educationLevel = createIRI(NS + "educationLevel");
    public static final IRI extent = createIRI(NS + "extent");
    public static final IRI format = createIRI(NS + "format");
    public static final IRI hasFormat = createIRI(NS + "hasFormat");
    public static final IRI hasPart = createIRI(NS + "hasPart");
    public static final IRI hasVersion = createIRI(NS + "hasVersion");
    public static final IRI identifier = createIRI(NS + "identifier");
    public static final IRI instructionalMethod = createIRI(NS + "instructionalMethod");
    public static final IRI isFormatOf = createIRI(NS + "isFormatOf");
    public static final IRI isPartOf = createIRI(NS + "isPartOf");
    public static final IRI isReferencedBy = createIRI(NS + "isReferencedBy");
    public static final IRI isReplacedBy = createIRI(NS + "isReplacedBy");
    public static final IRI isRequiredBy = createIRI(NS + "isRequiredBy");
    public static final IRI issued = createIRI(NS + "issued");
    public static final IRI isVersionOf = createIRI(NS + "isVersionOf");
    public static final IRI language = createIRI(NS + "language");
    public static final IRI license = createIRI(NS + "license");
    public static final IRI mediator = createIRI(NS + "mediator");
    public static final IRI medium = createIRI(NS + "medium");
    public static final IRI modified = createIRI(NS + "modified");
    public static final IRI provenance = createIRI(NS + "provenance");
    public static final IRI publisher = createIRI(NS + "publisher");
    public static final IRI references = createIRI(NS + "references");
    public static final IRI relation = createIRI(NS + "relation");
    public static final IRI replaces = createIRI(NS + "replaces");
    public static final IRI requires = createIRI(NS + "requires");
    public static final IRI rights = createIRI(NS + "rights");
    public static final IRI rightsHolder = createIRI(NS + "rightsHolder");
    public static final IRI source = createIRI(NS + "source");
    public static final IRI spatial = createIRI(NS + "spatial");
    public static final IRI subject = createIRI(NS + "subject");
    public static final IRI tableOfContents = createIRI(NS + "tableOfContents");
    public static final IRI temporal = createIRI(NS + "temporal");
    public static final IRI title = createIRI(NS + "title");
    public static final IRI type = createIRI(NS + "type");
    public static final IRI valid = createIRI(NS + "valid");

    private DC() {
        // prevent instantiation
    }
}

