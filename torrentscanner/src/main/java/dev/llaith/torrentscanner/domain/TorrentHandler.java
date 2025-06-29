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

package dev.llaith.torrentscanner.domain;

import dev.llaith.dirscanner.core.DirScannerHandler;
import dev.llaith.dirscanner.core.ProgressReporter;
import dev.llaith.dirscanner.core.ScanRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Handler for processing torrent files during directory scanning.
 * Implements the core business logic for torrent file processing.
 */
public class TorrentHandler implements DirScannerHandler {
    
    private static final String SEARCH_GLOB = "*.torrent";
    private static final int BATCH_SIZE = 100;
    private static final Pattern PATTERN = Pattern.compile("\\.torrent$");
    
    private final TorrentParser torrentParser;
    private final CsvWriter csvWriter;
    private final List<TorrentResult> indexBatch = new ArrayList<>();
    
    public TorrentHandler() {
        this.torrentParser = new TorrentParser();
        this.csvWriter = new CsvWriter();
    }
    
    @Override
    public String searchGlob() {
        return SEARCH_GLOB;
    }
    
    @Override
    public Predicate<Path> filter() {
        return Files::isRegularFile;
    }
    
    @Override
    public void handleMatched(
            final ScanRequest request,
            final ProgressReporter progressReporter,
            final Path torrentFile) {
        
        try {
            final TorrentMetadata metadata = this.torrentParser.parse(torrentFile);
            
            if (request.isIndexMode()) {
                writeToIndex(torrentFile, metadata, request, progressReporter);
            } else {
                writeToIndividual(torrentFile, metadata, request, progressReporter);
            }
            
            progressReporter.reportProcessed(torrentFile, "scanned");
            
        } catch (final TorrentParser.TorrentParseException torrentParseException) {
            
            progressReporter.reportErrored(
                    torrentFile,
                    "Failed to process torrent file:",
                    torrentParseException);
            
        } catch (final IOException ioException) {
            
            progressReporter.reportErrored(
                    torrentFile,
                    "Failed to output csv:",
                    ioException);
        }
    }
    
    /**
     * Writes torrent metadata to individual CSV file.
     */
    private void writeToIndividual(
            final Path torrentFile,
            final TorrentMetadata metadata,
            final ScanRequest request,
            final ProgressReporter progressReporter) throws IOException {
        
        final Path outputPath = verifyPath(
                determineOutputPath(torrentFile, metadata, request),
                request,
                progressReporter);
        
        if (!request.isDryRun() && outputPath != null) {
            writeIndividualCsv(outputPath, metadata);
        }
    }
    
    /**
     * Adds torrent metadata to index batch.
     */
    private void writeToIndex(
            final Path torrentFile,
            final TorrentMetadata metadata,
            final ScanRequest request,
            final ProgressReporter progressReporter) throws IOException {
        
        final Path outputPath = verifyPath(
                request.getIndexFilePath(),
                request,
                progressReporter);
        
        if (!request.isDryRun() && outputPath != null) {
            
            this.indexBatch.add(new TorrentResult(
                    torrentFile,
                    metadata,
                    outputPath));
            
            if (this.indexBatch.size() >= BATCH_SIZE) {
                writeIndexCsv(
                        request.getIndexFilePath(),
                        request.shouldOverwrite(),
                        this.indexBatch);
                
                this.indexBatch.clear();
            }
        }
    }
    
    /**
     * Flushes any remaining items in the index batch.
     */
    public void flushIndexBatch(final ScanRequest request) throws IOException {
        if (request.isIndexMode() && !this.indexBatch.isEmpty()) {
            writeIndexCsv(
                    request.getIndexFilePath(),
                    request.shouldOverwrite(),
                    this.indexBatch);
            this.indexBatch.clear();
        }
    }
    
    /**
     * Verifies that output path is valid and doesn't conflict with existing files.
     */
    private Path verifyPath(
            final Path outputPath,
            final ScanRequest request,
            final ProgressReporter progressReporter) {
        
        if (Files.exists(outputPath) && !request.shouldOverwrite()) {
            progressReporter.reportSkipped(outputPath, "file exists (no-clobber)");
            return null;
        }
        
        return outputPath;
    }
    
    /**
     * Determines the output path for an individual CSV file.
     */
    private Path determineOutputPath(
            final Path torrentFile,
            final TorrentMetadata metadata,
            final ScanRequest request) {
        
        final Path outputDir = request.getOutputDirectory() != null
                ? request.getOutputDirectory()
                : torrentFile.getParent();
        
        final String torrentName = torrentFile.getFileName().toString();
        final String baseName = PATTERN.matcher(torrentName).replaceFirst("");
        final String csvName = baseName + "." + metadata.getInfoHash() + ".csv";
        
        return outputDir.resolve(csvName);
    }
    
    /**
     * Writes an individual CSV file for a single torrent.
     */
    private void writeIndividualCsv(
            final Path outputPath,
            final TorrentMetadata metadata) throws IOException {
        
        final List<List<String>> rows = new ArrayList<>();
        
        for (final TorrentMetadata.FileInfo file : metadata.getFiles()) {
            rows.add(List.of(
                    metadata.getInfoHash(),
                    file.getPath(),
                    String.valueOf(file.getSize())));
        }
        
        final List<String> headers = List.of("InfoHash", "Path", "Size");
        
        this.csvWriter.write(outputPath, headers, rows);
    }
    
    /**
     * Writes a consolidated index CSV file.
     */
    private void writeIndexCsv(
            final Path indexPath,
            final boolean overwrite,
            final List<TorrentResult> results) throws IOException {
        
        if (Files.exists(indexPath) && !overwrite) {
            this.indexBatch.clear();
            return;
        }
        
        final List<List<String>> rows = new ArrayList<>();
        
        for (final TorrentResult result : results) {
            final TorrentMetadata metadata = result.metadata();
            final String torrentFileName = result.sourcePath().getFileName().toString();
            
            for (final TorrentMetadata.FileInfo file : metadata.getFiles()) {
                rows.add(List.of(
                        metadata.getInfoHash(),
                        file.getPath(),
                        String.valueOf(file.getSize()),
                        torrentFileName));
            }
        }
        
        final List<String> headers = List.of("InfoHash", "Path", "Size", "TorrentFile");
        
        this.csvWriter.write(indexPath, headers, rows);
    }
}