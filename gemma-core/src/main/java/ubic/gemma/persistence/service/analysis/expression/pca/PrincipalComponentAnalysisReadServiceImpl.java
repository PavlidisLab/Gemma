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
package ubic.gemma.persistence.service.analysis.expression.pca;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Implementation of {@link PrincipalComponentAnalysisReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL
 * enforcement is the responsibility of the facade
 * {@link PrincipalComponentAnalysisService} interface -- this class is
 * unsecured at the AOP boundary on purpose, so intra-{@code gemma-core}
 * callers can bypass duplicate ACL checks once authenticated.
 *
 * @see PrincipalComponentAnalysisService
 */
@Service("principalComponentAnalysisReadService")
public class PrincipalComponentAnalysisReadServiceImpl implements PrincipalComponentAnalysisReadService {

    private static final Logger log = LoggerFactory.getLogger( PrincipalComponentAnalysisReadServiceImpl.class );

    private final PrincipalComponentAnalysisDao principalComponentAnalysisDao;

    @Autowired
    public PrincipalComponentAnalysisReadServiceImpl( PrincipalComponentAnalysisDao principalComponentAnalysisDao ) {
        this.principalComponentAnalysisDao = principalComponentAnalysisDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProbeLoading> getTopLoadedProbes( ExpressionExperiment ee, int component, int count ) {
        PrincipalComponentAnalysis pca = loadForExperiment( ee );
        if ( pca == null ) {
            return new ArrayList<>();
        }
        if ( component < 1 ) {
            throw new IllegalArgumentException( "Component must be greater than zero" );
        }
        return this.principalComponentAnalysisDao.getTopLoadedProbes( ee, component, count );
    }

    @Override
    @Transactional(readOnly = true)
    public PrincipalComponentAnalysis loadForExperiment( ExpressionExperiment ee ) {
        Collection<PrincipalComponentAnalysis> pcas = this.principalComponentAnalysisDao.findByExperiment( ee );
        if ( pcas.size() > 1 )
            log.warn( "Multiple PCAs found for " + ee + ", returning arbitrary one" );
        if ( !pcas.isEmpty() ) {
            return pcas.iterator().next();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByExperiment( ExpressionExperiment ee ) {
        return this.principalComponentAnalysisDao.existsByExperiment( ee );
    }
}
