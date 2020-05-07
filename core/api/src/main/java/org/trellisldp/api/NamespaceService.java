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

import java.util.Map;

/**
 * Namespaces may be stored globally across the server, and the NamespaceService
 * provides a mechanism for retrieving and setting namespace values.
 *
 * @author acoburn
 */
public interface NamespaceService {

    /**
     * Fetch the entire namespace mapping.
     *
     * @return the namespace mapping as prefix, namespace pairs
     */
    Map<String, String> getNamespaces();

    /**
     * Set the namespace for a given prefix.
     *
     * @param prefix the prefix
     * @param namespace the namespace
     * @return whether the new prefix was set
     */
    boolean setPrefix(String prefix, String namespace);
}
