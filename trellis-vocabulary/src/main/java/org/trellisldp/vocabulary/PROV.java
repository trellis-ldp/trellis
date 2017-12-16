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
 * RDF Terms from the W3C PROV Ontology
 *
 * @see <a href="https://www.w3.org/TR/prov-o/">PROV Ontology</a>
 *
 * @author acoburn
 */
public final class PROV extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.w3.org/ns/prov#";

    /* Classes */
    public static final IRI Activity = createIRI(URI + "Activity");
    public static final IRI Agent = createIRI(URI + "Agent");
    public static final IRI Entity = createIRI(URI + "Entity");

    /* Expanded Classes */
    public static final IRI Bundle = createIRI(URI + "Bundle");
    public static final IRI Collection = createIRI(URI + "Collection");
    public static final IRI EmptyCollection = createIRI(URI + "EmptyCollection");
    public static final IRI Location = createIRI(URI + "Location");
    public static final IRI Organization = createIRI(URI + "Organization");
    public static final IRI Person = createIRI(URI + "Person");
    public static final IRI SoftwareAgent = createIRI(URI + "SoftwareAgent");

    /* Qualified Classes */
    public static final IRI ActivityInfluence = createIRI(URI + "ActivityInfluence");
    public static final IRI AgentInfluence = createIRI(URI + "AgentInfluence");
    public static final IRI Association = createIRI(URI + "Association");
    public static final IRI Attribution = createIRI(URI + "Attribution");
    public static final IRI Communication = createIRI(URI + "Communication");
    public static final IRI Delegation = createIRI(URI + "Delegation");
    public static final IRI Derivation = createIRI(URI + "Derivation");
    public static final IRI End = createIRI(URI + "End");
    public static final IRI EntityInfluence = createIRI(URI + "EntityInfluence");
    public static final IRI Generation = createIRI(URI + "Generation");
    public static final IRI Influence = createIRI(URI + "Influence");
    public static final IRI InstantaneousEvent = createIRI(URI + "InstantaneousEvent");
    public static final IRI Invalidation = createIRI(URI + "Invalidation");
    public static final IRI Plan = createIRI(URI + "Plan");
    public static final IRI PrimarySource = createIRI(URI + "PrimarySource");
    public static final IRI Quotation = createIRI(URI + "Quotation");
    public static final IRI Revision = createIRI(URI + "Revision");
    public static final IRI Start = createIRI(URI + "Start");
    public static final IRI Usage = createIRI(URI + "Usage");

    /* Properties */
    public static final IRI actedOnBehalfOf = createIRI(URI + "actedOnBehalfOf");
    public static final IRI endedAtTime = createIRI(URI + "endedAtTime");
    public static final IRI startedAtTime = createIRI(URI + "startedAtTime");
    public static final IRI used = createIRI(URI + "used");
    public static final IRI wasAssociatedWith = createIRI(URI + "wasAssociatedWith");
    public static final IRI wasAttributedTo = createIRI(URI + "wasAttributedTo");
    public static final IRI wasDerivedFrom = createIRI(URI + "wasDerivedFrom");
    public static final IRI wasGeneratedBy = createIRI(URI + "wasGeneratedBy");
    public static final IRI wasInformedBy = createIRI(URI + "wasInformedBy");

    /* Expanded Properties */
    public static final IRI alternateOf = createIRI(URI + "alternateOf");
    public static final IRI atLocation = createIRI(URI + "atLocation");
    public static final IRI generated = createIRI(URI + "generated");
    public static final IRI generatedAtTime = createIRI(URI + "generatedAtTime");
    public static final IRI hadMember = createIRI(URI + "hadMember");
    public static final IRI hadPrimarySource = createIRI(URI + "hadPrimarySource");
    public static final IRI influenced = createIRI(URI + "influenced");
    public static final IRI invalidated = createIRI(URI + "invalidated");
    public static final IRI invalidatedAtTime = createIRI(URI + "invalidatedAtTime");
    public static final IRI specializationOf = createIRI(URI + "specializationOf");
    public static final IRI value = createIRI(URI + "value");
    public static final IRI wasEndedBy = createIRI(URI + "wasEndedBy");
    public static final IRI wasInvalidatedBy = createIRI(URI + "wasInvalidatedBy");
    public static final IRI wasQuotedFrom = createIRI(URI + "wasQuotedFrom");
    public static final IRI wasRevisionOf = createIRI(URI + "wasRevisionOf");
    public static final IRI wasStartedBy = createIRI(URI + "wasStartedBy");

    /* Qualified Properties */
    public static final IRI activity = createIRI(URI + "activity");
    public static final IRI agent = createIRI(URI + "agent");
    public static final IRI atTime = createIRI(URI + "atTime");
    public static final IRI entity = createIRI(URI + "entity");
    public static final IRI hadActivity = createIRI(URI + "hadActivity");
    public static final IRI hadGeneration = createIRI(URI + "hadGeneration");
    public static final IRI hadPlan = createIRI(URI + "hadPlan");
    public static final IRI hadRole = createIRI(URI + "hadRole");
    public static final IRI hadUsage = createIRI(URI + "hadUsage");
    public static final IRI influencer = createIRI(URI + "influencer");
    public static final IRI qualifiedAssociation = createIRI(URI + "qualifiedAssociation");
    public static final IRI qualifiedAttribution = createIRI(URI + "qualifiedAttribution");
    public static final IRI qualifiedCommunication = createIRI(URI + "qualifiedCommunication");
    public static final IRI qualifiedDelegation = createIRI(URI + "qualifiedDelegation");
    public static final IRI qualifiedDerivation = createIRI(URI + "qualifiedDerivation");
    public static final IRI qualifiedEnd = createIRI(URI + "qualifiedEnd");
    public static final IRI qualifiedGeneration = createIRI(URI + "qualifiedGeneration");
    public static final IRI qualifiedInfluence = createIRI(URI + "qualifiedInfluence");
    public static final IRI qualifiedInvalidation = createIRI(URI + "qualifiedInvalidation");
    public static final IRI qualifiedPrimarySource = createIRI(URI + "qualifiedPrimarySource");
    public static final IRI qualifiedQuotation = createIRI(URI + "qualifiedQuotation");
    public static final IRI qualifiedRevision = createIRI(URI + "qualifiedRevision");
    public static final IRI qualifiedStart = createIRI(URI + "qualifiedStart");
    public static final IRI qualifiedUsage = createIRI(URI + "qualifiedUsage");
    public static final IRI wasInfluencedBy = createIRI(URI + "wasInfluencedBy");

    private PROV() {
        super();
    }
}
