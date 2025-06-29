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

package dev.llaith.dirscanner.core;

/**
 * Interface for reporting the overall status of scanning operations.
 * Handles start, completion, and failure reporting at the scan level.
 */
public interface ScanReporter {
    
    /**
     * Reports the start of a scanning operation.
     * 
     * @param request the scan request being executed
     */
    void reportStart(ScanRequest request);
    
    /**
     * Reports a failure that prevented the scan from completing.
     * 
     * @param throwable the exception that caused the failure
     */
    void reportFailure(Throwable throwable);
    
    /**
     * Reports the completion of a scanning operation and returns exit code.
     * 
     * @return exit code (0 for success, non-zero for errors)
     */
    int reportComplete();
}