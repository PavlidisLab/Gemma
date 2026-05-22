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
import ubic.gemma.model.common.auditAndSecurity.eventType.ExperimentalDesignUpdatedEvent;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Implementation of {@link SingleCellExperimentDesignAuditService}: a thin
 * co-bean that exists so the {@code @Audited} aspect can intercept the
 * cell-type-factor create / remove audit emissions previously written
 * imperatively from {@code private} helpers in
 * {@code SingleCellExpressionExperimentServiceImpl}.
 *
 * <p>The {@link Transactional} default propagation ({@code REQUIRED}) joins
 * whichever transaction the caller is in (all callers in
 * {@code SingleCellExpressionExperimentServiceImpl} run with
 * {@code @Transactional} themselves).
 */
@Service
public class SingleCellExperimentDesignAuditServiceImpl implements SingleCellExperimentDesignAuditService {

    private static final Log log = LogFactory.getLog( SingleCellExperimentDesignAuditServiceImpl.class );

    /**
     * {@inheritDoc}
     *
     * <p>The {@link Audited} annotation drives emission through the
     * {@code AuditedAspect}; this method body is intentionally a logging-only
     * marker so the proxy-intercepted return triggers exactly one
     * {@link ExperimentalDesignUpdatedEvent}.
     */
    @Override
    @Transactional
    @Audited(value = ExperimentalDesignUpdatedEvent.class,
            messageSpel = "'Created a cell type factor ' + #cellTypeFactor + ' from preferred cell type assignment ' + #ctlDescription + '.'")
    public void recordCellTypeFactorCreated( ExpressionExperiment ee, ExperimentalFactor cellTypeFactor, String ctlDescription ) {
        log.debug( "Cell-type factor created audit for " + ee + ": " + cellTypeFactor );
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@link Audited} annotation drives emission through the
     * {@code AuditedAspect}; this method body is intentionally a logging-only
     * marker so the proxy-intercepted return triggers exactly one
     * {@link ExperimentalDesignUpdatedEvent}.
     */
    @Override
    @Transactional
    @Audited(value = ExperimentalDesignUpdatedEvent.class,
            messageSpel = "'Removed the cell type factor ' + #removedCellTypeFactor + '.'")
    public void recordCellTypeFactorRemoved( ExpressionExperiment ee, ExperimentalFactor removedCellTypeFactor ) {
        log.debug( "Cell-type factor removed audit for " + ee + ": " + removedCellTypeFactor );
    }
}
