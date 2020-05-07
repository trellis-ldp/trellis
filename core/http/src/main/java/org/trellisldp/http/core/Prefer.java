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

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * A class representing an HTTP Prefer header.
 *
 * @author acoburn
 *
 * @see <a href="https://tools.ietf.org/html/rfc7240">RFC 7240</a> and
 * <a href="https://www.iana.org/assignments/http-parameters/http-parameters.xhtml#preferences">IANA values</a>
 */
public class Prefer {

    public static final String PREFER_REPRESENTATION = "representation";

    public static final String PREFER_MINIMAL = "minimal";

    public static final String PREFER_STRICT = "strict";

    public static final String PREFER_LENIENT = "lenient";

    public static final String PREFER_RETURN = "return";

    public static final String PREFER_INCLUDE = "include";

    public static final String PREFER_OMIT = "omit";

    public static final String PREFER_HANDLING = "handling";

    private static final String WS = " ";

    private final String preference;

    private final String handling;

    private final List<String> include;

    private final List<String> omit;

    private final Set<String> params;

    /**
     * Create a Prefer header representation.
     *
     * @param preference the preference value
     * @param include a list of include values
     * @param omit a list of omit values
     * @param params single-valued parameters
     * @param handling the handling value
     */
    public Prefer(final String preference, final List<String> include, final List<String> omit,
            final Set<String> params, final String handling) {
        this.preference = PREFER_MINIMAL.equals(preference) || PREFER_REPRESENTATION.equals(preference)
            ? preference : null;
        this.handling = PREFER_LENIENT.equals(handling) || PREFER_STRICT.equals(handling) ? handling : null;
        this.include = include != null ? unmodifiableList(include) : emptyList();
        this.omit = omit != null ? unmodifiableList(omit) : emptyList();
        this.params = params != null ? unmodifiableSet(params) : emptySet();
    }

    /**
     * Create a Prefer header representation from a header string.
     *
     * @param value the header value
     * @return a Prefer object or null on an invalid string
     */
    public static Prefer valueOf(final String value) {
        if (value != null) {
            final Map<String, String> data = new HashMap<>();
            final Set<String> params = new HashSet<>();
            stream(value.split(";")).map(String::trim).map(pref -> pref.split("=", 2)).forEach(x -> {
                if (x.length == 2) {
                    data.put(x[0].trim(), x[1].trim());
                } else {
                    params.add(x[0].trim());
                }
            });
            return new Prefer(data.get(PREFER_RETURN), parseParameter(data.get(PREFER_INCLUDE)),
                        parseParameter(data.get(PREFER_OMIT)), params, data.get(PREFER_HANDLING));
        }
        return null;
    }

    /**
     * Get the preferred return type.
     *
     * @return the preferred return type
     */
    public Optional<String> getPreference() {
        return ofNullable(preference);
    }

    /**
     * Get the handling type.
     *
     * @return the preferred handling type
     */
    public Optional<String> getHandling() {
        return ofNullable(handling);
    }

    /**
     * Identify whether the respond-async parameter was set.
     *
     * @return true if the respond-async parameter was set; false otherwise
     */
    public boolean getRespondAsync() {
        return params.contains("respond-async");
    }

    /**
     * Get the preferred include IRIs.
     *
     * @return the list of IRIs to be included in the representation
     */
    public List<String> getInclude() {
        return include;
    }

    /**
     * Get the preferred omit IRIs.
     *
     * @return the list of IRIs to be omitted from the representation
     */
    public List<String> getOmit() {
        return omit;
    }

    @Override
    public String toString() {
        final String includeParam = include.isEmpty() ? "" : "include=\"" + join(WS, include) + "\";";
        final String omitParam = omit.isEmpty() ? "" : "omit=\"" + join(WS, omit) + "\";";
        return getPreference().map(pref -> "return=" + pref + ";").orElse("") + includeParam + omitParam +
            getHandling().map(pref -> "handling=" + pref + ";").orElse("") +
            (getRespondAsync() ? "respond-async" : "");
    }

    /**
     * Build a Prefer object with a set of included IRIs.
     *
     * @param includes the IRIs to include
     * @return the Prefer object
     */
    public static Prefer ofInclude(final String... includes) {
        final List<String> iris = asList(includes);
        if (iris.isEmpty()) {
            return valueOf(join("=", PREFER_RETURN, PREFER_REPRESENTATION));
        }
        return valueOf(join("=", PREFER_RETURN, PREFER_REPRESENTATION) + "; " + PREFER_INCLUDE + "=\"" +
                join(WS, iris) + "\"");
    }

    /**
     * Build a Prefer object with a set of omitted IRIs.
     *
     * @param omits the IRIs to omit
     * @return the Prefer object
     */
    public static Prefer ofOmit(final String... omits) {
        final List<String> iris = asList(omits);
        if (iris.isEmpty()) {
            return valueOf(join("=", PREFER_RETURN, PREFER_REPRESENTATION));
        }
        return valueOf(join("=", PREFER_RETURN, PREFER_REPRESENTATION) + "; " + PREFER_OMIT + "=\"" +
                join(WS, iris) + "\"");
    }

    private static List<String> parseParameter(final String param) {
        if (param != null) {
            return asList(trimQuotes(param).split("\\s+"));
        }
        return emptyList();
    }

    private static String trimQuotes(final String param) {
        if (param.startsWith("\"") && param.endsWith("\"") && param.length() > 1) {
            return param.substring(1, param.length() - 1);
        }
        return param;
    }
}
