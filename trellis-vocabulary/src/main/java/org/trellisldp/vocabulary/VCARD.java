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
 * RDF Terms from the W3C vCARD Vocabulary
 *
 * @see <a href="https://www.w3.org/TR/vcard-rdf">vCARD</a>
 *
 * @author acoburn
 */
public final class VCARD extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "http://www.w3.org/2006/vcard/ns#";

    /* Classes */
    public static final IRI Acquaintance = createIRI(URI + "Acquaintance");
    public static final IRI Address = createIRI(URI + "Address");
    public static final IRI Agent = createIRI(URI + "Agent");
    public static final IRI Cell = createIRI(URI + "Cell");
    public static final IRI Child = createIRI(URI + "Child");
    public static final IRI Colleague = createIRI(URI + "Colleague");
    public static final IRI Contact = createIRI(URI + "Contact");
    public static final IRI Coresident = createIRI(URI + "Coresident");
    public static final IRI Coworker = createIRI(URI + "Coworker");
    public static final IRI Crush = createIRI(URI + "Crush");
    public static final IRI Date = createIRI(URI + "Date");
    public static final IRI Emergency = createIRI(URI + "Emergency");
    public static final IRI Fax = createIRI(URI + "Fax");
    public static final IRI Female = createIRI(URI + "Female");
    public static final IRI Friend = createIRI(URI + "Friend");
    public static final IRI Gender = createIRI(URI + "Gender");
    public static final IRI Group = createIRI(URI + "Group");
    public static final IRI Home = createIRI(URI + "Home");
    public static final IRI Individual = createIRI(URI + "Individual");
    public static final IRI Kin = createIRI(URI + "Kin");
    public static final IRI Kind = createIRI(URI + "Kind");
    public static final IRI Location = createIRI(URI + "Location");
    public static final IRI Male = createIRI(URI + "Male");
    public static final IRI Me = createIRI(URI + "Me");
    public static final IRI Met = createIRI(URI + "Met");
    public static final IRI Muse = createIRI(URI + "Muse");
    public static final IRI Name = createIRI(URI + "Name");
    public static final IRI Neighbor = createIRI(URI + "Neighbor");
    public static final IRI None = createIRI(URI + "None");
    public static final IRI Organization = createIRI(URI + "Organization");
    public static final IRI Other = createIRI(URI + "Other");
    public static final IRI Pager = createIRI(URI + "Pager");
    public static final IRI Parent = createIRI(URI + "Parent");
    public static final IRI RelatedType = createIRI(URI + "RelatedType");
    public static final IRI Sibling = createIRI(URI + "Sibling");
    public static final IRI Spouse = createIRI(URI + "Spouse");
    public static final IRI Sweetheart = createIRI(URI + "Sweetheart");
    public static final IRI TelephoneType = createIRI(URI + "TelephoneType");
    public static final IRI Text = createIRI(URI + "Text");
    public static final IRI TextPhone = createIRI(URI + "TextPhone");
    public static final IRI Type = createIRI(URI + "Type");
    public static final IRI Unknown = createIRI(URI + "Unknown");
    public static final IRI VCard = createIRI(URI + "VCard");
    public static final IRI Video = createIRI(URI + "Video");
    public static final IRI Voice = createIRI(URI + "Voice");
    public static final IRI Work = createIRI(URI + "Work");

    /* Object Properties */
    public static final IRI hasAdditionalName = createIRI(URI + "hasAdditionalName");
    public static final IRI hasAddress = createIRI(URI + "hasAddress");
    public static final IRI hasCalendarBusy = createIRI(URI + "hasCalendarBusy");
    public static final IRI hasCalendarLink = createIRI(URI + "hasCalendarLink");
    public static final IRI hasCalendarRequest = createIRI(URI + "hasCalendarRequest");
    public static final IRI hasCategory = createIRI(URI + "hasCategory");
    public static final IRI hasCountryName = createIRI(URI + "hasCountryName");
    public static final IRI hasEmail = createIRI(URI + "hasEmail");
    public static final IRI hasFamilyName = createIRI(URI + "hasFamilyName");
    public static final IRI hasFN = createIRI(URI + "hasFN");
    public static final IRI hasGender = createIRI(URI + "hasGender");
    public static final IRI hasGeo = createIRI(URI + "hasGeo");
    public static final IRI hasGivenName = createIRI(URI + "hasGivenName");
    public static final IRI hasHonorificPrefix = createIRI(URI + "hasHonorificPrefix");
    public static final IRI hasHonorificSuffix = createIRI(URI + "hasHonorificSuffix");
    public static final IRI hasInstantMessage = createIRI(URI + "hasInstantMessage");
    public static final IRI hasKey = createIRI(URI + "hasKey");
    public static final IRI hasLanguage = createIRI(URI + "hasLanguage");
    public static final IRI hasLocality = createIRI(URI + "hasLocality");
    public static final IRI hasLogo = createIRI(URI + "hasLogo");
    public static final IRI hasMember = createIRI(URI + "hasMember");
    public static final IRI hasName = createIRI(URI + "hasName");
    public static final IRI hasNickname = createIRI(URI + "hasNickname");
    public static final IRI hasNote = createIRI(URI + "hasNote");
    public static final IRI hasOrganizationName = createIRI(URI + "hasOrganizationName");
    public static final IRI hasOrganizationUnit = createIRI(URI + "hasOrganizationUnit");
    public static final IRI hasPhoto = createIRI(URI + "hasPhoto");
    public static final IRI hasPostalCode = createIRI(URI + "hasPostalCode");
    public static final IRI hasRegion = createIRI(URI + "hasRegion");
    public static final IRI hasRelated = createIRI(URI + "hasRelated");
    public static final IRI hasRole = createIRI(URI + "hasRole");
    public static final IRI hasSound = createIRI(URI + "hasSound");
    public static final IRI hasSource = createIRI(URI + "hasSource");
    public static final IRI hasStreetAddress = createIRI(URI + "hasStreetAddress");
    public static final IRI hasTelephone = createIRI(URI + "hasTelephone");
    public static final IRI hasTitle = createIRI(URI + "hasTitle");
    public static final IRI hasUID = createIRI(URI + "hasUID");
    public static final IRI hasURL = createIRI(URI + "hasURL");
    public static final IRI hasValue = createIRI(URI + "hasValue");

    /* Datatype Properties */
    public static final IRI additional_name = createIRI(URI + "additional-name");
    public static final IRI anniversary = createIRI(URI + "anniversary");
    public static final IRI bday = createIRI(URI + "bday");
    public static final IRI category = createIRI(URI + "category");
    public static final IRI country_name = createIRI(URI + "country-name");
    public static final IRI family_name = createIRI(URI + "family-name");
    public static final IRI fn = createIRI(URI + "fn");
    public static final IRI given_name = createIRI(URI + "given-name");
    public static final IRI honorific_prefix = createIRI(URI + "honorific-prefix");
    public static final IRI honorific_suffix = createIRI(URI + "honorific-suffix");
    public static final IRI language = createIRI(URI + "language");
    public static final IRI locality = createIRI(URI + "locality");
    public static final IRI nickname = createIRI(URI + "nickname");
    public static final IRI note = createIRI(URI + "note");
    public static final IRI organization_name = createIRI(URI + "organization-name");
    public static final IRI organization_unit = createIRI(URI + "organization-unit");
    public static final IRI postal_code = createIRI(URI + "postal-code");
    public static final IRI prodid = createIRI(URI + "prodid");
    public static final IRI region = createIRI(URI + "region");
    public static final IRI rev = createIRI(URI + "rev");
    public static final IRI role = createIRI(URI + "role");
    public static final IRI sort_string = createIRI(URI + "sort-string");
    public static final IRI street_address = createIRI(URI + "street-address");
    public static final IRI title = createIRI(URI + "title");
    public static final IRI tz = createIRI(URI + "tz");
    public static final IRI value = createIRI(URI + "value");

    private VCARD() {
        super();
    }
}
