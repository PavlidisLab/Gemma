/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.experiment.PreboardedExperiment;

/**
 * Co-bean carrying the {@code PreboardedCreatedEvent} emission for
 * {@link PreboardedExperimentServiceImpl#createPreboarded}.
 * <p>
 * Hoisted out of the create path so the emission method is invoked through a
 * Spring proxy and the {@code @Audited} aspect can intercept it. The aspect's
 * {@code findAuditable} scans the argument list left-to-right for the first
 * {@code Auditable}; in {@code createPreboarded}, the {@code PreboardedExperiment}
 * is freshly constructed inside the method body (the only arguments are
 * {@code String}s), so the target must be passed in as an argument here.
 *
 * @see ubic.gemma.core.security.audit.Audited
 */
public interface PreboardedAuditService {

    /**
     * Record a {@code PreboardedCreatedEvent} against the freshly persisted
     * preboarded with a note that interpolates the accession. Dispatch is via
     * the {@code @Audited} aspect.
     *
     * @param preboarded the freshly persisted preboarded; receives the audit row
     * @param accession  the accession the preboarded was created for; included in the note
     */
    void recordPreboardedCreated( PreboardedExperiment preboarded, String accession );
}
