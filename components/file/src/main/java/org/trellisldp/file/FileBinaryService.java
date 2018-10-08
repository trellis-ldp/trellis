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
import static java.nio.file.Files.delete;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;
import static java.util.Base64.getEncoder;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.getDigest;
import static org.apache.commons.codec.digest.DigestUtils.updateDigest;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD2;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA3_256;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA3_384;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA3_512;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_1;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_384;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_512;
import static org.apache.commons.lang3.StringUtils.stripStart;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.io.input.BoundedInputStream;
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
 *
 * <p>When running under JDK 9+, the following additional digest algorithms are supported:
 * <ul>
 * <li>SHA3-256</li>
 * <li>SHA3-384</li>
 * <li>SHA3-512</li>
 * </ul>
 */
public class FileBinaryService implements BinaryService {

    /** The configuration key controlling the base filesystem path for the binary service. */
    public static final String CONFIG_FILE_BINARY_BASE_PATH = "trellis.file.binary.basepath";

    /** The configuration key controlling the levels of hierarchy in a binary storage layout. */
    public static final String CONFIG_FILE_BINARY_HIERARCHY = "trellis.file.binary.hierarchy";

    /** The configuration key controlling the length of each level of hierarchy in a filesystem layout. */
    public static final String CONFIG_FILE_BINARY_LENGTH = "trellis.file.binary.length";

    private static final Logger LOGGER = getLogger(FileBinaryService.class);
    private static final String SHA = "SHA";
    private static final Integer DEFAULT_HIERARCHY = 3;
    private static final Integer DEFAULT_LENGTH = 2;

    private static final Set<String> algorithms = asList(MD5, MD2, SHA, SHA_1, SHA_256, SHA_384, SHA_512,
            SHA3_256, SHA3_384, SHA3_512).stream()
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
        requireNonNull(basePath, CONFIG_FILE_BINARY_BASE_PATH + " configuration may not be null!");
        this.basePath = basePath;
        this.idSupplier = idService.getSupplier("file:///", hierarchy, length);
    }

    private FileBinaryService(final IdentifierService idService, final Configuration config) {
        this(idService, config.get(CONFIG_FILE_BINARY_BASE_PATH),
                config.getOrDefault(CONFIG_FILE_BINARY_HIERARCHY, Integer.class, DEFAULT_HIERARCHY),
                config.getOrDefault(CONFIG_FILE_BINARY_LENGTH, Integer.class, DEFAULT_LENGTH));
    }

    @Override
    public CompletableFuture<InputStream> getContent(final IRI identifier) {
        return supplyAsync(() -> {
            try {
                return new FileInputStream(getFileFromIdentifier(identifier));
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    @Override
    public CompletableFuture<InputStream> getContent(final IRI identifier, final Integer from, final Integer to) {
        requireNonNull(from, "From value cannot be null!");
        requireNonNull(to, "To value cannot be null!");
        return supplyAsync(() -> {
            try {
                final InputStream input = new FileInputStream(getFileFromIdentifier(identifier));
                final long skipped = input.skip(from);
                LOGGER.debug("Skipped {} bytes", skipped);
                return new BoundedInputStream(input, to - from);
            } catch (final IOException ex) {
                throw new UncheckedIOException(ex);
            }
        });
    }

    @Override
    public CompletableFuture<Void> purgeContent(final IRI identifier) {
        return supplyAsync(() -> {
            try {
                delete(getFileFromIdentifier(identifier).toPath());
            } catch (final IOException ex) {
                LOGGER.warn("File could not deleted {}: {}", identifier, ex.getMessage());
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> setContent(final IRI identifier, final InputStream stream,
            final Map<String, String> metadata) {
        requireNonNull(stream, "InputStream may not be null!");
        return supplyAsync(() -> {
            final File file = getFileFromIdentifier(identifier);
            LOGGER.debug("Setting binary content for {} at {}", identifier.getIRIString(), file.getAbsolutePath());
            try (final InputStream input = stream) {
                final File parent = file.getParentFile();
                parent.mkdirs();
                copy(stream, file.toPath(), REPLACE_EXISTING);
            } catch (final IOException ex) {
                LOGGER.error("Error while setting content: {}", ex.getMessage());
                LOGGER.error("Error setting content", ex);
                throw new UncheckedIOException(ex);
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<String> calculateDigest(final IRI identifier, final String algorithm) {
        return supplyAsync(() -> {
            if (SHA.equals(algorithm)) {
                return computeDigest(identifier, getDigest(SHA_1));
            } else if (supportedAlgorithms().contains(algorithm)) {
                return computeDigest(identifier, getDigest(algorithm));
            }
            LOGGER.warn("Algorithm not supported: {}", algorithm);
            return null;
        });
    }

    @Override
    public Set<String> supportedAlgorithms() {
        return algorithms;
    }

    @Override
    public String generateIdentifier() {
        return idSupplier.get();
    }

    private File getFileFromIdentifier(final IRI identifier) {
        requireNonNull(identifier, "Identifier may not be null!");
        return of(identifier).map(IRI::getIRIString).filter(x -> x.startsWith("file:")).map(URI::create)
            .map(URI::getSchemeSpecificPart).map(x -> stripStart(x, "/")).map(x -> new File(basePath, x))
            .orElseThrow(() -> new IllegalArgumentException("Could not create File object from IRI: " + identifier));
    }

    private String computeDigest(final IRI identifier, final MessageDigest algorithm) {
        try (final InputStream input = new FileInputStream(getFileFromIdentifier(identifier))) {
            return getEncoder().encodeToString(updateDigest(algorithm, input).digest());
        } catch (final IOException ex) {
            LOGGER.error("Error computing digest", ex);
            throw new UncheckedIOException(ex);
        }
    }
}
