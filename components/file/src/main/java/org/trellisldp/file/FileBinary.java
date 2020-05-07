/*
 * Copyright (c) 2020 Aaron Coburn and individual contributors
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
package org.trellisldp.file;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;

import org.trellisldp.api.Binary;

/**
 * Implements {@link Binary} for files on a filesystem.
 *
 */
public class FileBinary implements Binary {

    private final File file;

    /**
     * @param file the file to wrap as a {@link Binary}
     */
    public FileBinary(final File file) {
        this.file = file;
    }

    @SuppressWarnings("resource")
    @Override
    public InputStream getContent() {
        try {
            return Files.newInputStream(file.toPath());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public InputStream getContent(final int from, final int to) {
        try {
            return FileUtils.getBoundedStream(Files.newInputStream(file.toPath()), from, to);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
