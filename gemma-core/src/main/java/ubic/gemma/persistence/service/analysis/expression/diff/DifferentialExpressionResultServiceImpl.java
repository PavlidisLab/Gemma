/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.persistence.service.analysis.expression.diff;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.*;

/**
 * @author keshav
 * @see DifferentialExpressionResultService
 */
@Service
public class DifferentialExpressionResultServiceImpl extends AbstractService<DifferentialExpressionAnalysisResult>
        implements DifferentialExpressionResultService {

    private final DifferentialExpressionResultDao DERDao;

    @Autowired
    public DifferentialExpressionResultServiceImpl( DifferentialExpressionResultDao DERDao ) {
        super( DERDao );
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