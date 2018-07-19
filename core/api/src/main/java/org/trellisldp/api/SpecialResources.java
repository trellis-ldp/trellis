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

import java.time.Instant;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;

/**
 * Singleton "special" resources.
 */
public enum SpecialResources implements Resource {

    MISSING_RESOURCE {
        @Override
        public IRI getIdentifier() {
            return null;
        }

        @Override
        public IRI getInteractionModel() {
            return null;
        }

        @Override
        public Instant getModified() {
            return Instant.EPOCH;
        }

        @Override
        public Stream<? extends Quad> stream() {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "A non-existent resource";
        }
    },

    DELETED_RESOURCE {
        @Override
        public IRI getIdentifier() {
            return null;
        }

        @Override
        public IRI getInteractionModel() {
            return null;
        }

        @Override
        public Instant getModified() {
            return Instant.EPOCH;
        }

        @Override
        public Stream<? extends Quad> stream() {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "A deleted resource";
        }
    }
}

