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
package org.trellisldp.http.core;

import static java.util.Collections.addAll;
import static java.util.Collections.unmodifiableSet;
import static org.trellisldp.vocabulary.LDP.PreferContainment;
import static org.trellisldp.vocabulary.LDP.PreferMembership;
import static org.trellisldp.vocabulary.Trellis.PreferServerManaged;
import static org.trellisldp.vocabulary.Trellis.PreferUserManaged;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.rdf.api.IRI;

/**
 * A collection of constant values used by the Trellis HTTP layer.
 *
 * @author acoburn
 */
public final class HttpConstants {

    /** The name of the HTTP request header used for Memento datetime negotiation. */
    public static final String ACCEPT_DATETIME = "Accept-Datetime";

    /** The name of the HTTP response header used to indicate the acceptable formats for PATCH requests. */
    public static final String ACCEPT_PATCH = "Accept-Patch";

    /** The name of the HTTP response header used to indicate the acceptable RDF formats in POST requests. */
    public static final String ACCEPT_POST = "Accept-Post";

    /** The name of the HTTP response header used to indicate the acceptable forms of range requests. */
    public static final String ACCEPT_RANGES = "Accept-Ranges";

    /** The ext parameter for ACL resources. */
    public static final String ACL = "acl";

    /** The query parameter for ACL resources. */
    public static final String ACL_QUERY_PARAM = "?ext=acl";

    /** The default Memento TimeMap output format. */
    public static final String APPLICATION_LINK_FORMAT = "application/link-format";

    /** Configuration key defining whether LDP interaction model modifications are supported. */
    public static final String CONFIG_HTTP_LDP_MODEL_MODIFICATIONS = "trellis.http.ldp-model-modifications";

    /** Configuration key defining the server's base URL. */
    public static final String CONFIG_HTTP_BASE_URL = "trellis.http.base-url";

    /** Configuration key defining the extension graph mapping. */
    public static final String CONFIG_HTTP_EXTENSION_GRAPHS = "trellis.http.extension-graphs";

    /** Configuration key defining whether to include dates in memento headers. */
    public static final String CONFIG_HTTP_MEMENTO_HEADER_DATES = "trellis.http.memento-header-dates";

    /** Configuration key defining whether to use weak ETags for RDF responses. */
    public static final String CONFIG_HTTP_WEAK_ETAG = "trellis.http.weak-etag";

    /** Configuration key defining the default JSON-LD profile. */
    public static final String CONFIG_HTTP_JSONLD_PROFILE = "trellis.http.jsonld-profile";

    /** Configuration key defining whether to require precondition headers for PUT operations. */
    public static final String CONFIG_HTTP_PRECONDITION_REQUIRED = "trellis.http.precondition-required";

    /** Configuration key defining whether PATCH requests can create resources. */
    public static final String CONFIG_HTTP_PATCH_CREATE = "trellis.http.patch-create";

    /** Configuration key defining whether PUT-on-create generates contained or uncontained resources. */
    public static final String CONFIG_HTTP_PUT_UNCONTAINED = "trellis.http.put-uncontained";

    /** Configuration key defining whether versions are created in the HTTP layer. */
    public static final String CONFIG_HTTP_VERSIONING = "trellis.http.versioning";

    /** The Trellis query parameter for extended features of a given resource. */
    public static final String EXT = "ext";

    /** The Memento link parameter to indicate the datetime of a Memento. */
    public static final String DATETIME = "datetime";

    /** The Trellis ext parameter value used for accessing the description of an LDP-NR. */
    public static final String DESCRIPTION = "description";

    /** The Memento link parameter indicating the beginning range of a TimeMap. */
    public static final String FROM = "from";

    /** The Memento link relation for mementos. */
    public static final String MEMENTO = "memento";

    /** The name of the HTTP response header used to indicate the date of a Memento resource. */
    public static final String MEMENTO_DATETIME = "Memento-Datetime";

    /** The Memento link relation for original resources. */
    public static final String ORIGINAL = "original";

    /** The name of the HTTP verb used to update resources. */
    public static final String PATCH = "PATCH";

    /** The name of the HTTP response code for a Precondition Required error. */
    public static final int PRECONDITION_REQUIRED = 428;

    /** The name of the HTTP request header used to influence what information is included in responses. */
    public static final String PREFER = "Prefer";

    /** The name of the HTTP response header used to indicate how the Prefer header was applied. */
    public static final String PREFERENCE_APPLIED = "Preference-Applied";

    /** The name of the HTTP request header used to make range requests. */
    public static final String RANGE = "Range";

    /** The name of the HTTP request header used to indicate the desired name for a new resource. */
    public static final String SLUG = "Slug";

    /** The Memento link relation for TimeGate resources. */
    public static final String TIMEGATE = "timegate";

    /** The Memento link relation for TimeMap resources. */
    public static final String TIMEMAP = "timemap";

    /** The Memento link parameter indicating the ending range of a TimeMap. */
    public static final String UNTIL = "until";

    private static final Set<IRI> DEFAULT_REPRESENTATION_ELEMENTS = new HashSet<>();

    static {
        addAll(DEFAULT_REPRESENTATION_ELEMENTS, PreferContainment, PreferMembership, PreferUserManaged,
                PreferServerManaged);
    }

    /** The implied or default set of IRIs used with a Prefer header. */
    public static final Set<IRI> DEFAULT_REPRESENTATION = unmodifiableSet(DEFAULT_REPRESENTATION_ELEMENTS);

    private HttpConstants() {
        // prevent instantiation
    }
}
