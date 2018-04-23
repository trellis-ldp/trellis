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

import static java.nio.file.Files.copy;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.Base64.getEncoder;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD2;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_1;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_384;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_512;
import static org.apache.commons.collections4.IteratorUtils.asEnumeration;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IdentifierService;

/**
 * A {@link BinaryService} implementation that stores LDP-NR resources as files on a local filesystem.
 *
 * <p>This service supports the following digest algorithms:
 * <ul>
 * <li>MD5</li>
 * <li>MD2</li>
 * <li>SHA</li>
 * <li>SHA-1</li>
 * <li>SHA-256</li>
 * <li>SHA-384</li>
 * <li>SHA-512</li>
 * </ul>
 */
public class FileBinaryService implements BinaryService {

    public static final String BINARY_BASE_PATH = "trellis.file.binary.basepath";
    public static final String BINARY_HIERARCHY = "trellis.file.binary.hierarchy";
    public static final String BINARY_LENGTH = "trellis.file.binary.length";

    private static final Logger LOGGER = getLogger(FileBinaryService.class);
    private static final String SHA = "SHA";
    private static final Integer DEFAULT_HIERARCHY = 3;
    private static final Integer DEFAULT_LENGTH = 2;

    // TODO JDK9 supports SHA3 algorithms (SHA3_256, SHA3_384, SHA3_512)
    private static final Set<String> algorithms = asList(MD5, MD2, SHA, SHA_1, SHA_256, SHA_384, SHA_512).stream()
        .collect(toSet());

    private final String basePath;
    private final Supplier<String> idSupplier;

    /**
     * Create a File-based Binary service.
     *
     * @param idService an identifier service
     */
    @Inject
    public FileBinaryService(final IdentifierService idService) {
        this(idService, ConfigurationProvider.getConfiguration());
    }

    /**
     * Create a File-based Binary service.
     *
     * @param idService an identifier service
     * @param basePath the base file path
     * @param hierarchy the levels of hierarchy
     * @param length the length of each level of hierarchy
     */
    public FileBinaryService(final IdentifierService idService, final String basePath,
            final Integer hierarchy, final Integer length) {
        requireNonNull(basePath, BINARY_BASE_PATH + " configuration may not be null!");
        this.basePath = basePath;
        this.idSupplier = idService.getSupplier("file:", hierarchy, length);
    }

    private FileBinaryService(final IdentifierService idService, final Configuration config) {
        this(idService, config.get(BINARY_BASE_PATH),
                config.getOrDefault(BINARY_HIERARCHY, Integer.class, DEFAULT_HIERARCHY),
                config.getOrDefault(BINARY_LENGTH, Integer.class, DEFAULT_LENGTH));
    }

    @Override
    public Boolean exists(final IRI identifier) {
         return getFileFromIdentifier(identifier).filter(File::isFile).isPresent();
    }

    @Override
    public Optional<InputStream> getContent(final IRI identifier, final List<Range<Integer>> ranges) {
        requireNonNull(ranges, "Byte ranges may not be null");
        return getFileFromIdentifier(identifier).map(file -> {
            try {
                if (ranges.isEmpty()) {
                    return new FileInputStream(file);
                } else {
                    final List<InputStream> iss = new ArrayList<>();
                    for (final Range<Integer> r : ranges) {
                        final InputStream input = new FileInputStream(file);
                        final long skipped = input.skip(r.getMinimum());
                        LOGGER.debug("Skipped {} bytes", skipped);
                        iss.add(new BoundedInputStream(input, r.getMaximum() - r.getMinimum()));
                    }
                    return new SequenceInputStream(asEnumeration(iss.iterator()));
                }
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    @Override
    public void purgeContent(final IRI identifier) {
        getFileFromIdentifier(identifier).ifPresent(File::delete);
    }

    @Override
    public void setContent(final IRI identifier, final InputStream stream,
            final Map<String, String> metadata) {
        requireNonNull(stream, "InputStream may not be null!");
        getFileFromIdentifier(identifier).ifPresent(file -> {
            LOGGER.debug("Setting binary content for {} at {}", identifier.getIRIString(), file.getAbsolutePath());
            try {
                final File parent = file.getParentFile();
                parent.mkdirs();
                copy(stream, file.toPath(), REPLACE_EXISTING);
                stream.close();
            } catch (final IOException ex) {
                LOGGER.error("Error while setting content: {}", ex.getMessage());
                throw new UncheckedIOException(ex);
            }
        });
    }

    @Override
    public Optional<String> digest(final String algorithm, final InputStream stream) {
        if (SHA.equals(algorithm)) {
            return of(SHA_1).map(DigestUtils::getDigest).flatMap(digest(stream));
        }
        return ofNullable(algorithm).filter(supportedAlgorithms()::contains).map(DigestUtils::getDigest)
            .flatMap(digest(stream));
    }

    @Override
    public Set<String> supportedAlgorithms() {
        return algorithms;
    }

    @Override
    public String generateIdentifier() {
        return idSupplier.get();
    }

    private Optional<File> getFileFromIdentifier(final IRI identifier) {
        return ofNullable(identifier).map(IRI::getIRIString).map(URI::create).map(URI::getSchemeSpecificPart)
            .map(x -> new File(basePath, x));
    }

    private Function<MessageDigest, Optional<String>> digest(final InputStream stream) {
        return algorithm -> {
            try {
                final String digest = getEncoder().encodeToString(DigestUtils.updateDigest(algorithm, stream).digest());
                stream.close();
                return of(digest);
            } catch (final IOException ex) {
                LOGGER.error("Error computing digest", ex);
            }
            return empty();
        };
    }
}
