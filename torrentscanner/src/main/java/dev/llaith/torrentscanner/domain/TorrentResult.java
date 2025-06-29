package dev.llaith.torrentscanner.domain;

import java.nio.file.Path;

/**
 * Represents the result of processing a single torrent file.
 */
public record TorrentResult(Path sourcePath, TorrentMetadata metadata, Path outputPath) {}
