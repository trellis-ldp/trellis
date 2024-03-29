/*
 * Copyright (c) Aaron Coburn and individual contributors
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
package org.trellisldp.app;

import java.util.Collection;
import java.util.Iterator;

import org.trellisldp.api.ConstraintService;

/**
 * A default ConstraintServices implementation, using a backing {@link Collection}.
 */
public class DefaultConstraintServices implements ConstraintServices {

    private final Collection<ConstraintService> services;

    /**
     * Create a ConstraintServices object.
     * @param services the constraint services.
     */
    public DefaultConstraintServices(final Collection<ConstraintService> services) {
        this.services = services;
    }

    @Override
    public Iterator<ConstraintService> iterator() {
        return services.iterator();
    }
}

