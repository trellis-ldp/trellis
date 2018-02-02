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

package org.trellisldp.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.IRI;

/**
 * For use when audit functionality is not desired.
 * 
 * @author ajs6f
 *
 */
public final class NoopAuditService implements AuditService {
    /*
     * No audit info will ever be generated, so return true because all audit info
     * indeed has been persisted.
     * 
     * @see org.trellisldp.api.AppendService#add(java.lang.Object, java.lang.Object)
     */
    @Override
    public Future<Boolean> add(final IRI identifier, final Dataset resource) {
        return CompletableFuture.completedFuture(true);
    }
}