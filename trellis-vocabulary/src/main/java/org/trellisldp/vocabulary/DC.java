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

import org.apache.commons.rdf.api.IRI;

/**
 * RDF Terms from the Dublin Core Vocabulary
 *
 * @see <a href="http://dublincore.org/documents/dcmi-terms/">DCMI Metadata Terms</a>
 *
 * @author acoburn
 */
public final class DC extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://purl.org/dc/terms/";

    /* Classes */
    public static final IRI Agent = createIRI(URI + "Agent");
    public static final IRI AgentClass = createIRI(URI + "AgentClass");
    public static final IRI BibliographicResource = createIRI(URI + "BibliographicResource");
    public static final IRI FileFormat = createIRI(URI + "FileFormat");
    public static final IRI Frequency = createIRI(URI + "Frequency");
    public static final IRI Jurisdiction = createIRI(URI + "JURIsdiction");
    public static final IRI LicenseDocument = createIRI(URI + "LicenseDocument");
    public static final IRI LinguisticSystem = createIRI(URI + "LinguisticSystem");
    public static final IRI Location = createIRI(URI + "Location");
    public static final IRI LocationPeriodOrJurisdiction = createIRI(URI + "LocationPeriodOrJURIsdiction");
    public static final IRI MediaType = createIRI(URI + "MediaType");
    public static final IRI MediaTypeOrExtent = createIRI(URI + "MediaTypeOrExtent");
    public static final IRI MethodOfAccrual = createIRI(URI + "MethodOfAccrual");
    public static final IRI MethodOfInstruction = createIRI(URI + "MethodOfInstruction");
    public static final IRI PeriodOfTime = createIRI(URI + "PeriodOfTime");
    public static final IRI PhysicalMedium = createIRI(URI + "PhysicalMedium");
    public static final IRI PhysicalResource = createIRI(URI + "PhysicalResource");
    public static final IRI Policy = createIRI(URI + "Policy");
    public static final IRI ProvenanceStatement = createIRI(URI + "ProvenanceStatement");
    public static final IRI RightsStatement = createIRI(URI + "RightsStatement");
    public static final IRI SizeOrDuration = createIRI(URI + "SizeOrDuration");
    public static final IRI Standard = createIRI(URI + "Standard");

    /* Properties */
    public static final IRI abstract_ = createIRI(URI + "abstract");
    public static final IRI accessRights = createIRI(URI + "accessRights");
    public static final IRI accrualMethod = createIRI(URI + "accrualMethod");
    public static final IRI accrualPeriodicity = createIRI(URI + "accrualPeriodicity");
    public static final IRI accrualPolicy = createIRI(URI + "accrualPolicy");
    public static final IRI alternative = createIRI(URI + "alternative");
    public static final IRI audience = createIRI(URI + "audience");
    public static final IRI available = createIRI(URI + "available");
    public static final IRI bibliographicCitation = createIRI(URI + "bibliographicCitation");
    public static final IRI conformsTo = createIRI(URI + "conformsTo");
    public static final IRI contributor = createIRI(URI + "contributor");
    public static final IRI coverage = createIRI(URI + "coverage");
    public static final IRI created = createIRI(URI + "created");
    public static final IRI creator = createIRI(URI + "creator");
    public static final IRI date = createIRI(URI + "date");
    public static final IRI dateAccepted = createIRI(URI + "dateAccepted");
    public static final IRI dateCopyrighted = createIRI(URI + "dateCopyrighted");
    public static final IRI dateSubmitted = createIRI(URI + "dateSubmitted");
    public static final IRI description = createIRI(URI + "description");
    public static final IRI educationLevel = createIRI(URI + "educationLevel");
    public static final IRI extent = createIRI(URI + "extent");
    public static final IRI format = createIRI(URI + "format");
    public static final IRI hasFormat = createIRI(URI + "hasFormat");
    public static final IRI hasPart = createIRI(URI + "hasPart");
    public static final IRI hasVersion = createIRI(URI + "hasVersion");
    public static final IRI identifier = createIRI(URI + "identifier");
    public static final IRI instructionalMethod = createIRI(URI + "instructionalMethod");
    public static final IRI isFormatOf = createIRI(URI + "isFormatOf");
    public static final IRI isPartOf = createIRI(URI + "isPartOf");
    public static final IRI isReferencedBy = createIRI(URI + "isReferencedBy");
    public static final IRI isReplacedBy = createIRI(URI + "isReplacedBy");
    public static final IRI isRequiredBy = createIRI(URI + "isRequiredBy");
    public static final IRI issued = createIRI(URI + "issued");
    public static final IRI isVersionOf = createIRI(URI + "isVersionOf");
    public static final IRI language = createIRI(URI + "language");
    public static final IRI license = createIRI(URI + "license");
    public static final IRI mediator = createIRI(URI + "mediator");
    public static final IRI medium = createIRI(URI + "medium");
    public static final IRI modified = createIRI(URI + "modified");
    public static final IRI provenance = createIRI(URI + "provenance");
    public static final IRI publisher = createIRI(URI + "publisher");
    public static final IRI references = createIRI(URI + "references");
    public static final IRI relation = createIRI(URI + "relation");
    public static final IRI replaces = createIRI(URI + "replaces");
    public static final IRI requires = createIRI(URI + "requires");
    public static final IRI rights = createIRI(URI + "rights");
    public static final IRI rightsHolder = createIRI(URI + "rightsHolder");
    public static final IRI source = createIRI(URI + "source");
    public static final IRI spatial = createIRI(URI + "spatial");
    public static final IRI subject = createIRI(URI + "subject");
    public static final IRI tableOfContents = createIRI(URI + "tableOfContents");
    public static final IRI temporal = createIRI(URI + "temporal");
    public static final IRI title = createIRI(URI + "title");
    public static final IRI type = createIRI(URI + "type");
    public static final IRI valid = createIRI(URI + "valid");

    private DC() {
        super();
    }
}

