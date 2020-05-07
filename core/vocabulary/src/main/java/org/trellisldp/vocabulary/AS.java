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
 * RDF Terms from the W3C Activity Streams Vocabulary.
 *
 * @see <a href="https://www.w3.org/TR/activitystreams-vocabulary/">Activity Streams Vocabulary</a>
 *
 * @author acoburn
 */
public final class AS {

    /* Namespace */
    private static final String URI = "https://www.w3.org/ns/activitystreams#";

    /* Classes */
    public static final IRI Accept = createIRI(getNamespace() + "Accept");
    public static final IRI Activity = createIRI(getNamespace() + "Activity");
    public static final IRI IntransitiveActivity = createIRI(getNamespace() + "IntransitiveActivity");
    public static final IRI Add = createIRI(getNamespace() + "Add");
    public static final IRI Announce = createIRI(getNamespace() + "Announce");
    public static final IRI Application = createIRI(getNamespace() + "Application");
    public static final IRI Arrive = createIRI(getNamespace() + "Arrive");
    public static final IRI Article = createIRI(getNamespace() + "Article");
    public static final IRI Audio = createIRI(getNamespace() + "Audio");
    public static final IRI Block = createIRI(getNamespace() + "Block");
    public static final IRI Collection = createIRI(getNamespace() + "Collection");
    public static final IRI CollectionPage = createIRI(getNamespace() + "CollectionPage");
    public static final IRI Relationship = createIRI(getNamespace() + "Relationship");
    public static final IRI Create = createIRI(getNamespace() + "Create");
    public static final IRI Delete = createIRI(getNamespace() + "Delete");
    public static final IRI Dislike = createIRI(getNamespace() + "Dislike");
    public static final IRI Document = createIRI(getNamespace() + "Document");
    public static final IRI Event = createIRI(getNamespace() + "Event");
    public static final IRI Follow = createIRI(getNamespace() + "Follow");
    public static final IRI Flag = createIRI(getNamespace() + "Flag");
    public static final IRI Group = createIRI(getNamespace() + "Group");
    public static final IRI Ignore = createIRI(getNamespace() + "Ignore");
    public static final IRI Image = createIRI(getNamespace() + "Image");
    public static final IRI Invite = createIRI(getNamespace() + "Invite");
    public static final IRI Join = createIRI(getNamespace() + "Join");
    public static final IRI Leave = createIRI(getNamespace() + "Leave");
    public static final IRI Like = createIRI(getNamespace() + "Like");
    public static final IRI Link = createIRI(getNamespace() + "Link");
    public static final IRI Mention = createIRI(getNamespace() + "Mention");
    public static final IRI Note = createIRI(getNamespace() + "Note");
    public static final IRI Object = createIRI(getNamespace() + "Object");
    public static final IRI Offer = createIRI(getNamespace() + "Offer");
    public static final IRI OrderedCollection = createIRI(getNamespace() + "OrderedCollection");
    public static final IRI OrderedCollectionPage = createIRI(getNamespace() + "OrderedCollectionPage");
    public static final IRI Organization = createIRI(getNamespace() + "Organization");
    public static final IRI Page = createIRI(getNamespace() + "Page");
    public static final IRI Person = createIRI(getNamespace() + "Person");
    public static final IRI Place = createIRI(getNamespace() + "Place");
    public static final IRI Profile = createIRI(getNamespace() + "Profile");
    public static final IRI Question = createIRI(getNamespace() + "Question");
    public static final IRI Reject = createIRI(getNamespace() + "Reject");
    public static final IRI Remove = createIRI(getNamespace() + "Remove");
    public static final IRI Service = createIRI(getNamespace() + "Service");
    public static final IRI TentativeAccept = createIRI(getNamespace() + "TentativeAccept");
    public static final IRI TentativeReject = createIRI(getNamespace() + "TentativeReject");
    public static final IRI Tombstone = createIRI(getNamespace() + "Tombstone");
    public static final IRI Undo = createIRI(getNamespace() + "Undo");
    public static final IRI Update = createIRI(getNamespace() + "Update");
    public static final IRI Video = createIRI(getNamespace() + "Video");
    public static final IRI View = createIRI(getNamespace() + "View");
    public static final IRI Listen = createIRI(getNamespace() + "Listen");
    public static final IRI Read = createIRI(getNamespace() + "Read");
    public static final IRI Move = createIRI(getNamespace() + "Move");
    public static final IRI Travel = createIRI(getNamespace() + "Travel");
    public static final IRI IsFollowing = createIRI(getNamespace() + "IsFollowing");
    public static final IRI IsFollowedBy = createIRI(getNamespace() + "IsFollowedBy");
    public static final IRI IsContact = createIRI(getNamespace() + "IsContact");
    public static final IRI IsMember = createIRI(getNamespace() + "IsMember");

    /* Properties */
    public static final IRI subject = createIRI(getNamespace() + "subject");
    public static final IRI relationship = createIRI(getNamespace() + "relationship");
    public static final IRI actor = createIRI(getNamespace() + "actor");
    public static final IRI attributedTo = createIRI(getNamespace() + "attributedTo");
    public static final IRI attachment = createIRI(getNamespace() + "attachment");
    public static final IRI attachments = createIRI(getNamespace() + "attachments");
    public static final IRI author = createIRI(getNamespace() + "author");
    public static final IRI bcc = createIRI(getNamespace() + "bcc");
    public static final IRI bto = createIRI(getNamespace() + "bto");
    public static final IRI cc = createIRI(getNamespace() + "cc");
    public static final IRI context = createIRI(getNamespace() + "context");
    public static final IRI current = createIRI(getNamespace() + "current");
    public static final IRI first = createIRI(getNamespace() + "first");
    public static final IRI generator = createIRI(getNamespace() + "generator");
    public static final IRI icon = createIRI(getNamespace() + "icon");
    public static final IRI image = createIRI(getNamespace() + "image");
    public static final IRI inReplyTo = createIRI(getNamespace() + "inReplyTo");
    public static final IRI items = createIRI(getNamespace() + "items");
    public static final IRI instrument = createIRI(getNamespace() + "instrument");
    public static final IRI last = createIRI(getNamespace() + "last");
    public static final IRI location = createIRI(getNamespace() + "location");
    public static final IRI next = createIRI(getNamespace() + "next");
    public static final IRI object = createIRI(getNamespace() + "object");
    public static final IRI oneOf = createIRI(getNamespace() + "oneOf");
    public static final IRI anyOf = createIRI(getNamespace() + "anyOf");
    public static final IRI closed = createIRI(getNamespace() + "closed");
    public static final IRI origin = createIRI(getNamespace() + "origin");
    public static final IRI accuracy = createIRI(getNamespace() + "accuracy");
    public static final IRI prev = createIRI(getNamespace() + "prev");
    public static final IRI preview = createIRI(getNamespace() + "preview");
    public static final IRI provider = createIRI(getNamespace() + "provider");
    public static final IRI replies = createIRI(getNamespace() + "replies");
    public static final IRI result = createIRI(getNamespace() + "result");
    public static final IRI audience = createIRI(getNamespace() + "audience");
    public static final IRI partOf = createIRI(getNamespace() + "partOf");
    public static final IRI tag = createIRI(getNamespace() + "tag");
    public static final IRI target = createIRI(getNamespace() + "target");
    public static final IRI to = createIRI(getNamespace() + "to");
    public static final IRI url = createIRI(getNamespace() + "url");
    public static final IRI altitude = createIRI(getNamespace() + "altitude");
    public static final IRI content = createIRI(getNamespace() + "content");
    public static final IRI name = createIRI(getNamespace() + "name");
    public static final IRI downStreamDuplicates = createIRI(getNamespace() + "downStreamDuplicates");
    public static final IRI duration = createIRI(getNamespace() + "duration");
    public static final IRI endTime = createIRI(getNamespace() + "endTime");
    public static final IRI height = createIRI(getNamespace() + "height");
    public static final IRI href = createIRI(getNamespace() + "href");
    public static final IRI hreflang = createIRI(getNamespace() + "hreflang");
    public static final IRI latitude = createIRI(getNamespace() + "latitude");
    public static final IRI longitude = createIRI(getNamespace() + "longitude");
    public static final IRI mediaType = createIRI(getNamespace() + "mediaType");
    public static final IRI published = createIRI(getNamespace() + "published");
    public static final IRI radius = createIRI(getNamespace() + "radius");
    public static final IRI rating = createIRI(getNamespace() + "rating");
    public static final IRI rel = createIRI(getNamespace() + "rel");
    public static final IRI startIndex = createIRI(getNamespace() + "startIndex");
    public static final IRI startTime = createIRI(getNamespace() + "startTime");
    public static final IRI summary = createIRI(getNamespace() + "summary");
    public static final IRI totalItems = createIRI(getNamespace() + "totalItems");
    public static final IRI units = createIRI(getNamespace() + "units");
    public static final IRI updated = createIRI(getNamespace() + "updated");
    public static final IRI upstreamDuplicates = createIRI(getNamespace() + "upstreamDuplicates");
    public static final IRI verb = createIRI(getNamespace() + "verb");
    public static final IRI width = createIRI(getNamespace() + "width");
    public static final IRI describes = createIRI(getNamespace() + "describes");
    public static final IRI formerType = createIRI(getNamespace() + "formerType");
    public static final IRI deleted = createIRI(getNamespace() + "deleted");
    public static final IRI outbox = createIRI(getNamespace() + "outbox");
    public static final IRI following = createIRI(getNamespace() + "following");
    public static final IRI followers = createIRI(getNamespace() + "followers");
    public static final IRI streams = createIRI(getNamespace() + "streams");
    public static final IRI preferredUsername = createIRI(getNamespace() + "preferredUsername");
    public static final IRI endpoints = createIRI(getNamespace() + "endpoints");
    public static final IRI uploadMedia = createIRI(getNamespace() + "uploadMedia");
    public static final IRI proxyUrl = createIRI(getNamespace() + "proxyUrl");
    public static final IRI oauthClientAuthorize = createIRI(getNamespace() + "oauthClientAuthorize");
    public static final IRI provideClientKey = createIRI(getNamespace() + "provideClientKey");
    public static final IRI authorizeClientKey = createIRI(getNamespace() + "authorizeClientKey");
    public static final IRI source = createIRI(getNamespace() + "source");

    /**
     * get the namespace.
     *
     * @return namespace
     */
    public static String getNamespace() {
        return URI;
    }

    private AS() {
        // prevent instantiation
    }
}
