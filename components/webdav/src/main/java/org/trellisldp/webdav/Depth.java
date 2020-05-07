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
package org.trellisldp.webdav;

import static java.util.Arrays.asList;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * A Depth header.
 */
public class Depth {

    private final DEPTH depthValue;
    private static final Set<String> values = new HashSet<>(asList("infinity", "0", "1"));

    /** Legitimate values for the Depth header. */
    public enum DEPTH {
        ZERO, ONE, INFINITY
    }

    /**
     * Create a Depth object from a header value.
     * @param depth the depth value
     */
    public Depth(final String depth) {
        if ("infinity".equalsIgnoreCase(depth)) {
            this.depthValue = DEPTH.INFINITY;
        } else if ("1".equals(depth)) {
            this.depthValue = DEPTH.ONE;
        } else {
            this.depthValue = DEPTH.ZERO;
        }
    }

    /**
     * Get the depth value.
     * @return the depth
     */
    public DEPTH getDepth() {
        return depthValue;
    }

    /**
     * Create a Depth object from a value.
     * @param value the value
     * @return the Depth object or null
     */
    public static Depth valueOf(final String value) {
        if (value != null && values.contains(value.toLowerCase(Locale.ENGLISH))) {
            return new Depth(value);
        }
        return null;
    }
}
