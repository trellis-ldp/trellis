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
package org.trellisldp.http.impl;

import static java.util.Optional.ofNullable;

import java.util.Optional;

class PersistenceResult {

    private final Boolean success;
    private final Long size;

    public PersistenceResult(final Boolean success) {
        this(success, null);
    }

    public PersistenceResult(final Boolean success, final Long size) {
        this.success = success;
        this.size = size;
    }

    /**
     * Get the size of the persisted resource.
     * @return the size of the persisted resource
     */
    public Optional<Long> getSize() {
        return ofNullable(size);
    }

    /**
     * Get whether the persistence operation was successful.
     * @return true if the persistence operation was successful
     */
    public Boolean isSuccess() {
        return success;
    }
}
