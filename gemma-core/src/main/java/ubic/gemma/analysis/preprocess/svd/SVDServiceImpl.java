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
package ubic.gemma.analysis.preprocess.svd;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.analysis.expression.pca.ProbeLoading;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Perform SVD on expression data and store the results.
 */
@Component
public class SVDServiceImpl implements SVDService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SVDServiceHelper svdServiceHelper;

    @Override
    public Map<ProbeLoading, DoubleVectorValueObject> getTopLoadedVectors( Long eeId, int component, int count ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) return null;

        return svdServiceHelper.getTopLoadedVectors( ee, component, count );

    }

    /**
     * @param ee
     * @return
     */
    @Override
    public boolean hasPca( Long eeId ) {
        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        return svdServiceHelper.hasPca( ee );

    }

    /**
     * Get the SVD information for experiment with id given.
     * 
     * @param id
     * @return value or null if there isn't one.
     */
    @Override
    public SVDValueObject getSvd( Long eeId ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        return svdServiceHelper.retrieveSvd( ee );

    }

    @Override
    public SVDValueObject svd( Long eeId ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        return svdServiceHelper.svd( ee );
    }

    @Override
    public SVDValueObject getSvdFactorAnalysis( Long eeId ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        return svdServiceHelper.svdFactorAnalysis( ee );
    }

}
