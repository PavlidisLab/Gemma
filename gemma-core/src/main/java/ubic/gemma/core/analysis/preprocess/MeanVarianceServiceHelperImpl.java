/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.core.analysis.preprocess;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * @author ptan
 */
@Component
public class MeanVarianceServiceHelperImpl implements MeanVarianceServiceHelper {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExpressionDataMatrixService expressionDataMatrixService;

    @Override
    public void createMeanVariance( ExpressionExperiment ee, MeanVarianceRelation mvr ) {
        mvr.setSecurityOwner( ee );
        ee.setMeanVarianceRelation( mvr );
        expressionExperimentService.update( ee );
    }

    @Override
    public ExpressionDataDoubleMatrix getIntensities( ExpressionExperiment ee ) {
        return expressionDataMatrixService.getProcessedExpressionDataMatrix( ee );
    }

}
