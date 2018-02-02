package org.trellisldp.audit.test;

import java.util.concurrent.CompletableFuture;
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

import java.util.concurrent.Future;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;
import org.trellisldp.audit.DefaultAuditService;

/**
 * For testing purposes only.
 * 
 * @author ajs6f
 *
 */
public class TestAuditService extends DefaultAuditService {

    @Override
    public Future<Boolean> add(final IRI id, final Dataset quads) {
        return CompletableFuture.completedFuture(true);
    }

}
