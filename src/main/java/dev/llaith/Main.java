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

package dev.llaith;

import dev.llaith.application.cli.DirScannerCli;
import picocli.CommandLine;

/**
 * Main entry point for the torrent scanner application.
 * Single responsibility: bootstrap the CLI and exit with appropriate code.
 */
public final class Main {
    
    public static void main(final String[] args) {
        final int exitCode = new CommandLine(new DirScannerCli()).execute(args);
        System.exit(exitCode);
    }
}
