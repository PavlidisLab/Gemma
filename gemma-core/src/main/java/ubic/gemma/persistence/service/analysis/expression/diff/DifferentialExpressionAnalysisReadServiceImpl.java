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
package ubic.gemma.persistence.service.analysis.expression.diff;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisValueObject;
import ubic.gemma.model.analysis.expression.diff.DiffExResultSetSummaryValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.persistence.util.Thaws;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.persistence.service.expression.biomaterial.BioMaterialUtils.visitBioMaterials;

/**
 * Implementation of {@link DifferentialExpressionAnalysisReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link DifferentialExpressionAnalysisService} interface --
 * this class is unsecured at the AOP boundary on purpose, so intra-{@code gemma-core}
 * callers can bypass duplicate ACL checks once authenticated.
 *
 * @see DifferentialExpressionAnalysisService
 */
@Service("differentialExpressionAnalysisReadService")
@Slf4j
public class DifferentialExpressionAnalysisReadServiceImpl implements DifferentialExpressionAnalysisReadService {

    private final DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    @Autowired
    private ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;
    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Autowired
    public DifferentialExpressionAnalysisReadServiceImpl( DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao ) {
        this.differentialExpressionAnalysisDao = differentialExpressionAnalysisDao;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis loadWithExperimentAnalyzed( Long id ) {
        DifferentialExpressionAnalysis analysis = differentialExpressionAnalysisDao.load( id );
        if ( analysis != null ) {
            Hibernate.initialize( analysis.getExperimentAnalyzed() );
        }
        return analysis;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.differentialExpressionAnalysisDao.findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return this.differentialExpressionAnalysisDao.findByFactor( ef );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene ) {
        return this.differentialExpressionAnalysisDao.findExperimentsWithAnalyses( gene );
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis findByExperimentAndAnalysisId( ExpressionExperiment expressionExperiment, boolean includeSubSets, Long analysisId ) {
        return differentialExpressionAnalysisDao.findByExperimentAndAnalysisId( expressionExperiment, includeSubSets, analysisId );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        HashSet<DifferentialExpressionAnalysis> results = new HashSet<>();
        for ( DifferentialExpressionAnalysis ea : expressionAnalyses ) {
            results.add( this.thaw( ea ) );
        }
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        StopWatch timer = new StopWatch();
        timer.start();

        differentialExpressionAnalysis = ensureInSession( differentialExpressionAnalysis );

        Hibernate.initialize( differentialExpressionAnalysis );
        Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed() );
        Hibernate.initialize( differentialExpressionAnalysis.getExperimentAnalyzed().getBioAssays() );
        for ( BioAssay bm : differentialExpressionAnalysis.getExperimentAnalyzed().getBioAssays() ) {
            visitBioMaterials( bm.getSampleUsed(), b -> {
                for ( FactorValue fv : b.getFactorValues() ) {
                    Hibernate.initialize( fv.getExperimentalFactor() );
                }
            } );
        }

        Hibernate.initialize( differentialExpressionAnalysis.getProtocol() );

        if ( differentialExpressionAnalysis.getSubsetFactorValue() != null ) {
            Hibernate.initialize( differentialExpressionAnalysis.getSubsetFactorValue() );
        }

        Collection<ExpressionAnalysisResultSet> ears = differentialExpressionAnalysis.getResultSets();
        Hibernate.initialize( ears );
        for ( ExpressionAnalysisResultSet ear : ears ) {
            Hibernate.initialize( ear );
            Hibernate.initialize( ear.getExperimentalFactors() );
        }
        if ( timer.getTime() > 1000 ) {
            log.info( "Thaw: " + timer.getTime() + "ms" );
        }

        return differentialExpressionAnalysis;
    }

    @Override
    @Transactional(readOnly = true)
    public DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        differentialExpressionAnalysis = thaw( differentialExpressionAnalysis );
        // just loading the entities in the session is sufficient for thawing them
        for ( ExpressionAnalysisResultSet dears : differentialExpressionAnalysis.getResultSets() ) {
            expressionAnalysisResultSetDao.loadWithResultsAndContrasts( dears.getId() );
        }
        return differentialExpressionAnalysis;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.expressionAnalysisResultSetDao.canDelete( differentialExpressionAnalysis );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> findByExperimentIds(
            Collection<Long> experimentIds, boolean includeSubSets, boolean includeAssays ) {
        Map<Long, Collection<Long>> arrayDesignsUsed = new HashMap<>();
        Map<Long, Collection<FactorValue>> experimentAnalyzed2FactorValuesUsed = new HashMap<>();
        Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> hits = this.differentialExpressionAnalysisDao
                .findByExperimentIds( experimentIds, includeSubSets, arrayDesignsUsed, experimentAnalyzed2FactorValuesUsed );

        if ( hits.isEmpty() ) {
            return Collections.emptyMap();
        }

        // initialize result sets and hit list sizes
        // this is necessary because the DEA VO constructor will ignore uninitialized associations
        for ( Collection<DifferentialExpressionAnalysis> deas : hits.values() ) {
            for ( DifferentialExpressionAnalysis dea : deas ) {
                Hibernate.initialize( dea.getResultSets() );
                for ( ExpressionAnalysisResultSet rs : dea.getResultSets() ) {
                    Hibernate.initialize( rs.getHitListSizes() );
                }
                if ( includeAssays ) {
                    dea.getExperimentAnalyzed().getBioAssays().forEach( Thaws::thawBioAssay );
                }
            }
        }

        Map<Long, ExpressionExperimentDetailsValueObject> idMap = IdentifiableUtils.getIdMap( expressionExperimentDao
                .loadDetailsValueObjectsByIds( IdentifiableUtils.getIds( hits.keySet() ) ) );

        Map<ExpressionExperimentDetailsValueObject, Collection<DifferentialExpressionAnalysisValueObject>> result = new HashMap<>();

        for ( Map.Entry<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> e : hits.entrySet() ) {
            ExpressionExperiment sourceExperiment = e.getKey();
            ExpressionExperimentDetailsValueObject eeVo = idMap.get( sourceExperiment.getId() );

            if ( eeVo == null ) {
                log.warn( "Could not find details VO for experiment with ID " + e.getKey() + ", ignoring." );
                continue;
            }

            Collection<DifferentialExpressionAnalysisValueObject> summaries = new HashSet<>();
            for ( DifferentialExpressionAnalysis analysis : e.getValue() ) {
                Collection<ExpressionAnalysisResultSet> results = analysis.getResultSets();

                DifferentialExpressionAnalysisValueObject avo = new DifferentialExpressionAnalysisValueObject( analysis );

                BioAssaySet experimentAnalyzed = analysis.getExperimentAnalyzed();

                avo.setExperimentAnalyzedId( experimentAnalyzed.getId() ); // might be a subset.

                if ( analysis.getSubsetFactorValue() != null ) {
                    // subset analysis
                    assert experimentAnalyzed instanceof ExpressionExperimentSubSet;
                    avo.setSourceExperimentId( ( ( ExpressionExperimentSubSet ) experimentAnalyzed ).getSourceExperiment().getId() );
                    avo.setSubsetFactorValue( new FactorValueValueObject( analysis.getSubsetFactorValue() ) );
                    avo.setSubsetFactor(
                            new ExperimentalFactorValueObject( analysis.getSubsetFactorValue().getExperimentalFactor() ) );
                }

                if ( arrayDesignsUsed.containsKey( experimentAnalyzed.getId() ) ) {
                    avo.setArrayDesignsUsed( arrayDesignsUsed.get( experimentAnalyzed.getId() ) );
                } else {
                    log.warn( "No array designs found for experiment analyzed with ID " + experimentAnalyzed.getId() + ", ignoring." );
                }

                if ( experimentAnalyzed2FactorValuesUsed.containsKey( experimentAnalyzed.getId() ) ) {
                    Collection<FactorValue> fvs = experimentAnalyzed2FactorValuesUsed.get( experimentAnalyzed.getId() );
                    ExperimentalFactorValueObject subsetFactor = avo.getSubsetFactor();
                    for ( FactorValue fv : fvs ) {
                        Long experimentalFactorId = fv.getExperimentalFactor().getId();
                        if ( subsetFactor != null && experimentalFactorId.equals( subsetFactor.getId() ) ) {
                            continue;
                        }
                        avo.getFactorValuesUsedByExperimentalFactorId()
                                .computeIfAbsent( experimentalFactorId, k -> new HashSet<>() )
                                .add( new FactorValueValueObject( fv ) );
                    }
                } else {
                    log.warn( "No factor values found for experiment analyzed with ID " + experimentAnalyzed.getId() + ", ignoring." );
                }

                for ( ExpressionAnalysisResultSet resultSet : results ) {
                    DiffExResultSetSummaryValueObject desvo = new DiffExResultSetSummaryValueObject( resultSet );
                    desvo.setArrayDesignsUsed( avo.getArrayDesignsUsed() );
                    desvo.setBioAssaySetAnalyzedId( experimentAnalyzed.getId() ); // might be a subset.
                    desvo.setAnalysisId( analysis.getId() );
                    avo.getResultSets().add( desvo );
                }

                summaries.add( avo );
            }
            result.put( eeVo, summaries );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DifferentialExpressionAnalysis> findByExperiment( ExpressionExperiment experiment, boolean includeSubSets ) {
        return this.differentialExpressionAnalysisDao.findByExperiment( experiment, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<ExpressionExperiment, Collection<DifferentialExpressionAnalysis>> findByExperiments(
            Collection<ExpressionExperiment> experiments, boolean includeSubSets ) {
        return this.differentialExpressionAnalysisDao
                .findByExperiments( experiments, includeSubSets );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> experimentIds, boolean includeSubSets ) {
        return this.differentialExpressionAnalysisDao.getExperimentsWithAnalysis( experimentIds, includeSubSets );
    }

    /**
     * Local re-implementation of {@code AbstractService#ensureInSession} so this service
     * doesn't have to extend the heavier base class. Matches the deprecated base-class
     * semantics: null-tolerant, transient-tolerant, otherwise re-loads by ID from the DAO.
     */
    private DifferentialExpressionAnalysis ensureInSession( DifferentialExpressionAnalysis entity ) {
        if ( entity == null ) {
            return null;
        }
        Long id = entity.getId();
        if ( id == null ) {
            return entity; // transient
        }
        return requireNonNull( differentialExpressionAnalysisDao.load( id ),
                String.format( "No DifferentialExpressionAnalysis with ID %d.", id ) );
    }
}
