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
 * RDF Terms from the W3C PROV Ontology.
 *
 * @see <a href="https://www.w3.org/TR/prov-o/">PROV Ontology</a>
 *
 * @author acoburn
 */
public final class PROV {

    /* Namespace */
    private static final String URI = "http://www.w3.org/ns/prov#";

    /* Classes */
    public static final IRI Activity = createIRI(getNamespace() + "Activity");
    public static final IRI Agent = createIRI(getNamespace() + "Agent");
    public static final IRI Entity = createIRI(getNamespace() + "Entity");

    /* Expanded Classes */
    public static final IRI Bundle = createIRI(getNamespace() + "Bundle");
    public static final IRI Collection = createIRI(getNamespace() + "Collection");
    public static final IRI EmptyCollection = createIRI(getNamespace() + "EmptyCollection");
    public static final IRI Location = createIRI(getNamespace() + "Location");
    public static final IRI Organization = createIRI(getNamespace() + "Organization");
    public static final IRI Person = createIRI(getNamespace() + "Person");
    public static final IRI SoftwareAgent = createIRI(getNamespace() + "SoftwareAgent");

    /* Qualified Classes */
    public static final IRI ActivityInfluence = createIRI(getNamespace() + "ActivityInfluence");
    public static final IRI AgentInfluence = createIRI(getNamespace() + "AgentInfluence");
    public static final IRI Association = createIRI(getNamespace() + "Association");
    public static final IRI Attribution = createIRI(getNamespace() + "Attribution");
    public static final IRI Communication = createIRI(getNamespace() + "Communication");
    public static final IRI Delegation = createIRI(getNamespace() + "Delegation");
    public static final IRI Derivation = createIRI(getNamespace() + "Derivation");
    public static final IRI End = createIRI(getNamespace() + "End");
    public static final IRI EntityInfluence = createIRI(getNamespace() + "EntityInfluence");
    public static final IRI Generation = createIRI(getNamespace() + "Generation");
    public static final IRI Influence = createIRI(getNamespace() + "Influence");
    public static final IRI InstantaneousEvent = createIRI(getNamespace() + "InstantaneousEvent");
    public static final IRI Invalidation = createIRI(getNamespace() + "Invalidation");
    public static final IRI Plan = createIRI(getNamespace() + "Plan");
    public static final IRI PrimarySource = createIRI(getNamespace() + "PrimarySource");
    public static final IRI Quotation = createIRI(getNamespace() + "Quotation");
    public static final IRI Revision = createIRI(getNamespace() + "Revision");
    public static final IRI Start = createIRI(getNamespace() + "Start");
    public static final IRI Usage = createIRI(getNamespace() + "Usage");

    /* Properties */
    public static final IRI actedOnBehalfOf = createIRI(getNamespace() + "actedOnBehalfOf");
    public static final IRI endedAtTime = createIRI(getNamespace() + "endedAtTime");
    public static final IRI startedAtTime = createIRI(getNamespace() + "startedAtTime");
    public static final IRI used = createIRI(getNamespace() + "used");
    public static final IRI wasAssociatedWith = createIRI(getNamespace() + "wasAssociatedWith");
    public static final IRI wasAttributedTo = createIRI(getNamespace() + "wasAttributedTo");
    public static final IRI wasDerivedFrom = createIRI(getNamespace() + "wasDerivedFrom");
    public static final IRI wasGeneratedBy = createIRI(getNamespace() + "wasGeneratedBy");
    public static final IRI wasInformedBy = createIRI(getNamespace() + "wasInformedBy");

    /* Expanded Properties */
    public static final IRI alternateOf = createIRI(getNamespace() + "alternateOf");
    public static final IRI atLocation = createIRI(getNamespace() + "atLocation");
    public static final IRI generated = createIRI(getNamespace() + "generated");
    public static final IRI generatedAtTime = createIRI(getNamespace() + "generatedAtTime");
    public static final IRI hadMember = createIRI(getNamespace() + "hadMember");
    public static final IRI hadPrimarySource = createIRI(getNamespace() + "hadPrimarySource");
    public static final IRI influenced = createIRI(getNamespace() + "influenced");
    public static final IRI invalidated = createIRI(getNamespace() + "invalidated");
    public static final IRI invalidatedAtTime = createIRI(getNamespace() + "invalidatedAtTime");
    public static final IRI specializationOf = createIRI(getNamespace() + "specializationOf");
    public static final IRI value = createIRI(getNamespace() + "value");
    public static final IRI wasEndedBy = createIRI(getNamespace() + "wasEndedBy");
    public static final IRI wasInvalidatedBy = createIRI(getNamespace() + "wasInvalidatedBy");
    public static final IRI wasQuotedFrom = createIRI(getNamespace() + "wasQuotedFrom");
    public static final IRI wasRevisionOf = createIRI(getNamespace() + "wasRevisionOf");
    public static final IRI wasStartedBy = createIRI(getNamespace() + "wasStartedBy");

    /* Qualified Properties */
    public static final IRI activity = createIRI(getNamespace() + "activity");
    public static final IRI agent = createIRI(getNamespace() + "agent");
    public static final IRI atTime = createIRI(getNamespace() + "atTime");
    public static final IRI entity = createIRI(getNamespace() + "entity");
    public static final IRI hadActivity = createIRI(getNamespace() + "hadActivity");
    public static final IRI hadGeneration = createIRI(getNamespace() + "hadGeneration");
    public static final IRI hadPlan = createIRI(getNamespace() + "hadPlan");
    public static final IRI hadRole = createIRI(getNamespace() + "hadRole");
    public static final IRI hadUsage = createIRI(getNamespace() + "hadUsage");
    public static final IRI influencer = createIRI(getNamespace() + "influencer");
    public static final IRI qualifiedAssociation = createIRI(getNamespace() + "qualifiedAssociation");
    public static final IRI qualifiedAttribution = createIRI(getNamespace() + "qualifiedAttribution");
    public static final IRI qualifiedCommunication = createIRI(getNamespace() + "qualifiedCommunication");
    public static final IRI qualifiedDelegation = createIRI(getNamespace() + "qualifiedDelegation");
    public static final IRI qualifiedDerivation = createIRI(getNamespace() + "qualifiedDerivation");
    public static final IRI qualifiedEnd = createIRI(getNamespace() + "qualifiedEnd");
    public static final IRI qualifiedGeneration = createIRI(getNamespace() + "qualifiedGeneration");
    public static final IRI qualifiedInfluence = createIRI(getNamespace() + "qualifiedInfluence");
    public static final IRI qualifiedInvalidation = createIRI(getNamespace() + "qualifiedInvalidation");
    public static final IRI qualifiedPrimarySource = createIRI(getNamespace() + "qualifiedPrimarySource");
    public static final IRI qualifiedQuotation = createIRI(getNamespace() + "qualifiedQuotation");
    public static final IRI qualifiedRevision = createIRI(getNamespace() + "qualifiedRevision");
    public static final IRI qualifiedStart = createIRI(getNamespace() + "qualifiedStart");
    public static final IRI qualifiedUsage = createIRI(getNamespace() + "qualifiedUsage");
    public static final IRI wasInfluencedBy = createIRI(getNamespace() + "wasInfluencedBy");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private PROV() {
        // prevent instantiation
    }
}
