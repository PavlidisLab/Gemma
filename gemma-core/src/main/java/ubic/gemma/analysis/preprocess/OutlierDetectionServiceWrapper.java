/*
 * The gemma-core project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.analysis.preprocess;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

public interface OutlierDetectionServiceWrapper {

    /** Call default */
    public abstract Collection<OutlierDetails> findOutliers( ExpressionExperiment ee );

    /** Use regression and/or sort-by-median algorithm; store information about test in testDetails */
    public abstract OutlierDetectionTestDetails findOutliers( ExpressionExperiment ee, boolean useRegression,
            boolean findByMedian );

    /** Returns combined results from Raymond's algorithm and sort-by-median algorithm; always uses regression */
    public abstract OutlierDetectionTestDetails findOutliersByCombinedMethod( ExpressionExperiment ee );

}
