/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
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
package org.trellisldp.webac;

import static java.util.Collections.unmodifiableSet;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.rdf.api.IRI;

public class AuthorizedModes {

    private final Set<IRI> modes;
    private final IRI effectiveAcl;

    /**
     * Create an object with the effective ACL and the corresponding access modes.
     * @param effectiveAcl the effective ACL
     * @param modes the access modes
     */
    public AuthorizedModes(final IRI effectiveAcl, final Set<IRI> modes) {
        this.effectiveAcl = effectiveAcl;
        this.modes = unmodifiableSet(modes);
    }

    /**
     * Get the location of the effective ACL.
     * @return the location of the effective ACL
     */
    public Optional<IRI> getEffectiveAcl() {
        return Optional.ofNullable(effectiveAcl);
    }

    /**
     * Get the access modes in effect.
     * @return the set of ACL modes
     */
    public Set<IRI> getAccessModes() {
        return modes;
    }
}
