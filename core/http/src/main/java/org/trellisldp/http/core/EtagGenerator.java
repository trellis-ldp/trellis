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
package org.trellisldp.http.core;

import org.trellisldp.api.Resource;

public interface EtagGenerator {

    /**
     * Generate the value portion for an entity tag for the given resource.
     *
     * @implSpec The default implementation of this method generates an ETag value based on
     *           the precision of {@link Resource#getModified} implementation, up to and
     *           including nano-second precision.
     * @param resource the Trellis resource
     * @return the value for the entity tag
     */
    String getValue(Resource resource);
}
