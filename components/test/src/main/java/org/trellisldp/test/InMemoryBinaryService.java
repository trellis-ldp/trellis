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

package org.trellisldp.test;

import static java.util.Arrays.copyOfRange;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.apache.commons.io.IOUtils.toByteArray;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.rdf.api.IRI;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.BinaryService;

/**
 * A {@link BinaryService} that stores its contents in memory, for testing.
 */
public class InMemoryBinaryService implements BinaryService {

    private static final CompletableFuture<Void> DONE = completedFuture(null);

    private static final AtomicLong serviceCounter = new AtomicLong();

    private final long serviceNumber = serviceCounter.getAndIncrement();

    private final String ID_PREFIX = getClass().getSimpleName() + "-" + serviceNumber + ":";

    private final AtomicLong idCounter = new AtomicLong();

    private final Map<IRI, Binary> data = new ConcurrentHashMap<>();

    @Override
    public CompletionStage<? extends Binary> get(IRI identifier) {
        return completedFuture(data.get(identifier));
    }

    @Override
    public CompletionStage<Void> setContent(BinaryMetadata meta, InputStream bytes) {
        try {
            final InMemoryBinary binary = new InMemoryBinary(toByteArray(bytes));
            data.put(meta.getIdentifier(), binary);
            return DONE;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public CompletionStage<Void> purgeContent(IRI identifier) {
        data.remove(identifier);
        return DONE;
    }

    @Override
    public String generateIdentifier() {
        return ID_PREFIX + idCounter.getAndIncrement();
    }

    private static class InMemoryBinary implements Binary {

        private final byte[] data;

        private InMemoryBinary(byte[] data) {
            this.data = data;
        }

        @Override
        public CompletionStage<InputStream> getContent() {
            return completedFuture(new ByteArrayInputStream(data));
        }

        @Override
        public CompletionStage<InputStream> getContent(int from, int to) {
            final byte[] slice = copyOfRange(data, from, to);
            return completedFuture(new ByteArrayInputStream(slice));
        }
    }
}
