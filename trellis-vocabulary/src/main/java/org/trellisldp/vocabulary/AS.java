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
 * RDF Terms from the W3C Activity Streams Vocabulary
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/">Activity Streams Vocabulary</a>
 *
 * @author acoburn
 */
public final class AS extends BaseVocabulary {

    /* Namespace */
    public static final String URI = "https://www.w3.org/ns/activitystreams#";

    /* Classes */
    public static final IRI Accept = createIRI(URI + "Accept");
    public static final IRI Activity = createIRI(URI + "Activity");
    public static final IRI IntransitiveActivity = createIRI(URI + "IntransitiveActivity");
    public static final IRI Add = createIRI(URI + "Add");
    public static final IRI Announce = createIRI(URI + "Announce");
    public static final IRI Application = createIRI(URI + "Application");
    public static final IRI Arrive = createIRI(URI + "Arrive");
    public static final IRI Article = createIRI(URI + "Article");
    public static final IRI Audio = createIRI(URI + "Audio");
    public static final IRI Block = createIRI(URI + "Block");
    public static final IRI Collection = createIRI(URI + "Collection");
    public static final IRI CollectionPage = createIRI(URI + "CollectionPage");
    public static final IRI Relationship = createIRI(URI + "Relationship");
    public static final IRI Create = createIRI(URI + "Create");
    public static final IRI Delete = createIRI(URI + "Delete");
    public static final IRI Dislike = createIRI(URI + "Dislike");
    public static final IRI Document = createIRI(URI + "Document");
    public static final IRI Event = createIRI(URI + "Event");
    public static final IRI Follow = createIRI(URI + "Follow");
    public static final IRI Flag = createIRI(URI + "Flag");
    public static final IRI Group = createIRI(URI + "Group");
    public static final IRI Ignore = createIRI(URI + "Ignore");
    public static final IRI Image = createIRI(URI + "Image");
    public static final IRI Invite = createIRI(URI + "Invite");
    public static final IRI Join = createIRI(URI + "Join");
    public static final IRI Leave = createIRI(URI + "Leave");
    public static final IRI Like = createIRI(URI + "Like");
    public static final IRI Link = createIRI(URI + "Link");
    public static final IRI Mention = createIRI(URI + "Mention");
    public static final IRI Note = createIRI(URI + "Note");
    public static final IRI Object = createIRI(URI + "Object");
    public static final IRI Offer = createIRI(URI + "Offer");
    public static final IRI OrderedCollection = createIRI(URI + "OrderedCollection");
    public static final IRI OrderedCollectionPage = createIRI(URI + "OrderedCollectionPage");
    public static final IRI Organization = createIRI(URI + "Organization");
    public static final IRI Page = createIRI(URI + "Page");
    public static final IRI Person = createIRI(URI + "Person");
    public static final IRI Place = createIRI(URI + "Place");
    public static final IRI Profile = createIRI(URI + "Profile");
    public static final IRI Question = createIRI(URI + "Question");
    public static final IRI Reject = createIRI(URI + "Reject");
    public static final IRI Remove = createIRI(URI + "Remove");
    public static final IRI Service = createIRI(URI + "Service");
    public static final IRI TentativeAccept = createIRI(URI + "TentativeAccept");
    public static final IRI TentativeReject = createIRI(URI + "TentativeReject");
    public static final IRI Tombstone = createIRI(URI + "Tombstone");
    public static final IRI Undo = createIRI(URI + "Undo");
    public static final IRI Update = createIRI(URI + "Update");
    public static final IRI Video = createIRI(URI + "Video");
    public static final IRI View = createIRI(URI + "View");
    public static final IRI Listen = createIRI(URI + "Listen");
    public static final IRI Read = createIRI(URI + "Read");
    public static final IRI Move = createIRI(URI + "Move");
    public static final IRI Travel = createIRI(URI + "Travel");
    public static final IRI IsFollowing = createIRI(URI + "IsFollowing");
    public static final IRI IsFollowedBy = createIRI(URI + "IsFollowedBy");
    public static final IRI IsContact = createIRI(URI + "IsContact");
    public static final IRI IsMember = createIRI(URI + "IsMember");

    /* Properties */
    public static final IRI subject = createIRI(URI + "subject");
    public static final IRI relationship = createIRI(URI + "relationship");
    public static final IRI actor = createIRI(URI + "actor");
    public static final IRI attributedTo = createIRI(URI + "attributedTo");
    public static final IRI attachment = createIRI(URI + "attachment");
    public static final IRI attachments = createIRI(URI + "attachments");
    public static final IRI author = createIRI(URI + "author");
    public static final IRI bcc = createIRI(URI + "bcc");
    public static final IRI bto = createIRI(URI + "bto");
    public static final IRI cc = createIRI(URI + "cc");
    public static final IRI context = createIRI(URI + "context");
    public static final IRI current = createIRI(URI + "current");
    public static final IRI first = createIRI(URI + "first");
    public static final IRI generator = createIRI(URI + "generator");
    public static final IRI icon = createIRI(URI + "icon");
    public static final IRI image = createIRI(URI + "image");
    public static final IRI inReplyTo = createIRI(URI + "inReplyTo");
    public static final IRI items = createIRI(URI + "items");
    public static final IRI instrument = createIRI(URI + "instrument");
    public static final IRI last = createIRI(URI + "last");
    public static final IRI location = createIRI(URI + "location");
    public static final IRI next = createIRI(URI + "next");
    public static final IRI object = createIRI(URI + "object");
    public static final IRI oneOf = createIRI(URI + "oneOf");
    public static final IRI anyOf = createIRI(URI + "anyOf");
    public static final IRI closed = createIRI(URI + "closed");
    public static final IRI origin = createIRI(URI + "origin");
    public static final IRI accuracy = createIRI(URI + "accuracy");
    public static final IRI prev = createIRI(URI + "prev");
    public static final IRI preview = createIRI(URI + "preview");
    public static final IRI provider = createIRI(URI + "provider");
    public static final IRI replies = createIRI(URI + "replies");
    public static final IRI result = createIRI(URI + "result");
    public static final IRI audience = createIRI(URI + "audience");
    public static final IRI partOf = createIRI(URI + "partOf");
    public static final IRI tag = createIRI(URI + "tag");
    public static final IRI target = createIRI(URI + "target");
    public static final IRI to = createIRI(URI + "to");
    public static final IRI url = createIRI(URI + "url");
    public static final IRI altitude = createIRI(URI + "altitude");
    public static final IRI content = createIRI(URI + "content");
    public static final IRI name = createIRI(URI + "name");
    public static final IRI downStreamDuplicates = createIRI(URI + "downStreamDuplicates");
    public static final IRI duration = createIRI(URI + "duration");
    public static final IRI endTime = createIRI(URI + "endTime");
    public static final IRI height = createIRI(URI + "height");
    public static final IRI href = createIRI(URI + "href");
    public static final IRI hreflang = createIRI(URI + "hreflang");
    public static final IRI latitude = createIRI(URI + "latitude");
    public static final IRI longitude = createIRI(URI + "longitude");
    public static final IRI mediaType = createIRI(URI + "mediaType");
    public static final IRI published = createIRI(URI + "published");
    public static final IRI radius = createIRI(URI + "radius");
    public static final IRI rating = createIRI(URI + "rating");
    public static final IRI rel = createIRI(URI + "rel");
    public static final IRI startIndex = createIRI(URI + "startIndex");
    public static final IRI startTime = createIRI(URI + "startTime");
    public static final IRI summary = createIRI(URI + "summary");
    public static final IRI totalItems = createIRI(URI + "totalItems");
    public static final IRI units = createIRI(URI + "units");
    public static final IRI updated = createIRI(URI + "updated");
    public static final IRI upstreamDuplicates = createIRI(URI + "upstreamDuplicates");
    public static final IRI verb = createIRI(URI + "verb");
    public static final IRI width = createIRI(URI + "width");
    public static final IRI describes = createIRI(URI + "describes");
    public static final IRI formerType = createIRI(URI + "formerType");
    public static final IRI deleted = createIRI(URI + "deleted");
    public static final IRI outbox = createIRI(URI + "outbox");
    public static final IRI following = createIRI(URI + "following");
    public static final IRI followers = createIRI(URI + "followers");
    public static final IRI streams = createIRI(URI + "streams");
    public static final IRI preferredUsername = createIRI(URI + "preferredUsername");
    public static final IRI endpoints = createIRI(URI + "endpoints");
    public static final IRI uploadMedia = createIRI(URI + "uploadMedia");
    public static final IRI proxyUrl = createIRI(URI + "proxyUrl");
    public static final IRI oauthClientAuthorize = createIRI(URI + "oauthClientAuthorize");
    public static final IRI provideClientKey = createIRI(URI + "provideClientKey");
    public static final IRI authorizeClientKey = createIRI(URI + "authorizeClientKey");
    public static final IRI source = createIRI(URI + "source");

    private AS() {
        super();
    }
}
