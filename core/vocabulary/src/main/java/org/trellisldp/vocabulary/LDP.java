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
 * RDF Terms from the LDP Vocabulary.
 *
 * @see <a href="https://www.w3.org/ns/ldp">LDP Vocabulary</a>
 *
 * @author acoburn
 */
public final class LDP {

    /* Namespace */
    public static final String NS = "http://www.w3.org/ns/ldp#";

    /* Classes */
    public static final IRI BasicContainer = createIRI(NS + "BasicContainer");
    public static final IRI Container = createIRI(NS + "Container");
    public static final IRI DirectContainer = createIRI(NS + "DirectContainer");
    public static final IRI IndirectContainer = createIRI(NS + "IndirectContainer");
    public static final IRI NonRDFSource = createIRI(NS + "NonRDFSource");
    public static final IRI Resource = createIRI(NS + "Resource");
    public static final IRI RDFSource = createIRI(NS + "RDFSource");

    /* Properties */
    public static final IRI contains = createIRI(NS + "contains");
    public static final IRI hasMemberRelation = createIRI(NS + "hasMemberRelation");
    public static final IRI inbox = createIRI(NS + "inbox");
    public static final IRI insertedContentRelation = createIRI(NS + "insertedContentRelation");
    public static final IRI isMemberOfRelation = createIRI(NS + "isMemberOfRelation");
    public static final IRI member = createIRI(NS + "member");
    public static final IRI membershipResource = createIRI(NS + "membershipResource");

    /* Prefer-related Classes */
    public static final IRI PreferContainment = createIRI(NS + "PreferContainment");
    public static final IRI PreferEmptyContainer = createIRI(NS + "EmptyContainer");
    public static final IRI PreferMembership = createIRI(NS + "PreferMembership");
    public static final IRI PreferMinimalContainer = createIRI(NS + "PreferMinimalContainer");

    /* Paging Classes */
    public static final IRI PageSortCriterion = createIRI(NS + "PageSortCriterion");
    public static final IRI Ascending = createIRI(NS + "Ascending");
    public static final IRI Descending = createIRI(NS + "Descending");
    public static final IRI Page = createIRI(NS + "Page");

    /* Paging Properties */
    public static final IRI constrainedBy = createIRI(NS + "constrainedBy");
    public static final IRI pageSortCriteria = createIRI(NS + "pageSortCriteria");
    public static final IRI pageSortPredicate = createIRI(NS + "pageSortPredicate");
    public static final IRI pageSortOrder = createIRI(NS + "pageSortOrder");
    public static final IRI pageSortCollation = createIRI(NS + "pageSortCollation");
    public static final IRI pageSequence = createIRI(NS + "pageSequence");

    /* Other Classes */
    public static final IRI MemberSubject = createIRI(NS + "MemberSubject");

    private LDP() {
        // prevent instantiation
    }
}
