/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;

/**
 * I made a wrapper class to OutlierDetectionServiceImpl so that I could write transactional methods
 */
@Service
public class OutlierDetectionServiceWrapperImpl implements OutlierDetectionServiceWrapper {

    @Autowired
    private OutlierDetectionService outlierDetectionService;

    @Override
    @Transactional(readOnly = true)
    public Collection<OutlierDetails> findOutliers( ExpressionExperiment ee ) {
        return outlierDetectionService.identifyOutliers( ee );
    }

    @Override
    @Transactional(readOnly = true)
    public OutlierDetectionTestDetails findOutliers( ExpressionExperiment ee, boolean useRegression,
            boolean findByMedian ) {
        return outlierDetectionService.identifyOutliers( ee, useRegression, findByMedian );
    }

    @Override
    @Transactional(readOnly = true)
    public OutlierDetectionTestDetails findOutliersByCombinedMethod( ExpressionExperiment ee ) {
        return outlierDetectionService.identifyOutliersByCombinedMethod( ee );
    }

}
