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
    private static final String URI = "http://purl.org/dc/terms/";

    /* Classes */
    public static final IRI Agent = createIRI(getNamespace() + "Agent");
    public static final IRI AgentClass = createIRI(getNamespace() + "AgentClass");
    public static final IRI BibliographicResource = createIRI(getNamespace() + "BibliographicResource");
    public static final IRI FileFormat = createIRI(getNamespace() + "FileFormat");
    public static final IRI Frequency = createIRI(getNamespace() + "Frequency");
    public static final IRI Jurisdiction = createIRI(getNamespace() + "Jurisdiction");
    public static final IRI LicenseDocument = createIRI(getNamespace() + "LicenseDocument");
    public static final IRI LinguisticSystem = createIRI(getNamespace() + "LinguisticSystem");
    public static final IRI Location = createIRI(getNamespace() + "Location");
    public static final IRI LocationPeriodOrJurisdiction = createIRI(getNamespace() + "LocationPeriodOrJurisdiction");
    public static final IRI MediaType = createIRI(getNamespace() + "MediaType");
    public static final IRI MediaTypeOrExtent = createIRI(getNamespace() + "MediaTypeOrExtent");
    public static final IRI MethodOfAccrual = createIRI(getNamespace() + "MethodOfAccrual");
    public static final IRI MethodOfInstruction = createIRI(getNamespace() + "MethodOfInstruction");
    public static final IRI PeriodOfTime = createIRI(getNamespace() + "PeriodOfTime");
    public static final IRI PhysicalMedium = createIRI(getNamespace() + "PhysicalMedium");
    public static final IRI PhysicalResource = createIRI(getNamespace() + "PhysicalResource");
    public static final IRI Policy = createIRI(getNamespace() + "Policy");
    public static final IRI ProvenanceStatement = createIRI(getNamespace() + "ProvenanceStatement");
    public static final IRI RightsStatement = createIRI(getNamespace() + "RightsStatement");
    public static final IRI SizeOrDuration = createIRI(getNamespace() + "SizeOrDuration");
    public static final IRI Standard = createIRI(getNamespace() + "Standard");

    /* Properties */
    public static final IRI abstract_ = createIRI(getNamespace() + "abstract");
    public static final IRI accessRights = createIRI(getNamespace() + "accessRights");
    public static final IRI accrualMethod = createIRI(getNamespace() + "accrualMethod");
    public static final IRI accrualPeriodicity = createIRI(getNamespace() + "accrualPeriodicity");
    public static final IRI accrualPolicy = createIRI(getNamespace() + "accrualPolicy");
    public static final IRI alternative = createIRI(getNamespace() + "alternative");
    public static final IRI audience = createIRI(getNamespace() + "audience");
    public static final IRI available = createIRI(getNamespace() + "available");
    public static final IRI bibliographicCitation = createIRI(getNamespace() + "bibliographicCitation");
    public static final IRI conformsTo = createIRI(getNamespace() + "conformsTo");
    public static final IRI contributor = createIRI(getNamespace() + "contributor");
    public static final IRI coverage = createIRI(getNamespace() + "coverage");
    public static final IRI created = createIRI(getNamespace() + "created");
    public static final IRI creator = createIRI(getNamespace() + "creator");
    public static final IRI date = createIRI(getNamespace() + "date");
    public static final IRI dateAccepted = createIRI(getNamespace() + "dateAccepted");
    public static final IRI dateCopyrighted = createIRI(getNamespace() + "dateCopyrighted");
    public static final IRI dateSubmitted = createIRI(getNamespace() + "dateSubmitted");
    public static final IRI description = createIRI(getNamespace() + "description");
    public static final IRI educationLevel = createIRI(getNamespace() + "educationLevel");
    public static final IRI extent = createIRI(getNamespace() + "extent");
    public static final IRI format = createIRI(getNamespace() + "format");
    public static final IRI hasFormat = createIRI(getNamespace() + "hasFormat");
    public static final IRI hasPart = createIRI(getNamespace() + "hasPart");
    public static final IRI hasVersion = createIRI(getNamespace() + "hasVersion");
    public static final IRI identifier = createIRI(getNamespace() + "identifier");
    public static final IRI instructionalMethod = createIRI(getNamespace() + "instructionalMethod");
    public static final IRI isFormatOf = createIRI(getNamespace() + "isFormatOf");
    public static final IRI isPartOf = createIRI(getNamespace() + "isPartOf");
    public static final IRI isReferencedBy = createIRI(getNamespace() + "isReferencedBy");
    public static final IRI isReplacedBy = createIRI(getNamespace() + "isReplacedBy");
    public static final IRI isRequiredBy = createIRI(getNamespace() + "isRequiredBy");
    public static final IRI issued = createIRI(getNamespace() + "issued");
    public static final IRI isVersionOf = createIRI(getNamespace() + "isVersionOf");
    public static final IRI language = createIRI(getNamespace() + "language");
    public static final IRI license = createIRI(getNamespace() + "license");
    public static final IRI mediator = createIRI(getNamespace() + "mediator");
    public static final IRI medium = createIRI(getNamespace() + "medium");
    public static final IRI modified = createIRI(getNamespace() + "modified");
    public static final IRI provenance = createIRI(getNamespace() + "provenance");
    public static final IRI publisher = createIRI(getNamespace() + "publisher");
    public static final IRI references = createIRI(getNamespace() + "references");
    public static final IRI relation = createIRI(getNamespace() + "relation");
    public static final IRI replaces = createIRI(getNamespace() + "replaces");
    public static final IRI requires = createIRI(getNamespace() + "requires");
    public static final IRI rights = createIRI(getNamespace() + "rights");
    public static final IRI rightsHolder = createIRI(getNamespace() + "rightsHolder");
    public static final IRI source = createIRI(getNamespace() + "source");
    public static final IRI spatial = createIRI(getNamespace() + "spatial");
    public static final IRI subject = createIRI(getNamespace() + "subject");
    public static final IRI tableOfContents = createIRI(getNamespace() + "tableOfContents");
    public static final IRI temporal = createIRI(getNamespace() + "temporal");
    public static final IRI title = createIRI(getNamespace() + "title");
    public static final IRI type = createIRI(getNamespace() + "type");
    public static final IRI valid = createIRI(getNamespace() + "valid");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private DC() {
        // prevent instantiation
    }
}

