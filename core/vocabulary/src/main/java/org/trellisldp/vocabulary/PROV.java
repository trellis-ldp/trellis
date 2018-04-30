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
 * RDF Terms from the W3C PROV Ontology.
 *
 * @see <a href="https://www.w3.org/TR/prov-o/">PROV Ontology</a>
 *
 * @author acoburn
 */
public final class PROV {

    /* Namespace */
    public static final String NS = "http://www.w3.org/ns/prov#";

    /* Classes */
    public static final IRI Activity = createIRI(NS + "Activity");
    public static final IRI Agent = createIRI(NS + "Agent");
    public static final IRI Entity = createIRI(NS + "Entity");

    /* Expanded Classes */
    public static final IRI Bundle = createIRI(NS + "Bundle");
    public static final IRI Collection = createIRI(NS + "Collection");
    public static final IRI EmptyCollection = createIRI(NS + "EmptyCollection");
    public static final IRI KeyEntityPair = createIRI(NS + "KeyEntityPair");
    public static final IRI Location = createIRI(NS + "Location");
    public static final IRI Organization = createIRI(NS + "Organization");
    public static final IRI Person = createIRI(NS + "Person");
    public static final IRI SoftwareAgent = createIRI(NS + "SoftwareAgent");

    /* DC Classes */
    public static final IRI Accept = createIRI(NS + "Accept");
    public static final IRI Contribute = createIRI(NS + "Contribute");
    public static final IRI Contributor = createIRI(NS + "Contributor");
    public static final IRI Copyright = createIRI(NS + "Copyright");
    public static final IRI Create = createIRI(NS + "Create");
    public static final IRI Creator = createIRI(NS + "Creator");
    public static final IRI Modify = createIRI(NS + "Modify");
    public static final IRI Publish = createIRI(NS + "Publish");
    public static final IRI Publisher = createIRI(NS + "Publisher");
    public static final IRI Replace = createIRI(NS + "Replace");
    public static final IRI RightsAssignment = createIRI(NS + "RightsAssignment");
    public static final IRI RightsHolder = createIRI(NS + "RightsHolder");
    public static final IRI Submit = createIRI(NS + "Submit");

    /* Qualified Classes */
    public static final IRI ActivityInfluence = createIRI(NS + "ActivityInfluence");
    public static final IRI AgentInfluence = createIRI(NS + "AgentInfluence");
    public static final IRI Association = createIRI(NS + "Association");
    public static final IRI Attribution = createIRI(NS + "Attribution");
    public static final IRI Communication = createIRI(NS + "Communication");
    public static final IRI Delegation = createIRI(NS + "Delegation");
    public static final IRI Derivation = createIRI(NS + "Derivation");
    public static final IRI End = createIRI(NS + "End");
    public static final IRI EntityInfluence = createIRI(NS + "EntityInfluence");
    public static final IRI Generation = createIRI(NS + "Generation");
    public static final IRI Influence = createIRI(NS + "Influence");
    public static final IRI InstantaneousEvent = createIRI(NS + "InstantaneousEvent");
    public static final IRI Invalidation = createIRI(NS + "Invalidation");
    public static final IRI Plan = createIRI(NS + "Plan");
    public static final IRI PrimarySource = createIRI(NS + "PrimarySource");
    public static final IRI Quotation = createIRI(NS + "Quotation");
    public static final IRI Revision = createIRI(NS + "Revision");
    public static final IRI Start = createIRI(NS + "Start");
    public static final IRI Usage = createIRI(NS + "Usage");

    /* Properties */
    public static final IRI aq = createIRI(NS + "aq");
    public static final IRI actedOnBehalfOf = createIRI(NS + "actedOnBehalfOf");
    public static final IRI definition = createIRI(NS + "definition");
    public static final IRI dm = createIRI(NS + "dm");
    public static final IRI editorsDefinition = createIRI(NS + "editorsDefinition");
    public static final IRI endedAtTime = createIRI(NS + "endedAtTime");
    public static final IRI entityOfInfluence = createIRI(NS + "entityOfInfluence");
    public static final IRI inverse = createIRI(NS + "inverse");
    public static final IRI insertedKeyEntityPair = createIRI(NS + "insertedKeyEntityPair");
    public static final IRI order = createIRI(NS + "order");
    public static final IRI pairEntity = createIRI(NS + "pairEntity");
    public static final IRI revisedEntity = createIRI(NS + "revisedEntity");
    public static final IRI startedAtTime = createIRI(NS + "startedAtTime");
    public static final IRI used = createIRI(NS + "used");
    public static final IRI wasAssociatedWith = createIRI(NS + "wasAssociatedWith");
    public static final IRI wasAttributedTo = createIRI(NS + "wasAttributedTo");
    public static final IRI wasDerivedFrom = createIRI(NS + "wasDerivedFrom");
    public static final IRI wasGeneratedBy = createIRI(NS + "wasGeneratedBy");
    public static final IRI wasInformedBy = createIRI(NS + "wasInformedBy");

    /* Expanded Properties */
    public static final IRI alternateOf = createIRI(NS + "alternateOf");
    public static final IRI atLocation = createIRI(NS + "atLocation");
    public static final IRI generated = createIRI(NS + "generated");
    public static final IRI generatedAsDerivation = createIRI(NS + "generatedAsDerivation");
    public static final IRI generatedAtTime = createIRI(NS + "generatedAtTime");
    public static final IRI hadDelegate = createIRI(NS + "hadDelegate");
    public static final IRI hadMember = createIRI(NS + "hadMember");
    public static final IRI hadPrimarySource = createIRI(NS + "hadPrimarySource");
    public static final IRI influenced = createIRI(NS + "influenced");
    public static final IRI invalidated = createIRI(NS + "invalidated");
    public static final IRI invalidatedAtTime = createIRI(NS + "invalidatedAtTime");
    public static final IRI qualifiedAssociationOf = createIRI(NS + "qualifiedAssociationOf");
    public static final IRI qualifiedCommunicationOf = createIRI(NS + "qualifiedCommunicationOf");
    public static final IRI qualifiedEndOf = createIRI(NS + "qualifiedEndOf");
    public static final IRI qualifiedGenerationOf = createIRI(NS + "qualifiedGenerationOf");
    public static final IRI qualifiedQuotationOf = createIRI(NS + "qualifiedQuotationOf");
    public static final IRI specializationOf = createIRI(NS + "specializationOf");
    public static final IRI value = createIRI(NS + "value");
    public static final IRI wasActivityOfInfluence = createIRI(NS + "wasActivityOfInfluence");
    public static final IRI wasEndedBy = createIRI(NS + "wasEndedBy");
    public static final IRI wasInvalidatedBy = createIRI(NS + "wasInvalidatedBy");
    public static final IRI wasMemberOf = createIRI(NS + "wasMemberOf");
    public static final IRI wasPlanOf = createIRI(NS + "wasPlanOf");
    public static final IRI wasPrimarySourceOf = createIRI(NS + "wasPrimarySourceOf");
    public static final IRI wasQuotedFrom = createIRI(NS + "wasQuotedFrom");
    public static final IRI wasRevisionOf = createIRI(NS + "wasRevisionOf");
    public static final IRI wasStartedBy = createIRI(NS + "wasStartedBy");

    /* Qualified Properties */
    public static final IRI activity = createIRI(NS + "activity");
    public static final IRI agent = createIRI(NS + "agent");
    public static final IRI atTime = createIRI(NS + "atTime");
    public static final IRI entity = createIRI(NS + "entity");
    public static final IRI hadActivity = createIRI(NS + "hadActivity");
    public static final IRI hadGeneration = createIRI(NS + "hadGeneration");
    public static final IRI hadPlan = createIRI(NS + "hadPlan");
    public static final IRI hadRole = createIRI(NS + "hadRole");
    public static final IRI hadUsage = createIRI(NS + "hadUsage");
    public static final IRI influencer = createIRI(NS + "influencer");
    public static final IRI qualifiedAssociation = createIRI(NS + "qualifiedAssociation");
    public static final IRI qualifiedAttribution = createIRI(NS + "qualifiedAttribution");
    public static final IRI qualifiedCommunication = createIRI(NS + "qualifiedCommunication");
    public static final IRI qualifiedDelegation = createIRI(NS + "qualifiedDelegation");
    public static final IRI qualifiedDerivation = createIRI(NS + "qualifiedDerivation");
    public static final IRI qualifiedEnd = createIRI(NS + "qualifiedEnd");
    public static final IRI qualifiedGeneration = createIRI(NS + "qualifiedGeneration");
    public static final IRI qualifiedInfluence = createIRI(NS + "qualifiedInfluence");
    public static final IRI qualifiedInsertion = createIRI(NS + "qualifiedInsertion");
    public static final IRI qualifiedInvalidation = createIRI(NS + "qualifiedInvalidation");
    public static final IRI qualifiedPrimarySource = createIRI(NS + "qualifiedPrimarySource");
    public static final IRI qualifiedQuotation = createIRI(NS + "qualifiedQuotation");
    public static final IRI qualifiedRevision = createIRI(NS + "qualifiedRevision");
    public static final IRI qualifiedStart = createIRI(NS + "qualifiedStart");
    public static final IRI qualifiedUsage = createIRI(NS + "qualifiedUsage");
    public static final IRI wasInfluencedBy = createIRI(NS + "wasInfluencedBy");

    private PROV() {
        // prevent instantiation
    }
}
