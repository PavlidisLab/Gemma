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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.audit.Audited;
import ubic.gemma.model.common.auditAndSecurity.eventType.PreboardedCreatedEvent;
import ubic.gemma.model.expression.experiment.PreboardedExperiment;

/**
 * Implementation of {@link PreboardedAuditService}: a thin co-bean that exists
 * so {@link #recordPreboardedCreated(PreboardedExperiment, String)} is invoked
 * through a Spring proxy and the {@code @Audited} aspect can intercept its
 * return path and emit the {@link PreboardedCreatedEvent}.
 *
 * <p>The {@code AuditedAspect} can only locate an {@code Auditable} target on
 * the argument list (see {@code AuditedAspect#findAuditable}); the original
 * imperative call in {@code PreboardedExperimentServiceImpl.createPreboarded}
 * targeted a freshly constructed {@code PreboardedExperiment} that was not in
 * the argument list. Hoisting to this co-bean lets the target be passed in as
 * the first argument.
 *
 * <p>The {@link Transactional} default propagation ({@code REQUIRED}) joins
 * the wrapping {@code @Transactional} on {@code createPreboarded}.
 */
@Service
public class PreboardedAuditServiceImpl implements PreboardedAuditService {

    private static final Log log = LogFactory.getLog( PreboardedAuditServiceImpl.class );

    /**
     * {@inheritDoc}
     *
     * <p>The {@link Audited} annotation drives emission through the
     * {@code AuditedAspect}; this method body is intentionally a logging-only
     * marker so the proxy-intercepted return triggers exactly one
     * {@link PreboardedCreatedEvent}.
     */
    @Override
    @Transactional
    @Audited(value = PreboardedCreatedEvent.class,
            messageSpel = "'Preboarded created for accession ' + #accession")
    public void recordPreboardedCreated( PreboardedExperiment preboarded, String accession ) {
        log.debug( "Preboarded created audit for " + preboarded + " (accession=" + accession + ")" );
    }
}
