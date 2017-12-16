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
package org.trellisldp.http.domain;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toSet;
import static org.trellisldp.vocabulary.LDP.PreferContainment;
import static org.trellisldp.vocabulary.LDP.PreferMembership;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.util.Set;

import org.apache.commons.rdf.api.IRI;

/**
 * A collection of constant values used by the Trellis HTTP layer
 *
 * @author acoburn
 */
public final class HttpConstants {

    public static final String ACCEPT_DATETIME = "Accept-Datetime";

    public static final String ACCEPT_PATCH = "Accept-Patch";

    public static final String ACCEPT_POST = "Accept-Post";

    public static final String ACCEPT_RANGES = "Accept-Ranges";

    public static final String ACL = "acl";

    public static final String APPLICATION_LINK_FORMAT = "application/link-format";

    public static final String DIGEST = "Digest";

    public static final String LINK_TEMPLATE = "Link-Template";

    public static final String MEMENTO_DATETIME = "Memento-Datetime";

    public static final String PATCH = "PATCH";

    public static final String PREFER = "Prefer";

    public static final String PREFERENCE_APPLIED = "Preference-Applied";

    public static final String RANGE = "Range";

    public static final String SESSION_PROPERTY = "session";

    public static final String TIMEMAP = "timemap";

    public static final String UPLOADS = "uploads";

    public static final String UPLOAD_PREFIX = "upload/";

    public static final String WANT_DIGEST = "Want-Digest";

    public static final Set<String> DEFAULT_REPRESENTATION = unmodifiableSet(asList(PreferContainment, PreferMembership,
                PreferUserManaged).stream().map(IRI::getIRIString).collect(toSet()));

    private HttpConstants() {
        // prevent instantiation
    }
}
