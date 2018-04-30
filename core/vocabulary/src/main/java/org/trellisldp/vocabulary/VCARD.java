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
 * RDF Terms from the W3C vCARD Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/vcard-rdf">vCARD</a>
 *
 * @author acoburn
 */
public final class VCARD {

    /* Namespace */
    public static final String NS = "http://www.w3.org/2006/vcard/ns#";

    /* Classes */
    public static final IRI Acquaintance = createIRI(NS + "Acquaintance");
    public static final IRI Address = createIRI(NS + "Address");
    public static final IRI Agent = createIRI(NS + "Agent");
    public static final IRI Cell = createIRI(NS + "Cell");
    public static final IRI Child = createIRI(NS + "Child");
    public static final IRI Colleague = createIRI(NS + "Colleague");
    public static final IRI Contact = createIRI(NS + "Contact");
    public static final IRI Coresident = createIRI(NS + "Coresident");
    public static final IRI Coworker = createIRI(NS + "Coworker");
    public static final IRI Crush = createIRI(NS + "Crush");
    public static final IRI Date = createIRI(NS + "Date");
    public static final IRI Emergency = createIRI(NS + "Emergency");
    public static final IRI Fax = createIRI(NS + "Fax");
    public static final IRI Female = createIRI(NS + "Female");
    public static final IRI Friend = createIRI(NS + "Friend");
    public static final IRI Gender = createIRI(NS + "Gender");
    public static final IRI Group = createIRI(NS + "Group");
    public static final IRI Home = createIRI(NS + "Home");
    public static final IRI Individual = createIRI(NS + "Individual");
    public static final IRI Kin = createIRI(NS + "Kin");
    public static final IRI Kind = createIRI(NS + "Kind");
    public static final IRI Location = createIRI(NS + "Location");
    public static final IRI Male = createIRI(NS + "Male");
    public static final IRI Me = createIRI(NS + "Me");
    public static final IRI Met = createIRI(NS + "Met");
    public static final IRI Muse = createIRI(NS + "Muse");
    public static final IRI Name = createIRI(NS + "Name");
    public static final IRI Neighbor = createIRI(NS + "Neighbor");
    public static final IRI None = createIRI(NS + "None");
    public static final IRI Organization = createIRI(NS + "Organization");
    public static final IRI Other = createIRI(NS + "Other");
    public static final IRI Pager = createIRI(NS + "Pager");
    public static final IRI Parent = createIRI(NS + "Parent");
    public static final IRI RelatedType = createIRI(NS + "RelatedType");
    public static final IRI Sibling = createIRI(NS + "Sibling");
    public static final IRI Spouse = createIRI(NS + "Spouse");
    public static final IRI Sweetheart = createIRI(NS + "Sweetheart");
    public static final IRI TelephoneType = createIRI(NS + "TelephoneType");
    public static final IRI Text = createIRI(NS + "Text");
    public static final IRI TextPhone = createIRI(NS + "TextPhone");
    public static final IRI Type = createIRI(NS + "Type");
    public static final IRI Unknown = createIRI(NS + "Unknown");
    public static final IRI VCard = createIRI(NS + "VCard");
    public static final IRI Video = createIRI(NS + "Video");
    public static final IRI Voice = createIRI(NS + "Voice");
    public static final IRI Work = createIRI(NS + "Work");

    /* Object Properties */
    public static final IRI hasAdditionalName = createIRI(NS + "hasAdditionalName");
    public static final IRI hasAddress = createIRI(NS + "hasAddress");
    public static final IRI hasCalendarBusy = createIRI(NS + "hasCalendarBusy");
    public static final IRI hasCalendarLink = createIRI(NS + "hasCalendarLink");
    public static final IRI hasCalendarRequest = createIRI(NS + "hasCalendarRequest");
    public static final IRI hasCategory = createIRI(NS + "hasCategory");
    public static final IRI hasCountryName = createIRI(NS + "hasCountryName");
    public static final IRI hasEmail = createIRI(NS + "hasEmail");
    public static final IRI hasFamilyName = createIRI(NS + "hasFamilyName");
    public static final IRI hasFN = createIRI(NS + "hasFN");
    public static final IRI hasGender = createIRI(NS + "hasGender");
    public static final IRI hasGeo = createIRI(NS + "hasGeo");
    public static final IRI hasGivenName = createIRI(NS + "hasGivenName");
    public static final IRI hasHonorificPrefix = createIRI(NS + "hasHonorificPrefix");
    public static final IRI hasHonorificSuffix = createIRI(NS + "hasHonorificSuffix");
    public static final IRI hasInstantMessage = createIRI(NS + "hasInstantMessage");
    public static final IRI hasKey = createIRI(NS + "hasKey");
    public static final IRI hasLanguage = createIRI(NS + "hasLanguage");
    public static final IRI hasLocality = createIRI(NS + "hasLocality");
    public static final IRI hasLogo = createIRI(NS + "hasLogo");
    public static final IRI hasMember = createIRI(NS + "hasMember");
    public static final IRI hasName = createIRI(NS + "hasName");
    public static final IRI hasNickname = createIRI(NS + "hasNickname");
    public static final IRI hasNote = createIRI(NS + "hasNote");
    public static final IRI hasOrganizationName = createIRI(NS + "hasOrganizationName");
    public static final IRI hasOrganizationUnit = createIRI(NS + "hasOrganizationUnit");
    public static final IRI hasPhoto = createIRI(NS + "hasPhoto");
    public static final IRI hasPostalCode = createIRI(NS + "hasPostalCode");
    public static final IRI hasRegion = createIRI(NS + "hasRegion");
    public static final IRI hasRelated = createIRI(NS + "hasRelated");
    public static final IRI hasRole = createIRI(NS + "hasRole");
    public static final IRI hasSound = createIRI(NS + "hasSound");
    public static final IRI hasSource = createIRI(NS + "hasSource");
    public static final IRI hasStreetAddress = createIRI(NS + "hasStreetAddress");
    public static final IRI hasTelephone = createIRI(NS + "hasTelephone");
    public static final IRI hasTitle = createIRI(NS + "hasTitle");
    public static final IRI hasUID = createIRI(NS + "hasUID");
    public static final IRI hasURL = createIRI(NS + "hasURL");
    public static final IRI hasValue = createIRI(NS + "hasValue");

    /* Datatype Properties */
    public static final IRI additional_name = createIRI(NS + "additional-name");
    public static final IRI anniversary = createIRI(NS + "anniversary");
    public static final IRI bday = createIRI(NS + "bday");
    public static final IRI category = createIRI(NS + "category");
    public static final IRI country_name = createIRI(NS + "country-name");
    public static final IRI family_name = createIRI(NS + "family-name");
    public static final IRI fn = createIRI(NS + "fn");
    public static final IRI given_name = createIRI(NS + "given-name");
    public static final IRI honorific_prefix = createIRI(NS + "honorific-prefix");
    public static final IRI honorific_suffix = createIRI(NS + "honorific-suffix");
    public static final IRI language = createIRI(NS + "language");
    public static final IRI locality = createIRI(NS + "locality");
    public static final IRI nickname = createIRI(NS + "nickname");
    public static final IRI note = createIRI(NS + "note");
    public static final IRI organization_name = createIRI(NS + "organization-name");
    public static final IRI organization_unit = createIRI(NS + "organization-unit");
    public static final IRI postal_code = createIRI(NS + "postal-code");
    public static final IRI prodid = createIRI(NS + "prodid");
    public static final IRI region = createIRI(NS + "region");
    public static final IRI rev = createIRI(NS + "rev");
    public static final IRI role = createIRI(NS + "role");
    public static final IRI sort_string = createIRI(NS + "sort-string");
    public static final IRI street_address = createIRI(NS + "street-address");
    public static final IRI title = createIRI(NS + "title");
    public static final IRI tz = createIRI(NS + "tz");
    public static final IRI value = createIRI(NS + "value");

    private VCARD() {
        // prevent instantiation
    }
}
