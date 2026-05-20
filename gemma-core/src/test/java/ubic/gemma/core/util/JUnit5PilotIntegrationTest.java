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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 3 JUnit 5 migration pilot integration test (Phase B0).
 * <p>
 * Counterpart to {@link JUnit5PilotTest} for the failsafe side. Proves:
 * <ul>
 *   <li>maven-failsafe-plugin selects {@code @Tag("integration")} classes
 *       under the Phase B0 dual-selector tag expression
 *       ({@code ubic.gemma.core.util.test.category.IntegrationTest | integration}),</li>
 *   <li>{@link SpringExtension} integrates with the Jupiter engine — i.e.,
 *       autowiring works inside a {@code @Test} method without the legacy
 *       {@code AbstractJUnit4SpringContextTests} base class.</li>
 * </ul>
 * Deliberately uses a tiny inline {@code @Configuration} so the pilot does
 * not depend on the full Gemma application context (no DB, no network).
 * Retained post-Phase-C-cleanup as a minimal Jupiter+Spring smoke-test
 * exemplar; the legacy JUnit 4 chain it once pre-empted has been retired.
 *
 * @author Phase 3 JUnit 5 migration
 */
@Tag( "integration" )
@ExtendWith( SpringExtension.class )
@ContextConfiguration( classes = JUnit5PilotIntegrationTest.PilotConfig.class )
class JUnit5PilotIntegrationTest {

    @Autowired
    private String pilotBean;

    @Test
    @DisplayName( "Failsafe selects @Tag(\"integration\") + SpringExtension autowires" )
    void pilotIntegrationCheck() {
        assertNotNull( pilotBean, "Spring context should have injected pilotBean" );
        assertEquals( "pilot", pilotBean );
    }

    @Configuration
    static class PilotConfig {
        @Bean
        public String pilotBean() {
            return "pilot";
        }
    }
}
