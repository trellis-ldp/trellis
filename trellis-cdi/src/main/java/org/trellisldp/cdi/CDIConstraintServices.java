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
package org.trellisldp.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Iterator;

import org.trellisldp.api.ConstraintService;
import org.trellisldp.app.ConstraintServices;

/**
 * A CDI-based implementation of the {@link ConstraintServices} interface, suitable
 * for any CDI or MicroProfile-based application.
 */
@ApplicationScoped
public class CDIConstraintServices implements ConstraintServices {

    @Inject
    protected Instance<ConstraintService> constraintServices;

    @Override
    public Iterator<ConstraintService> iterator() {
        return constraintServices.iterator();
    }
}
