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

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.ContrastsValueObject;
import ubic.gemma.model.analysis.expression.diff.DiffExResultSetSummaryValueObject;
import ubic.gemma.model.analysis.expression.diff.DiffExprGeneSearchResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.BioAssaySetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubsetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link DifferentialExpressionResultReadService}.
 * <p>
 * All public methods are {@code @Transactional(readOnly = true)}. ACL enforcement is the
 * responsibility of the facade {@link DifferentialExpressionResultService} interface --
 * this class is unsecured at the AOP boundary on purpose, so intra-{@code gemma-core}
 * callers can bypass duplicate ACL checks once authenticated.
 *
 * @see DifferentialExpressionResultService
 */
@Service("differentialExpressionResultReadService")
public class DifferentialExpressionResultReadServiceImpl implements DifferentialExpressionResultReadService {

    private final DifferentialExpressionResultDao DERDao;

    @Autowired
    public DifferentialExpressionResultReadServiceImpl( DifferentialExpressionResultDao DERDao ) {
        this.DERDao = DERDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DifferentialExpressionAnalysisResult> findByGeneAndExperimentAnalyzedIds( Gene gene, boolean useGene2Cs, boolean keepNonSpecific, Collection<Long> experimentAnalyzedIds, boolean includeSubSets, Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap, double threshold, boolean initializeFactorValues ) {
        return DERDao.findByGeneAndExperimentAnalyzed( gene, experimentAnalyzedIds, includeSubSets,
                sourceExperimentIdMap, experimentAnalyzedIdMap, baselineMap, threshold, useGene2Cs, keepNonSpecific, initializeFactorValues );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByExperimentAnalyzed(
            Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets, double threshold, int limit ) {
        return groupDiffExResultVos( this.DERDao.findByExperimentAnalyzed( IdentifiableUtils.getIds( experimentsAnalyzed ),
                experimentsAnalyzed.stream().anyMatch( ea -> ea instanceof ExpressionExperiment ) && includeSubSets,
                threshold, limit ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGene( Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes ) {
        return groupDiffExResultVos( this.DERDao.findByGene( gene, useGene2Cs, keepNonSpecificProbes ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGene( Gene gene,
            boolean useGene2Cs, boolean keepNonSpecificProbes, double threshold, int limit ) {
        return groupDiffExResultVos( this.DERDao.findByGene( gene, useGene2Cs, keepNonSpecificProbes, threshold, limit ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGeneAndExperimentAnalyzed( Gene gene,
            boolean useGene2Cs, boolean keepNonSpecificProbes, Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets ) {
        return groupDiffExResultVos( this.DERDao.findByGeneAndExperimentAnalyzed( gene, useGene2Cs, keepNonSpecificProbes, IdentifiableUtils.getIds( experimentsAnalyzed ),
                experimentsAnalyzed.stream().anyMatch( ea -> ea instanceof ExpressionExperiment ) && includeSubSets ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGeneAndExperimentAnalyzed( Gene gene,
            boolean useGene2Cs, boolean keepNonSpecificProbes, Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets, double threshold, int limit ) {
        return groupDiffExResultVos( this.DERDao.findByGeneAndExperimentAnalyzed( gene, useGene2Cs, keepNonSpecificProbes, IdentifiableUtils.getIds( experimentsAnalyzed ),
                experimentsAnalyzed.stream().anyMatch( ea -> ea instanceof ExpressionExperiment ) && includeSubSets,
                threshold, limit ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findGeneResultsByResultSetIdsAndGeneIds(
            Collection<DiffExResultSetSummaryValueObject> resultSets, Collection<Long> geneIds ) {
        return this.DERDao.findGeneResultsByResultSetIdsAndGeneIds( resultSets, geneIds );
    }

    @Override
    @Transactional(readOnly = true)
    public List<DifferentialExpressionValueObject> findByResultSet( ExpressionAnalysisResultSet resultSet,
            double threshold, int maxResultsToReturn, int minNumberOfResults ) {
        return this.DERDao.findByResultSet( resultSet, threshold, maxResultsToReturn, minNumberOfResults );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ContrastsValueObject> findContrastsByAnalysisResultIds( Collection<Long> ids ) {
        return this.DERDao.findContrastsByAnalysisResultIds( ids );
    }

    private Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> groupDiffExResultVos( Map<? extends BioAssaySet, List<DifferentialExpressionAnalysisResult>> qResult ) {
        Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> results = new HashMap<>();
        for ( Map.Entry<? extends BioAssaySet, List<DifferentialExpressionAnalysisResult>> e : qResult.entrySet() ) {
            BioAssaySetValueObject ee = createValueObject( e.getKey() );
            for ( DifferentialExpressionAnalysisResult dear : e.getValue() ) {
                Hibernate.initialize( dear.getProbe() );
                DifferentialExpressionValueObject probeResult = new DifferentialExpressionValueObject( dear );
                results.computeIfAbsent( ee, k -> new ArrayList<>() ).add( probeResult );
            }
        }
        return results;
    }

    /**
     * Special use case. Use a constructor of the desired VO instead, or the loadValueObject() in all VO-Enabled services.
     * @return an expression experiment value object.
     */
    private BioAssaySetValueObject createValueObject( BioAssaySet bioAssaySet ) {
        if ( bioAssaySet instanceof ExpressionExperiment ) {
            return new ExpressionExperimentValueObject( ( ExpressionExperiment ) bioAssaySet );
        } else if ( bioAssaySet instanceof ExpressionExperimentSubSet ) {
            return new ExpressionExperimentSubsetValueObject( ( ExpressionExperimentSubSet ) bioAssaySet );
        } else {
            throw new UnsupportedOperationException( "Unsupported BioAssaySet type for VO conversion: " + bioAssaySet.getClass().getName() );
        }
    }
}
