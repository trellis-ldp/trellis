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
 * RDF Terms from the LDP Vocabulary
 *
 * @see <a href="https://www.w3.org/ns/ldp">LDP Vocabulary</a>
 *
 * @author acoburn
 */
public final class LDP extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.w3.org/ns/ldp#";

    /* Classes */
    public static final IRI BasicContainer = createIRI(URI + "BasicContainer");
    public static final IRI Container = createIRI(URI + "Container");
    public static final IRI DirectContainer = createIRI(URI + "DirectContainer");
    public static final IRI IndirectContainer = createIRI(URI + "IndirectContainer");
    public static final IRI NonRDFSource = createIRI(URI + "NonRDFSource");
    public static final IRI Resource = createIRI(URI + "Resource");
    public static final IRI RDFSource = createIRI(URI + "RDFSource");

    /* Properties */
    public static final IRI contains = createIRI(URI + "contains");
    public static final IRI hasMemberRelation = createIRI(URI + "hasMemberRelation");
    public static final IRI inbox = createIRI(URI + "inbox");
    public static final IRI insertedContentRelation = createIRI(URI + "insertedContentRelation");
    public static final IRI isMemberOfRelation = createIRI(URI + "isMemberOfRelation");
    public static final IRI member = createIRI(URI + "member");
    public static final IRI membershipResource = createIRI(URI + "membershipResource");

    /* Prefer-related Classes */
    public static final IRI PreferContainment = createIRI(URI + "PreferContainment");
    public static final IRI PreferMembership = createIRI(URI + "PreferMembership");
    public static final IRI PreferMinimalContainer = createIRI(URI + "PreferMinimalContainer");

    /* Paging Classes */
    public static final IRI PageSortCriterion = createIRI(URI + "PageSortCriterion");
    public static final IRI Ascending = createIRI(URI + "Ascending");
    public static final IRI Descending = createIRI(URI + "Descending");
    public static final IRI Page = createIRI(URI + "Page");

    /* Paging Properties */
    public static final IRI constrainedBy = createIRI(URI + "constrainedBy");
    public static final IRI pageSortCriteria = createIRI(URI + "pageSortCriteria");
    public static final IRI pageSortPredicate = createIRI(URI + "pageSortPredicate");
    public static final IRI pageSortOrder = createIRI(URI + "pageSortOrder");
    public static final IRI pageSortCollation = createIRI(URI + "pageSortCollation");
    public static final IRI pageSequence = createIRI(URI + "pageSequence");

    /* Other Classes */
    public static final IRI MemberSubject = createIRI(URI + "MemberSubject");

    private LDP() {
        super();
    }
}
