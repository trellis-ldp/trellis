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
 * RDF Terms from the W3C Activity Streams Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/">Activity Streams Vocabulary</a>
 *
 * @author acoburn
 */
public final class AS {

    /* Namespace */
    public static final String NS = "https://www.w3.org/ns/activitystreams#";

    /* Classes */
    public static final IRI Accept = createIRI(NS + "Accept");
    public static final IRI Activity = createIRI(NS + "Activity");
    public static final IRI IntransitiveActivity = createIRI(NS + "IntransitiveActivity");
    public static final IRI Add = createIRI(NS + "Add");
    public static final IRI Announce = createIRI(NS + "Announce");
    public static final IRI Application = createIRI(NS + "Application");
    public static final IRI Arrive = createIRI(NS + "Arrive");
    public static final IRI Article = createIRI(NS + "Article");
    public static final IRI Audio = createIRI(NS + "Audio");
    public static final IRI Block = createIRI(NS + "Block");
    public static final IRI Collection = createIRI(NS + "Collection");
    public static final IRI CollectionPage = createIRI(NS + "CollectionPage");
    public static final IRI Relationship = createIRI(NS + "Relationship");
    public static final IRI Create = createIRI(NS + "Create");
    public static final IRI Delete = createIRI(NS + "Delete");
    public static final IRI Dislike = createIRI(NS + "Dislike");
    public static final IRI Document = createIRI(NS + "Document");
    public static final IRI Event = createIRI(NS + "Event");
    public static final IRI Follow = createIRI(NS + "Follow");
    public static final IRI Flag = createIRI(NS + "Flag");
    public static final IRI Group = createIRI(NS + "Group");
    public static final IRI Ignore = createIRI(NS + "Ignore");
    public static final IRI Image = createIRI(NS + "Image");
    public static final IRI Invite = createIRI(NS + "Invite");
    public static final IRI Join = createIRI(NS + "Join");
    public static final IRI Leave = createIRI(NS + "Leave");
    public static final IRI Like = createIRI(NS + "Like");
    public static final IRI Link = createIRI(NS + "Link");
    public static final IRI Mention = createIRI(NS + "Mention");
    public static final IRI Note = createIRI(NS + "Note");
    public static final IRI Object = createIRI(NS + "Object");
    public static final IRI Offer = createIRI(NS + "Offer");
    public static final IRI OrderedCollection = createIRI(NS + "OrderedCollection");
    public static final IRI OrderedCollectionPage = createIRI(NS + "OrderedCollectionPage");
    public static final IRI Organization = createIRI(NS + "Organization");
    public static final IRI Page = createIRI(NS + "Page");
    public static final IRI Person = createIRI(NS + "Person");
    public static final IRI Place = createIRI(NS + "Place");
    public static final IRI Profile = createIRI(NS + "Profile");
    public static final IRI Question = createIRI(NS + "Question");
    public static final IRI Reject = createIRI(NS + "Reject");
    public static final IRI Remove = createIRI(NS + "Remove");
    public static final IRI Service = createIRI(NS + "Service");
    public static final IRI TentativeAccept = createIRI(NS + "TentativeAccept");
    public static final IRI TentativeReject = createIRI(NS + "TentativeReject");
    public static final IRI Tombstone = createIRI(NS + "Tombstone");
    public static final IRI Undo = createIRI(NS + "Undo");
    public static final IRI Update = createIRI(NS + "Update");
    public static final IRI Video = createIRI(NS + "Video");
    public static final IRI View = createIRI(NS + "View");
    public static final IRI Listen = createIRI(NS + "Listen");
    public static final IRI Read = createIRI(NS + "Read");
    public static final IRI Move = createIRI(NS + "Move");
    public static final IRI Travel = createIRI(NS + "Travel");
    public static final IRI IsFollowing = createIRI(NS + "IsFollowing");
    public static final IRI IsFollowedBy = createIRI(NS + "IsFollowedBy");
    public static final IRI IsContact = createIRI(NS + "IsContact");
    public static final IRI IsMember = createIRI(NS + "IsMember");

    /* Properties */
    public static final IRI subject = createIRI(NS + "subject");
    public static final IRI relationship = createIRI(NS + "relationship");
    public static final IRI actor = createIRI(NS + "actor");
    public static final IRI attributedTo = createIRI(NS + "attributedTo");
    public static final IRI attachment = createIRI(NS + "attachment");
    public static final IRI attachments = createIRI(NS + "attachments");
    public static final IRI author = createIRI(NS + "author");
    public static final IRI bcc = createIRI(NS + "bcc");
    public static final IRI bto = createIRI(NS + "bto");
    public static final IRI cc = createIRI(NS + "cc");
    public static final IRI context = createIRI(NS + "context");
    public static final IRI current = createIRI(NS + "current");
    public static final IRI first = createIRI(NS + "first");
    public static final IRI generator = createIRI(NS + "generator");
    public static final IRI icon = createIRI(NS + "icon");
    public static final IRI image = createIRI(NS + "image");
    public static final IRI inReplyTo = createIRI(NS + "inReplyTo");
    public static final IRI items = createIRI(NS + "items");
    public static final IRI instrument = createIRI(NS + "instrument");
    public static final IRI last = createIRI(NS + "last");
    public static final IRI location = createIRI(NS + "location");
    public static final IRI next = createIRI(NS + "next");
    public static final IRI object = createIRI(NS + "object");
    public static final IRI oneOf = createIRI(NS + "oneOf");
    public static final IRI anyOf = createIRI(NS + "anyOf");
    public static final IRI closed = createIRI(NS + "closed");
    public static final IRI origin = createIRI(NS + "origin");
    public static final IRI accuracy = createIRI(NS + "accuracy");
    public static final IRI prev = createIRI(NS + "prev");
    public static final IRI preview = createIRI(NS + "preview");
    public static final IRI provider = createIRI(NS + "provider");
    public static final IRI replies = createIRI(NS + "replies");
    public static final IRI result = createIRI(NS + "result");
    public static final IRI audience = createIRI(NS + "audience");
    public static final IRI partOf = createIRI(NS + "partOf");
    public static final IRI tag = createIRI(NS + "tag");
    public static final IRI target = createIRI(NS + "target");
    public static final IRI to = createIRI(NS + "to");
    public static final IRI url = createIRI(NS + "url");
    public static final IRI altitude = createIRI(NS + "altitude");
    public static final IRI content = createIRI(NS + "content");
    public static final IRI name = createIRI(NS + "name");
    public static final IRI downStreamDuplicates = createIRI(NS + "downStreamDuplicates");
    public static final IRI duration = createIRI(NS + "duration");
    public static final IRI endTime = createIRI(NS + "endTime");
    public static final IRI height = createIRI(NS + "height");
    public static final IRI href = createIRI(NS + "href");
    public static final IRI hreflang = createIRI(NS + "hreflang");
    public static final IRI latitude = createIRI(NS + "latitude");
    public static final IRI longitude = createIRI(NS + "longitude");
    public static final IRI mediaType = createIRI(NS + "mediaType");
    public static final IRI published = createIRI(NS + "published");
    public static final IRI radius = createIRI(NS + "radius");
    public static final IRI rating = createIRI(NS + "rating");
    public static final IRI rel = createIRI(NS + "rel");
    public static final IRI startIndex = createIRI(NS + "startIndex");
    public static final IRI startTime = createIRI(NS + "startTime");
    public static final IRI summary = createIRI(NS + "summary");
    public static final IRI totalItems = createIRI(NS + "totalItems");
    public static final IRI units = createIRI(NS + "units");
    public static final IRI updated = createIRI(NS + "updated");
    public static final IRI upstreamDuplicates = createIRI(NS + "upstreamDuplicates");
    public static final IRI verb = createIRI(NS + "verb");
    public static final IRI width = createIRI(NS + "width");
    public static final IRI describes = createIRI(NS + "describes");
    public static final IRI formerType = createIRI(NS + "formerType");
    public static final IRI deleted = createIRI(NS + "deleted");
    public static final IRI outbox = createIRI(NS + "outbox");
    public static final IRI following = createIRI(NS + "following");
    public static final IRI followers = createIRI(NS + "followers");
    public static final IRI streams = createIRI(NS + "streams");
    public static final IRI preferredUsername = createIRI(NS + "preferredUsername");
    public static final IRI endpoints = createIRI(NS + "endpoints");
    public static final IRI uploadMedia = createIRI(NS + "uploadMedia");
    public static final IRI proxyUrl = createIRI(NS + "proxyUrl");
    public static final IRI oauthClientAuthorize = createIRI(NS + "oauthClientAuthorize");
    public static final IRI provideClientKey = createIRI(NS + "provideClientKey");
    public static final IRI authorizeClientKey = createIRI(NS + "authorizeClientKey");
    public static final IRI source = createIRI(NS + "source");

    private AS() {
        // prevent instantiation
    }
}
