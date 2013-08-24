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
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import org.springframework.stereotype.Service;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * I made a wrapper class to OutlierDetectionServiceImpl so that I could write transactional methods
 * 
 * @version $Id$
 */
@Service
public class OutlierDetectionServiceWrapperImpl implements OutlierDetectionServiceWrapper {

    @Override
    public Collection<OutlierDetails> findOutliers( ExpressionExperiment ee, OutlierDetectionService outlierDetector ) {
        return outlierDetector.identifyOutliers( ee );
    }

    @Override
    public OutlierDetectionTestDetails findOutliers( ExpressionExperiment ee, OutlierDetectionService outlierDetector,
            boolean useRegression, boolean findByMedian ) {
        return outlierDetector.identifyOutliers( ee, useRegression, findByMedian );
    }

    @Override
    public OutlierDetectionTestDetails findOutliersByCombinedMethod( ExpressionExperiment ee,
            OutlierDetectionService outlierDetector ) {
        return outlierDetector.identifyOutliersByCombinedMethod( ee );
    }

}
