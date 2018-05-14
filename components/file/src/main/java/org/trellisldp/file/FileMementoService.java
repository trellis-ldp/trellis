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

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.deleteIfExists;
import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.time.Instant.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static org.apache.commons.lang3.Range.between;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;

/**
 * A file-based versioning system.
 */
public class FileMementoService implements MementoService {

    public static final String MEMENTO_BASE_PATH = "trellis.file.memento.basepath";

    private static final Logger LOGGER = getLogger(FileMementoService.class);

    private final File directory;

    /**
     * Create a file-based memento service.
     */
    @Inject
    public FileMementoService() {
        this(ConfigurationProvider.getConfiguration().get(MEMENTO_BASE_PATH));
    }

    /**
     * Create a file-based memento service.
     * @param path the file path
     */
    public FileMementoService(final String path) {
        requireNonNull(path, "Memento base path is undefined!");
        this.directory = new File(path);
        init();
    }

    @Override
    public void put(final IRI identifier, final Instant time, final Stream<? extends Quad> data) {
        final File resourceDir = FileUtils.getResourceDirectory(directory, identifier);
        if (!resourceDir.exists()) {
            resourceDir.mkdirs();
        }

        try (final BufferedWriter writer = newBufferedWriter(getNquadsFile(resourceDir, time).toPath(), UTF_8, CREATE,
                    WRITE, TRUNCATE_EXISTING)) {
            final Iterator<String> lineIter = data.map(FileUtils::serializeQuad).iterator();
            while (lineIter.hasNext()) {
                writer.write(lineIter.next() + lineSeparator());
            }
        } catch (final IOException ex) {
            LOGGER.error("Error writing resource version for " + identifier.getIRIString(), ex);
        }
    }

    @Override
    public Optional<Resource> get(final IRI identifier, final Instant time) {
        final File resourceDir = FileUtils.getResourceDirectory(directory, identifier);
        final File file = getNquadsFile(resourceDir, time);
        if (file.exists()) {
            return of(new FileMementoResource(identifier, file));
        }
        return list(identifier).stream()
                .filter(range -> !range.getMinimum().isAfter(time))
                .max((t1, t2) -> t1.getMinimum().compareTo(t2.getMinimum()))
                .map(t -> getNquadsFile(resourceDir, t.getMinimum()))
                .map(f -> new FileMementoResource(identifier, f));
    }

    @Override
    public List<Range<Instant>> list(final IRI identifier) {
        final File resourceDir = FileUtils.getResourceDirectory(directory, identifier);
        if (!resourceDir.exists()) {
            return emptyList();
        }

        final List<Instant> instants = new ArrayList<>();
        try (final Stream<Path> files = Files.list(resourceDir.toPath())) {
            files.map(Path::toString).filter(path -> path.endsWith(".nq")).map(FilenameUtils::getBaseName)
                .map(Long::parseLong).map(Instant::ofEpochSecond).forEach(instants::add);
        } catch (final IOException ex) {
            LOGGER.error("Error fetching memento list for " + identifier, ex);
        }

        sort(instants);

        final List<Range<Instant>> versions = new ArrayList<>();
        Instant last = null;
        for (final Instant time : instants) {
            if (nonNull(last)) {
                versions.add(between(last, time));
            }
            last = time;
        }
        if (nonNull(last)) {
            versions.add(between(last, now()));
        }
        return unmodifiableList(versions);
    }

    @Override
    public Boolean delete(final IRI identifier, final Instant time) {
        try {
            return deleteIfExists(getNquadsFile(FileUtils.getResourceDirectory(directory, identifier), time).toPath());
        } catch (final IOException ex) {
            LOGGER.error("Could not delete Memento for " + identifier + " at " + time, ex);
        }
        return false;
    }

    private void init() {
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    private File getNquadsFile(final File dir, final Instant time) {
        return new File(dir, Long.toString(time.getEpochSecond()) + ".nq");
    }
}
