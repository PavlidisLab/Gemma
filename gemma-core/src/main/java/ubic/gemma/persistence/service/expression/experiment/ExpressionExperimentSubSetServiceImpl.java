/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import java.util.Collection;

/**
 * @author pavlidis
 * @see ExpressionExperimentSubSetService
 */
@Service
public class ExpressionExperimentSubSetServiceImpl extends AbstractService<ExpressionExperimentSubSet>
        implements ExpressionExperimentSubSetService {

    private final ExpressionExperimentSubSetReadService expressionExperimentSubSetReadService;
    private final DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    public ExpressionExperimentSubSetServiceImpl( ExpressionExperimentSubSetDao expressionExperimentSubSetDao,
            ExpressionExperimentSubSetReadService expressionExperimentSubSetReadService,
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService ) {
        super( expressionExperimentSubSetDao );
        this.expressionExperimentSubSetReadService = expressionExperimentSubSetReadService;
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ExpressionExperimentSubSet> findByBioAssayIn( Collection<BioAssay> bioAssays ) {
        return expressionExperimentSubSetReadService.findByBioAssayIn( bioAssays );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor ) {
        return expressionExperimentSubSetReadService.getFactorValuesUsed( entity, factor );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValueValueObject> getFactorValuesUsed( Long subSetId, Long experimentalFactor ) {
        return expressionExperimentSubSetReadService.getFactorValuesUsedAsVO( subSetId, experimentalFactor );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ArrayDesign> getArrayDesignsUsed( ExpressionExperimentSubSet subset ) {
        return expressionExperimentSubSetReadService.getArrayDesignsUsed( subset );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionExperimentSubSet loadWithBioAssays( Long id ) {
        return expressionExperimentSubSetReadService.loadSubSetWithBioAssays( id );
    }

    /**
     * doesn't include removal of sample coexpression matrices, PCA, probe2probe coexpression links, or adjusting
     * experiment set members
     *
     * @param subset subset
     */
    @Override
    @Transactional
    public void remove( ExpressionExperimentSubSet subset ) {
        subset = ensureInSession( subset );
        // Remove differential expression analyses
        this.differentialExpressionAnalysisService.removeForExperimentAnalyzed( subset );
        super.remove( subset );
    }

    @Override
    public void remove( Collection<ExpressionExperimentSubSet> entities ) {
        entities.forEach( this::remove );
    }

}