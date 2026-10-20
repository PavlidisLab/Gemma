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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.diff.Baseline;
import ubic.gemma.model.analysis.expression.diff.ContrastsValueObject;
import ubic.gemma.model.analysis.expression.diff.DiffExResultSetSummaryValueObject;
import ubic.gemma.model.analysis.expression.diff.DiffExprGeneSearchResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.BioAssaySetValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Facade for {@link DifferentialExpressionResultService}.
 * <p>
 * Phase 3 service decomposition: the row-level result read cluster has been
 * extracted to {@link DifferentialExpressionResultReadService}; this facade delegates
 * reads to that bean while continuing to host the ACL {@code @Secured} annotations
 * (which live on the {@link DifferentialExpressionResultService} interface) and the
 * inherited {@link ubic.gemma.persistence.service.BaseReadOnlyService} surface
 * (provided by {@link AbstractService}).
 *
 * @author keshav
 * @see DifferentialExpressionResultService
 */
@Service
public class DifferentialExpressionResultServiceImpl extends AbstractService<DifferentialExpressionAnalysisResult>
        implements DifferentialExpressionResultService {

    private final DifferentialExpressionResultReadService readService;

    @Autowired
    public DifferentialExpressionResultServiceImpl( DifferentialExpressionResultDao DERDao,
            DifferentialExpressionResultReadService readService ) {
        super( DERDao );
        this.readService = readService;
    }

    // =====================================================================
    // Read methods -- delegate to DifferentialExpressionResultReadService.
    // ACL @Secured annotations live on the DifferentialExpressionResultService
    // interface and apply at the facade proxy boundary.
    // =====================================================================

    @Override
    public List<DifferentialExpressionAnalysisResult> findByGeneAndExperimentAnalyzedIds( Gene gene, boolean useGene2Cs, boolean keepNonSpecific, Collection<Long> experimentAnalyzedIds, boolean includeSubSets, Map<DifferentialExpressionAnalysisResult, Long> sourceExperimentIdMap, Map<DifferentialExpressionAnalysisResult, Long> experimentAnalyzedIdMap, Map<DifferentialExpressionAnalysisResult, Baseline> baselineMap, double threshold, boolean initializeFactorValues ) {
        return readService.findByGeneAndExperimentAnalyzedIds( gene, useGene2Cs, keepNonSpecific, experimentAnalyzedIds, includeSubSets,
                sourceExperimentIdMap, experimentAnalyzedIdMap, baselineMap, threshold, initializeFactorValues );
    }

    @Override
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByExperimentAnalyzed(
            Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets, double threshold, int limit ) {
        return readService.findByExperimentAnalyzed( experimentsAnalyzed, includeSubSets, threshold, limit );
    }

    @Override
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGene( Gene gene, boolean useGene2Cs, boolean keepNonSpecificProbes ) {
        return readService.findByGene( gene, useGene2Cs, keepNonSpecificProbes );
    }

    @Override
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGene( Gene gene,
            boolean useGene2Cs, boolean keepNonSpecificProbes, double threshold, int limit ) {
        return readService.findByGene( gene, useGene2Cs, keepNonSpecificProbes, threshold, limit );
    }

    @Override
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGeneAndExperimentAnalyzed( Gene gene,
            boolean useGene2Cs, boolean keepNonSpecificProbes, Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets ) {
        return readService.findByGeneAndExperimentAnalyzed( gene, useGene2Cs, keepNonSpecificProbes, experimentsAnalyzed, includeSubSets );
    }

    @Override
    public Map<BioAssaySetValueObject, List<DifferentialExpressionValueObject>> findByGeneAndExperimentAnalyzed( Gene gene,
            boolean useGene2Cs, boolean keepNonSpecificProbes, Collection<? extends BioAssaySet> experimentsAnalyzed, boolean includeSubSets, double threshold, int limit ) {
        return readService.findByGeneAndExperimentAnalyzed( gene, useGene2Cs, keepNonSpecificProbes, experimentsAnalyzed, includeSubSets, threshold, limit );
    }

    @Override
    public Map<Long, Map<Long, DiffExprGeneSearchResult>> findGeneResultsByResultSetIdsAndGeneIds(
            Collection<DiffExResultSetSummaryValueObject> resultSets, Collection<Long> geneIds ) {
        return readService.findGeneResultsByResultSetIdsAndGeneIds( resultSets, geneIds );
    }

    @Override
    public List<DifferentialExpressionValueObject> findByResultSet( ExpressionAnalysisResultSet resultSet,
            double threshold, int maxResultsToReturn, int minNumberOfResults ) {
        return readService.findByResultSet( resultSet, threshold, maxResultsToReturn, minNumberOfResults );
    }

    @Override
    public Map<Long, ContrastsValueObject> findContrastsByAnalysisResultIds( Collection<Long> ids ) {
        return readService.findContrastsByAnalysisResultIds( ids );
    }
}
