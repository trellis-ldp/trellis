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

import java.util.Collections;
import java.util.Map;

/**
 * A {@link NamespaceService} that stores nothing and offers nothing.
 *
 * @author ajs6f
 *
 */
@NoopImplementation
public class NoopNamespaceService implements NamespaceService {

    @Override
    public Map<String, String> getNamespaces() {
        return Collections.emptyMap();
    }

    @Override
    public boolean setPrefix(final String prefix, final String namespace) {
        return true;
    }
}
