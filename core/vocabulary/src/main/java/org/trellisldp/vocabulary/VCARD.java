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
 * RDF Terms from the W3C vCARD Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/vcard-rdf">vCARD</a>
 *
 * @author acoburn
 */
public final class VCARD {

    /* Namespace */
    private static final String URI = "http://www.w3.org/2006/vcard/ns#";

    /* Classes */
    public static final IRI Acquaintance = createIRI(getNamespace() + "Acquaintance");
    public static final IRI Address = createIRI(getNamespace() + "Address");
    public static final IRI Agent = createIRI(getNamespace() + "Agent");
    public static final IRI Cell = createIRI(getNamespace() + "Cell");
    public static final IRI Child = createIRI(getNamespace() + "Child");
    public static final IRI Colleague = createIRI(getNamespace() + "Colleague");
    public static final IRI Contact = createIRI(getNamespace() + "Contact");
    public static final IRI Coresident = createIRI(getNamespace() + "Coresident");
    public static final IRI Coworker = createIRI(getNamespace() + "Coworker");
    public static final IRI Crush = createIRI(getNamespace() + "Crush");
    public static final IRI Date = createIRI(getNamespace() + "Date");
    public static final IRI Emergency = createIRI(getNamespace() + "Emergency");
    public static final IRI Fax = createIRI(getNamespace() + "Fax");
    public static final IRI Female = createIRI(getNamespace() + "Female");
    public static final IRI Friend = createIRI(getNamespace() + "Friend");
    public static final IRI Gender = createIRI(getNamespace() + "Gender");
    public static final IRI Group = createIRI(getNamespace() + "Group");
    public static final IRI Home = createIRI(getNamespace() + "Home");
    public static final IRI Individual = createIRI(getNamespace() + "Individual");
    public static final IRI Kin = createIRI(getNamespace() + "Kin");
    public static final IRI Kind = createIRI(getNamespace() + "Kind");
    public static final IRI Location = createIRI(getNamespace() + "Location");
    public static final IRI Male = createIRI(getNamespace() + "Male");
    public static final IRI Me = createIRI(getNamespace() + "Me");
    public static final IRI Met = createIRI(getNamespace() + "Met");
    public static final IRI Muse = createIRI(getNamespace() + "Muse");
    public static final IRI Name = createIRI(getNamespace() + "Name");
    public static final IRI Neighbor = createIRI(getNamespace() + "Neighbor");
    public static final IRI None = createIRI(getNamespace() + "None");
    public static final IRI Organization = createIRI(getNamespace() + "Organization");
    public static final IRI Other = createIRI(getNamespace() + "Other");
    public static final IRI Pager = createIRI(getNamespace() + "Pager");
    public static final IRI Parent = createIRI(getNamespace() + "Parent");
    public static final IRI RelatedType = createIRI(getNamespace() + "RelatedType");
    public static final IRI Sibling = createIRI(getNamespace() + "Sibling");
    public static final IRI Spouse = createIRI(getNamespace() + "Spouse");
    public static final IRI Sweetheart = createIRI(getNamespace() + "Sweetheart");
    public static final IRI TelephoneType = createIRI(getNamespace() + "TelephoneType");
    public static final IRI Text = createIRI(getNamespace() + "Text");
    public static final IRI TextPhone = createIRI(getNamespace() + "TextPhone");
    public static final IRI Type = createIRI(getNamespace() + "Type");
    public static final IRI Unknown = createIRI(getNamespace() + "Unknown");
    public static final IRI VCard = createIRI(getNamespace() + "VCard");
    public static final IRI Video = createIRI(getNamespace() + "Video");
    public static final IRI Voice = createIRI(getNamespace() + "Voice");
    public static final IRI Work = createIRI(getNamespace() + "Work");

    /* Object Properties */
    public static final IRI hasAdditionalName = createIRI(getNamespace() + "hasAdditionalName");
    public static final IRI hasAddress = createIRI(getNamespace() + "hasAddress");
    public static final IRI hasCalendarBusy = createIRI(getNamespace() + "hasCalendarBusy");
    public static final IRI hasCalendarLink = createIRI(getNamespace() + "hasCalendarLink");
    public static final IRI hasCalendarRequest = createIRI(getNamespace() + "hasCalendarRequest");
    public static final IRI hasCategory = createIRI(getNamespace() + "hasCategory");
    public static final IRI hasCountryName = createIRI(getNamespace() + "hasCountryName");
    public static final IRI hasEmail = createIRI(getNamespace() + "hasEmail");
    public static final IRI hasFamilyName = createIRI(getNamespace() + "hasFamilyName");
    public static final IRI hasFN = createIRI(getNamespace() + "hasFN");
    public static final IRI hasGender = createIRI(getNamespace() + "hasGender");
    public static final IRI hasGeo = createIRI(getNamespace() + "hasGeo");
    public static final IRI hasGivenName = createIRI(getNamespace() + "hasGivenName");
    public static final IRI hasHonorificPrefix = createIRI(getNamespace() + "hasHonorificPrefix");
    public static final IRI hasHonorificSuffix = createIRI(getNamespace() + "hasHonorificSuffix");
    public static final IRI hasInstantMessage = createIRI(getNamespace() + "hasInstantMessage");
    public static final IRI hasKey = createIRI(getNamespace() + "hasKey");
    public static final IRI hasLanguage = createIRI(getNamespace() + "hasLanguage");
    public static final IRI hasLocality = createIRI(getNamespace() + "hasLocality");
    public static final IRI hasLogo = createIRI(getNamespace() + "hasLogo");
    public static final IRI hasMember = createIRI(getNamespace() + "hasMember");
    public static final IRI hasName = createIRI(getNamespace() + "hasName");
    public static final IRI hasNickname = createIRI(getNamespace() + "hasNickname");
    public static final IRI hasNote = createIRI(getNamespace() + "hasNote");
    public static final IRI hasOrganizationName = createIRI(getNamespace() + "hasOrganizationName");
    public static final IRI hasOrganizationUnit = createIRI(getNamespace() + "hasOrganizationUnit");
    public static final IRI hasPhoto = createIRI(getNamespace() + "hasPhoto");
    public static final IRI hasPostalCode = createIRI(getNamespace() + "hasPostalCode");
    public static final IRI hasRegion = createIRI(getNamespace() + "hasRegion");
    public static final IRI hasRelated = createIRI(getNamespace() + "hasRelated");
    public static final IRI hasRole = createIRI(getNamespace() + "hasRole");
    public static final IRI hasSound = createIRI(getNamespace() + "hasSound");
    public static final IRI hasSource = createIRI(getNamespace() + "hasSource");
    public static final IRI hasStreetAddress = createIRI(getNamespace() + "hasStreetAddress");
    public static final IRI hasTelephone = createIRI(getNamespace() + "hasTelephone");
    public static final IRI hasTitle = createIRI(getNamespace() + "hasTitle");
    public static final IRI hasUID = createIRI(getNamespace() + "hasUID");
    public static final IRI hasURL = createIRI(getNamespace() + "hasURL");
    public static final IRI hasValue = createIRI(getNamespace() + "hasValue");

    /* Datatype Properties */
    public static final IRI additional_name = createIRI(getNamespace() + "additional-name");
    public static final IRI anniversary = createIRI(getNamespace() + "anniversary");
    public static final IRI bday = createIRI(getNamespace() + "bday");
    public static final IRI category = createIRI(getNamespace() + "category");
    public static final IRI country_name = createIRI(getNamespace() + "country-name");
    public static final IRI family_name = createIRI(getNamespace() + "family-name");
    public static final IRI fn = createIRI(getNamespace() + "fn");
    public static final IRI given_name = createIRI(getNamespace() + "given-name");
    public static final IRI honorific_prefix = createIRI(getNamespace() + "honorific-prefix");
    public static final IRI honorific_suffix = createIRI(getNamespace() + "honorific-suffix");
    public static final IRI language = createIRI(getNamespace() + "language");
    public static final IRI locality = createIRI(getNamespace() + "locality");
    public static final IRI nickname = createIRI(getNamespace() + "nickname");
    public static final IRI note = createIRI(getNamespace() + "note");
    public static final IRI organization_name = createIRI(getNamespace() + "organization-name");
    public static final IRI organization_unit = createIRI(getNamespace() + "organization-unit");
    public static final IRI postal_code = createIRI(getNamespace() + "postal-code");
    public static final IRI prodid = createIRI(getNamespace() + "prodid");
    public static final IRI region = createIRI(getNamespace() + "region");
    public static final IRI rev = createIRI(getNamespace() + "rev");
    public static final IRI role = createIRI(getNamespace() + "role");
    public static final IRI sort_string = createIRI(getNamespace() + "sort-string");
    public static final IRI street_address = createIRI(getNamespace() + "street-address");
    public static final IRI title = createIRI(getNamespace() + "title");
    public static final IRI tz = createIRI(getNamespace() + "tz");
    public static final IRI value = createIRI(getNamespace() + "value");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private VCARD() {
        // prevent instantiation
    }
}
