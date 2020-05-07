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
package org.trellisldp.webdav.impl;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import java.io.IOException;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.RDFFactory;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * @author acoburn
 */
class WebDAVUtilsTest {

    private static final RDF rdf = RDFFactory.getInstance();

    @Test
    void testPathLastSegments() {
        assertEquals("", WebDAVUtils.getLastSegment(emptyList()));
        assertEquals("foo", WebDAVUtils.getLastSegment(singletonList(asPathSegment("foo"))));
        assertEquals("bar", WebDAVUtils.getLastSegment(asList(asPathSegment("foo"), asPathSegment("bar"))));
        assertEquals("baz", WebDAVUtils.getLastSegment(asList(asPathSegment("foo"), asPathSegment("bar"),
                        asPathSegment("baz"))));
    }

    @Test
    void testEmptyPathSegments() {
        assertEquals("", WebDAVUtils.getAllButLastSegment(emptyList()));
        assertEquals("", WebDAVUtils.getAllButLastSegment(singletonList(asPathSegment("foo"))));
        assertEquals("foo", WebDAVUtils.getAllButLastSegment(asList(asPathSegment("foo"), asPathSegment("bar"))));
        assertEquals("foo/bar", WebDAVUtils.getAllButLastSegment(asList(asPathSegment("foo"), asPathSegment("bar"),
                        asPathSegment("baz"))));
    }

    @Test
    void testBaseUrl() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "resource");
        assertEquals("https://example.com/resource", WebDAVUtils.externalUrl(identifier, "https://example.com"));
        assertEquals("https://example.com/resource", WebDAVUtils.externalUrl(identifier, "https://example.com/"));
    }

    @Test
    void testCloseDataset() throws Exception {
        final Dataset mockDataset = mock(Dataset.class);
        doThrow(new IOException()).when(mockDataset).close();
        assertThrows(RuntimeTrellisException.class, () -> WebDAVUtils.closeDataset(mockDataset));
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
