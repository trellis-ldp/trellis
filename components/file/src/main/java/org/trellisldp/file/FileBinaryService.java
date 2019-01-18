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
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.ServiceLoader.load;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toSet;
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
import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import javax.inject.Inject;

import org.apache.commons.rdf.api.IRI;
import org.apache.tamaya.Configuration;
import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.DefaultIdentifierService;
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
    private static final int DEFAULT_HIERARCHY = 3;
    private static final int DEFAULT_LENGTH = 2;

    private static final Set<String> algorithms = asList(MD5, MD2, SHA, SHA_1, SHA_256, SHA_384, SHA_512,
            SHA3_256, SHA3_384, SHA3_512).stream()
        .collect(toSet());

    private final String basePath;
    private final Supplier<String> idSupplier;

    /**
     * Create a File-based Binary service.
     */
    @Inject
    public FileBinaryService() {
        this(of(load(IdentifierService.class)).map(ServiceLoader::iterator).filter(Iterator::hasNext)
                .map(Iterator::next).orElseGet(DefaultIdentifierService::new));
    }

    /**
     * Create a File-based Binary service.
     *
     * @param idService an identifier service
     */
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
            final int hierarchy, final int length) {
        this.basePath = requireNonNull(basePath, CONFIG_FILE_BINARY_BASE_PATH + " configuration may not be null!");
        LOGGER.info("Storing binaries as files at {}", basePath);
        this.idSupplier = idService.getSupplier("file:///", hierarchy, length);
    }

    private FileBinaryService(final IdentifierService idService, final Configuration config) {
        this(idService, config.get(CONFIG_FILE_BINARY_BASE_PATH),
                config.getOrDefault(CONFIG_FILE_BINARY_HIERARCHY, Integer.class, DEFAULT_HIERARCHY),
                config.getOrDefault(CONFIG_FILE_BINARY_LENGTH, Integer.class, DEFAULT_LENGTH));
    }

    @Override
    public CompletionStage<Binary> get(final IRI identifier) {
        return supplyAsync(() -> new FileBinary(getFileFromIdentifier(identifier)));
    }

    @Override
    public CompletionStage<Void> purgeContent(final IRI identifier) {
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
    public CompletionStage<Void> setContent(final BinaryMetadata metadata, final InputStream stream) {
        requireNonNull(stream, "InputStream may not be null!");
        return supplyAsync(() -> {
            final File file = getFileFromIdentifier(metadata.getIdentifier());
            LOGGER.debug("Setting binary content for {} at {}", metadata.getIdentifier(), file.getAbsolutePath());
            try (final InputStream input = stream) {
                final File parent = file.getParentFile();
                parent.mkdirs();
                copy(stream, file.toPath(), REPLACE_EXISTING);
            } catch (final IOException ex) {
                throw new UncheckedIOException("Error while setting content for " + metadata.getIdentifier(), ex);
            }
            return null;
        });
    }

    @Override
    public CompletionStage<MessageDigest> calculateDigest(final IRI identifier, final MessageDigest algorithm) {
        return supplyAsync(() -> computeDigest(identifier, algorithm));
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
            .map(URI::getSchemeSpecificPart).map(x -> trimStart(x, "/")).map(x -> new File(basePath, x))
            .orElseThrow(() -> new IllegalArgumentException("Could not create File object from IRI: " + identifier));
    }

    private MessageDigest computeDigest(final IRI identifier, final MessageDigest algorithm) {
        try (final InputStream input = new FileInputStream(getFileFromIdentifier(identifier))) {
            return updateDigest(algorithm, input);
        } catch (final IOException ex) {
            throw new UncheckedIOException("Error computing digest", ex);
        }
    }

    private static String trimStart(final String str, final String trim) {
        if (str.startsWith(trim)) {
            return trimStart(str.substring(trim.length()), trim);
        }
        return str;
    }

}
