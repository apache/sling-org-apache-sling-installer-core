/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.api.provider;

import java.util.Set;
import java.util.regex.Pattern;

public final class RunModeSupport {

    private static final String OR_SEPARATOR = ",";
    private static final String AND_SEPARATOR = ".";
    private static final String NOT_PREFIX = "-";

    private RunModeSupport() {
        // not supposed to be instantiated
    }

    /**
     * Checks if a given run mode string is satisfied by the given active run modes.
     * A run mode string consists out of run modes and operators (AND = ".", OR = "," and NOT = "-")
     * The run mode string follows the following grammar in EBNF:
     * <pre><code>
     * run mode string ::= disjunctions
     * disjunctions ::= conjunctions { "," conjunctions }
     * conjunctions ::= conjunction { '.' conjunction }
     * conjunction ::= notrunmode | runmode
     * notrunmode ::= '-' runmode
     * </code></pre>
     * 
     * The operator order is first "-" (not), second "." (AND), last "," (OR).
     * @param disjunctions the expected run mode string to check
     * @param activeRunModes the run modes against which to check
     * @return the number of matching run modes or 0 if no match. If multiple disjunctions match the one with the highest number of matching run modes is returned.
     */
    public static int getNumberOfMatchingRunmodesFromDisjunctions(String disjunctions, Set<String> activeRunModes) {
        int numMatchingRunModes = 0;
        // 1. support OR
        for (String discjunctivePart : disjunctions.split(Pattern.quote(OR_SEPARATOR))) {
            int newNumMatchingRunModes = getNumberOfMatchingRunModesFromConjunctions(discjunctivePart, activeRunModes);
            if (newNumMatchingRunModes > numMatchingRunModes) {
                numMatchingRunModes = newNumMatchingRunModes;
            }
        }
        return numMatchingRunModes;
    }

    private static int getNumberOfMatchingRunModesFromConjunctions(String conjunctions, Set<String> activeRunModes) {
        int numMatchingRunModes = 0;
        // 2. support AND
        for (String conjunctivePart : conjunctions.split(Pattern.quote(AND_SEPARATOR))) {
            // 3. support NOT operator
            if (conjunctivePart.startsWith(NOT_PREFIX)) {
                if (activeRunModes.contains(conjunctivePart.substring(NOT_PREFIX.length()))) {
                    return 0;
                }
            } else {
                if (!activeRunModes.contains(conjunctivePart)) {
                    return 0;
                }
            }
            numMatchingRunModes++;
        }
        return numMatchingRunModes;
    }
}
