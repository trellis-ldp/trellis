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

import static java.util.Collections.unmodifiableMap;
import static org.trellisldp.vocabulary.VocabUtils.createIRI;

import java.util.HashMap;
import java.util.Map;

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
    private static final String URI = "http://www.w3.org/ns/ldp#";

    /* Superclass mapping */
    private static final Map<IRI, IRI> superclassOf;

    /* Classes */
    public static final IRI BasicContainer = createIRI(getNamespace() + "BasicContainer");
    public static final IRI Container = createIRI(getNamespace() + "Container");
    public static final IRI DirectContainer = createIRI(getNamespace() + "DirectContainer");
    public static final IRI IndirectContainer = createIRI(getNamespace() + "IndirectContainer");
    public static final IRI NonRDFSource = createIRI(getNamespace() + "NonRDFSource");
    public static final IRI Resource = createIRI(getNamespace() + "Resource");
    public static final IRI RDFSource = createIRI(getNamespace() + "RDFSource");

    /* Properties */
    public static final IRI contains = createIRI(getNamespace() + "contains");
    public static final IRI hasMemberRelation = createIRI(getNamespace() + "hasMemberRelation");
    public static final IRI inbox = createIRI(getNamespace() + "inbox");
    public static final IRI insertedContentRelation = createIRI(getNamespace() + "insertedContentRelation");
    public static final IRI isMemberOfRelation = createIRI(getNamespace() + "isMemberOfRelation");
    public static final IRI member = createIRI(getNamespace() + "member");
    public static final IRI membershipResource = createIRI(getNamespace() + "membershipResource");

    /* Prefer-related Classes */
    public static final IRI PreferContainment = createIRI(getNamespace() + "PreferContainment");
    public static final IRI PreferMembership = createIRI(getNamespace() + "PreferMembership");
    public static final IRI PreferMinimalContainer = createIRI(getNamespace() + "PreferMinimalContainer");

    /* Paging Classes */
    public static final IRI PageSortCriterion = createIRI(getNamespace() + "PageSortCriterion");
    public static final IRI Ascending = createIRI(getNamespace() + "Ascending");
    public static final IRI Descending = createIRI(getNamespace() + "Descending");
    public static final IRI Page = createIRI(getNamespace() + "Page");

    /* Paging Properties */
    public static final IRI constrainedBy = createIRI(getNamespace() + "constrainedBy");
    public static final IRI pageSortCriteria = createIRI(getNamespace() + "pageSortCriteria");
    public static final IRI pageSortPredicate = createIRI(getNamespace() + "pageSortPredicate");
    public static final IRI pageSortOrder = createIRI(getNamespace() + "pageSortOrder");
    public static final IRI pageSortCollation = createIRI(getNamespace() + "pageSortCollation");
    public static final IRI pageSequence = createIRI(getNamespace() + "pageSequence");

    /* Other Classes */
    public static final IRI MemberSubject = createIRI(getNamespace() + "MemberSubject");

    static {
        final Map<IRI, IRI> data = new HashMap<>();
        data.put(NonRDFSource, Resource);
        data.put(RDFSource, Resource);
        data.put(Container, RDFSource);
        data.put(BasicContainer, Container);
        data.put(DirectContainer, Container);
        data.put(IndirectContainer, Container);
        superclassOf = unmodifiableMap(data);
    }

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    /**
     * Get the superclass of this LDP type.
     * @param type the type
     * @return the superclass or null if none exists.
     */
    public static IRI getSuperclassOf(final IRI type) {
        return superclassOf.get(type);
    }

    private LDP() {
        // prevent instantiation
    }
}
