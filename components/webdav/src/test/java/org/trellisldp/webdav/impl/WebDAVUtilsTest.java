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
package org.trellisldp.webdav.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import org.junit.jupiter.api.Test;

/**
 * @author acoburn
 */
public class WebDAVUtilsTest {

    @Test
    public void testPathLastSegments() {
        assertEquals("", WebDAVUtils.getLastSegment(emptyList()));
        assertEquals("foo", WebDAVUtils.getLastSegment(singletonList(asPathSegment("foo"))));
        assertEquals("bar", WebDAVUtils.getLastSegment(asList(asPathSegment("foo"), asPathSegment("bar"))));
        assertEquals("baz", WebDAVUtils.getLastSegment(asList(asPathSegment("foo"), asPathSegment("bar"),
                        asPathSegment("baz"))));
    }

    @Test
    public void testEmptyPathSegments() {
        assertEquals("", WebDAVUtils.getAllButLastSegment(emptyList()));
        assertEquals("", WebDAVUtils.getAllButLastSegment(singletonList(asPathSegment("foo"))));
        assertEquals("foo", WebDAVUtils.getAllButLastSegment(asList(asPathSegment("foo"), asPathSegment("bar"))));
        assertEquals("foo/bar", WebDAVUtils.getAllButLastSegment(asList(asPathSegment("foo"), asPathSegment("bar"),
                        asPathSegment("baz"))));
    }

    private PathSegment asPathSegment(final String segment) {
        return new PathSegment() {
            @Override
            public String getPath() {
                return segment;
            }
            @Override
            public MultivaluedMap<String, String> getMatrixParameters() {
                return new MultivaluedHashMap<>();
            }
        };
    }

}
