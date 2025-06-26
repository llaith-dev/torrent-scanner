/*
 * Copyright 2025 Nos Doughty.
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

package dev.llaith.domain;

import bt.metainfo.MetadataService;
import bt.metainfo.Torrent;
import bt.metainfo.TorrentFile;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Pure domain service for parsing torrent files.
 * Single responsibility: convert .torrent files to TorrentMetadata.
 * No logging, no console output, no file system dependencies beyond reading the torrent.
 */
public final class TorrentParser {

    private final MetadataService metadataService;

    public TorrentParser() {
        this.metadataService = new MetadataService();
    }

    /**
     * Parses a torrent file and extracts metadata.
     *
     * @param torrentPath path to the .torrent file
     * @return torrent metadata
     * @throws TorrentParseException if parsing fails
     */
    public TorrentMetadata parse(final Path torrentPath) throws TorrentParseException {

        try (final java.io.InputStream is = new java.io.FileInputStream(torrentPath.toFile())) {

            final Torrent torrent = this.metadataService.fromInputStream(is);


            final List<TorrentMetadata.FileInfo> files = new ArrayList<>();

            long totalSize = 0;

            for (final TorrentFile torrentFile : torrent.getFiles()) {

                final String filePath = String.join("/", torrentFile.getPathElements());

                final long fileSize = torrentFile.getSize();

                files.add(new TorrentMetadata.FileInfo(filePath, fileSize));

                totalSize += fileSize;

            }

            final String infoHash = torrent.getTorrentId().toString();

            final String name = torrent.getName();

            return new TorrentMetadata(infoHash, name, totalSize, files);

        } catch (final IOException ioException) {

            throw new TorrentParseException(
                    format("Failed to parse torrent file: %s", torrentPath),
                    ioException);

        }

    }

    /**
     * Exception thrown when torrent parsing fails.
     */
    public static final class TorrentParseException extends Exception {

        @Serial private static final long serialVersionUID = -5732038831914494314L;

        public TorrentParseException(final String message, final Throwable cause) {
            super(message, cause);
        }
        
    }
}
