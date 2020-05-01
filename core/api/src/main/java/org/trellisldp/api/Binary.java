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
package org.trellisldp.api;

import java.io.InputStream;

/**
 * The non-RDF content of an LDP NonRDFSource.
 *
 */
public interface Binary {

    /**
     * @return the content of this {@link Binary}
     */
    InputStream getContent();

    /**
     * @param from the point in bytes from which to begin content
     * @param to the point in bytes at which to end content
     * @return content from {@code from} to {@code to} inclusive
     */
    InputStream getContent(int from, int to);

}
