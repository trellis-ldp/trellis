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
 * RDF Terms from the FOAF Vocabulary.
 *
 * @see <a href="http://xmlns.com/foaf/spec/">Foaf Vocabulary</a>
 *
 * @author acoburn
 */
public final class FOAF {

    /* Namespace */
    public static final String NS = "http://xmlns.com/foaf/0.1/";

    /* Classes */
    public static final IRI Agent = createIRI(NS + "Agent");
    public static final IRI Document = createIRI(NS + "Document");
    public static final IRI Group = createIRI(NS + "Group");
    public static final IRI Image = createIRI(NS + "Image");
    public static final IRI LabelProperty = createIRI(NS + "LabelProperty");
    public static final IRI OnlineAccount = createIRI(NS + "OnlineAccount");
    public static final IRI OnlineChatAccount = createIRI(NS + "OnlineEcommerceAccount");
    public static final IRI OnlineEcommerceAccount = createIRI(NS + "OnlineEcommerceAccount");
    public static final IRI OnlineGamingAccount = createIRI(NS + "OnlineGamingAccount");
    public static final IRI Organization = createIRI(NS + "Organization");
    public static final IRI Person = createIRI(NS + "Person");
    public static final IRI PersonalProfileDocument = createIRI(NS + "PersonalProfileDocument");
    public static final IRI Project = createIRI(NS + "Project");

    /* Properties */
    public static final IRI account = createIRI(NS + "account");
    public static final IRI accountName = createIRI(NS + "accountName");
    public static final IRI accountServiceHomepage = createIRI(NS + "accountServiceHomepage");
    public static final IRI age = createIRI(NS + "age");
    public static final IRI aimChatID = createIRI(NS + "aimChatID");
    public static final IRI birthday = createIRI(NS + "birthday");
    public static final IRI currentProject = createIRI(NS + "currentProject");
    public static final IRI depicts = createIRI(NS + "depicts");
    public static final IRI depiction = createIRI(NS + "depiction");
    public static final IRI dnaChecksum = createIRI(NS + "dnaChecksum");
    public static final IRI familyName = createIRI(NS + "familyName");
    public static final IRI firstName = createIRI(NS + "firstName");
    public static final IRI focus = createIRI(NS + "focus");
    public static final IRI fundedBy = createIRI(NS + "fundedBy");
    public static final IRI geekcode = createIRI(NS + "geekcode");
    public static final IRI gender = createIRI(NS + "gender");
    public static final IRI givenname = createIRI(NS + "givenname");
    public static final IRI givenName = createIRI(NS + "givenName");
    public static final IRI holdsAccount = createIRI(NS + "holdsAccount");
    public static final IRI homepage = createIRI(NS + "homepage");
    public static final IRI icqChatID = createIRI(NS + "icqChatID");
    public static final IRI img = createIRI(NS + "img");
    public static final IRI interest = createIRI(NS + "interest");
    public static final IRI isPrimaryTopicOf = createIRI(NS + "isPrimaryTopicOf");
    public static final IRI jabberID = createIRI(NS + "jabberID");
    public static final IRI knows = createIRI(NS + "knows");
    public static final IRI lastName = createIRI(NS + "lastName");
    public static final IRI logo = createIRI(NS + "logo");
    public static final IRI made = createIRI(NS + "made");
    public static final IRI maker = createIRI(NS + "maker");
    public static final IRI member = createIRI(NS + "member");
    public static final IRI membershipClass = createIRI(NS + "membershipClass");
    public static final IRI mbox = createIRI(NS + "mbox");
    public static final IRI msnChatID = createIRI(NS + "msnChatID");
    public static final IRI myersBriggs = createIRI(NS + "myersBriggs");
    public static final IRI name = createIRI(NS + "name");
    public static final IRI nick = createIRI(NS + "nick");
    public static final IRI openid = createIRI(NS + "openid");
    public static final IRI page = createIRI(NS + "page");
    public static final IRI pastProject = createIRI(NS + "pastProject");
    public static final IRI phone = createIRI(NS + "phone");
    public static final IRI plan = createIRI(NS + "plan");
    public static final IRI primaryTopic = createIRI(NS + "primaryTopic");
    public static final IRI publications = createIRI(NS + "publications");
    public static final IRI schoolHomepage = createIRI(NS + "schoolHomepage");
    public static final IRI sha1 = createIRI(NS + "sha1");
    public static final IRI status = createIRI(NS + "status");
    public static final IRI skypeID = createIRI(NS + "skypeID");
    public static final IRI surname = createIRI(NS + "surname");
    public static final IRI theme = createIRI(NS + "theme");
    public static final IRI thumbnail = createIRI(NS + "thumbnail");
    public static final IRI tipjar = createIRI(NS + "tipjar");
    public static final IRI title = createIRI(NS + "title");
    public static final IRI topic = createIRI(NS + "topic");
    public static final IRI weblog = createIRI(NS + "weblog");
    public static final IRI workInfoHomepage = createIRI(NS + "workInfoHomepage");
    public static final IRI workplaceHomepage = createIRI(NS + "workplaceHomepage");
    public static final IRI yahooChatID = createIRI(NS + "yahooChatID");

    private FOAF() {
        // prevent instantiation
    }
}
