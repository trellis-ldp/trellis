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
package org.trellisldp.file;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.emptySortedSet;
import static java.util.Collections.unmodifiableSortedSet;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.Resource.SpecialResources.MISSING_RESOURCE;

import java.io.File;
import java.nio.file.Path;
import java.time.Instant;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.rdf.api.IRI;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;

/**
 * A file-based versioning system.
 */
public class FileMementoService implements MementoService {

    /** The configuration key controlling the base filesystem path for memento storage. **/
    public static final String CONFIG_FILE_MEMENTO_BASE_PATH = "trellis.file.memento.basepath";

    private static final Logger LOGGER = getLogger(FileMementoService.class);

    private final File directory;

    /**
     * Create a file-based memento service.
     */
    @Inject
    public FileMementoService() {
        this(ConfigProvider.getConfig().getValue(CONFIG_FILE_MEMENTO_BASE_PATH, String.class));
    }

    /**
     * Create a file-based memento service.
     * @param path the file path
     */
    public FileMementoService(final String path) {
        LOGGER.info("Storing Mementos as files at {}", path);
        this.directory = new File(path);
        init();
    }

    @Override
    public CompletionStage<Void> put(final Resource resource) {
        return put(resource, resource.getModified());
    }

    /**
     * Create a Memento from a resource at a particular time.
     * @param resource the resource
     * @param time the time to which the Memento corresponds
     * @return the completion stage representing that the operation has completed
     */
    public CompletionStage<Void> put(final Resource resource, final Instant time) {
        return runAsync(() -> {
            final File resourceDir = FileUtils.getResourceDirectory(directory, resource.getIdentifier());
            if (!resourceDir.exists()) {
                resourceDir.mkdirs();
            }
            FileUtils.writeMemento(resourceDir, resource, time.truncatedTo(SECONDS));
        });
    }

    @Override
    public CompletionStage<Resource> get(final IRI identifier, final Instant time) {
        return supplyAsync(() -> {
            final Instant mementoTime = time.truncatedTo(SECONDS);
            final File resourceDir = FileUtils.getResourceDirectory(directory, identifier);
            final File file = FileUtils.getNquadsFile(resourceDir, mementoTime);
            if (file.exists()) {
                return new FileResource(identifier, file);
            }
            final SortedSet<Instant> allMementos = listMementos(identifier);
            if (allMementos.isEmpty()) {
                return MISSING_RESOURCE;
            }
            final SortedSet<Instant> possible = allMementos.headSet(mementoTime);
            if (possible.isEmpty()) {
                // In this case, the requested Memento is earlier than the set of all existing Mementos.
                // Based on RFC 7089, Section 4.5.3 https://tools.ietf.org/html/rfc7089#section-4.5.3
                // the first extant memento should therefore be returned.
                return new FileResource(identifier, FileUtils.getNquadsFile(resourceDir, allMementos.first()));
            }
            return new FileResource(identifier, FileUtils.getNquadsFile(resourceDir, possible.last()));
        });
    }

    @Override
    public CompletionStage<SortedSet<Instant>> mementos(final IRI identifier) {
        return supplyAsync(() -> listMementos(identifier));
    }

    /**
     * Delete a memento at the given time.
     *
     * @param identifier the resource identifier
     * @param time the memento time
     * @return the next stage of completion
     */
    public CompletionStage<Void> delete(final IRI identifier, final Instant time) {
        return runAsync(() -> {
            final File resourceDir = FileUtils.getResourceDirectory(directory, identifier);
            final File file = FileUtils.getNquadsFile(resourceDir, time.truncatedTo(SECONDS));
            if (FileUtils.uncheckedDeleteIfExists(file.toPath())) {
                LOGGER.debug("Deleted Memento {} at {}", identifier, file);
            }
        });
    }

    private void init() {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private SortedSet<Instant> listMementos(final IRI identifier) {
        final File resourceDir = FileUtils.getResourceDirectory(directory, identifier);
        if (!resourceDir.exists()) {
            return emptySortedSet();
        }

        final SortedSet<Instant> instants = new TreeSet<>();
        try (final Stream<Path> files = FileUtils.uncheckedList(resourceDir.toPath())) {
            files.map(Path::toString).filter(path -> path.endsWith(".nq")).map(FilenameUtils::getBaseName)
                .map(Long::parseLong).map(Instant::ofEpochSecond).map(t -> t.truncatedTo(SECONDS))
                .forEach(instants::add);
        }

        return unmodifiableSortedSet(instants);
    }
}
