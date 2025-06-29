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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Pure domain service for writing CSV files.
 * Single responsibility: format and write CSV data.
 * No logging, no console output, no business logic.
 */
public final class CsvWriter {

    private final String delimiter;
    private final String quote;
    private final String lineEnding;

    public CsvWriter() {
        this(",", "\"", System.lineSeparator());
    }

    public CsvWriter(
            final String delimiter,
            final String quote,
            final String lineEnding) {

        this.delimiter = delimiter;
        this.quote = quote;
        this.lineEnding = lineEnding;

    }

    /**
     * Writes CSV data to a file.
     *
     * @param outputPath the file to write to
     * @param headers    the column headers
     * @param rows       the data rows
     * @throws IOException if writing fails
     */
    public void write(
            final Path outputPath,
            final List<String> headers,
            final List<List<String>> rows) throws IOException {

        if (outputPath == null || headers == null || rows == null) {
            throw new IllegalArgumentException("Output path, headers, and rows must not be null");
        }

        if (outputPath.getParent() == null) {
            throw new IOException("Refusing to write to the root directory: " + outputPath);
        }

        Files.createDirectories(outputPath.getParent());

        try (final BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {

            writeRow(writer, headers);

            for (final List<String> row : rows)
                writeRow(writer, row);

        }

    }

    /**
     * Writes a single row to the writer.
     */
    private void writeRow(
            final BufferedWriter writer,
            final List<String> fields) throws IOException {

        for (int i = 0; i < fields.size(); i++) {

            if (i > 0) writer.write(this.delimiter);

            final String field = fields.get(i);

            if (requiresQuoting(field)) {
                writer.write(this.quote);
                writer.write(escapeField(field));
                writer.write(this.quote);

            } else writer.write(field);

        }

        writer.write(this.lineEnding);

    }

    /**
     * Checks if a field needs to be quoted.
     */
    private boolean requiresQuoting(final String field) {

        return field != null && (
                field.contains(this.delimiter) ||
                        field.contains(this.quote) ||
                        field.contains("\n") ||
                        field.contains("\r"));

    }

    /**
     * Escapes quotes in a field by doubling them.
     */
    private String escapeField(final String field) {

        return field == null
                ? ""
                : field.replace(this.quote, this.quote + this.quote);

    }
}
