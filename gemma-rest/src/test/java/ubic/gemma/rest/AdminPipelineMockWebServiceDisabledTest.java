/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.rest;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The prod-safety guarantee: with no {@code scheduler-mock} profile, the
 * {@link ubic.gemma.core.pipeline.MockSchedulerControl} bean is absent, so the
 * resource's {@code control} field is null and every endpoint 404s. Verified as a
 * plain unit test (a freshly-constructed resource has a null control) — no need for a
 * second Jersey context that merely omits one bean.
 */
class AdminPipelineMockWebServiceDisabledTest {

    private final AdminPipelineMockWebService resource = new AdminPipelineMockWebService();

    @Test
    void advance_404WhenControlAbsent() {
        AdminPipelineMockWebService.AdvanceRequest req = new AdminPipelineMockWebService.AdvanceRequest();
        req.ms = 1000;
        assertThatThrownBy( () -> resource.advance( req ) ).isInstanceOf( NotFoundException.class );
    }

    @Test
    void setScenario_404WhenControlAbsent() {
        assertThatThrownBy( () -> resource.setScenario( new AdminPipelineMockWebService.SetScenarioRequest() ) )
                .isInstanceOf( NotFoundException.class );
    }

    @Test
    void listScenarios_404WhenControlAbsent() {
        assertThatThrownBy( resource::listScenarios ).isInstanceOf( NotFoundException.class );
    }
}
