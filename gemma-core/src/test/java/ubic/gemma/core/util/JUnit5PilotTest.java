/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package ubic.gemma.core.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 3 JUnit 5 migration smoke test (Phase A).
 * <p>
 * Proves that {@code org.junit.jupiter:junit-jupiter} is on the test
 * classpath and that Maven Surefire discovers + runs Jupiter tests
 * alongside the existing JUnit 4 corpus (which continues to run via
 * {@code org.junit.vintage:junit-vintage-engine}). Pure unit logic, no
 * Spring context, no DB, no network. Delete during Phase C cleanup once
 * the rest of the corpus has been migrated.
 *
 * @author Phase 3 JUnit 5 migration
 */
@Tag( "junit5-smoke" )
class JUnit5PilotTest {

    @Test
    @DisplayName( "Jupiter engine smoke check" )
    void smokeTestPipeline() {
        // Note: Jupiter Assertions.assertEquals takes (expected, actual) — same
        // as JUnit 4 — but the optional message is the LAST arg, not the first.
        assertEquals( 2, 1 + 1, "trivial arithmetic" );
    }

    @Test
    void anotherSmokeCheck() {
        assertTrue( "gemma".startsWith( "g" ) );
    }
}
