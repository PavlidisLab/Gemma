/*
 * The Gemma project
 *
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.core.analysis.preprocess;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Collection;
import java.util.Optional;

/**
 * @author paul
 */
public interface OutlierDetectionService {

    /**
     * Obtain existing outlier details from a previous {@link #identifyOutliersByMedianCorrelation(ExpressionExperiment)} invocation.
     * @return outlier details or {@code Optional#empty()} if no sample coexpression analysis exists
     */
    Optional<Collection<OutlierDetails>> getOutlierDetails( ExpressionExperiment ee );

    /**
     * Identify outliers by median correlation for the given dataset.
     *
     * @param ee The experiment to identify sample outliers in.
     * @return the information about the identified outliers.
     * @see #identifyOutliersByMedianCorrelation(DoubleMatrix)
     */
    Collection<OutlierDetails> identifyOutliersByMedianCorrelation( ExpressionExperiment ee ) throws FilteringException;

    /**
     * Identify outliers by sorting by median, then looking for non-overlap of first quartile-second quartile range
     * This is exposed for efficiency in geeq score calculation, use this#identifyOutliers(ExpressionExperiment, boolean, boolean)
     * to have the correlation matrix computed correctly for you.
     *
     * @param cormat the correlation matrix to identify outliers in.
     * @return the information about the identified outliers.
     */
    Collection<OutlierDetails> identifyOutliersByMedianCorrelation( DoubleMatrix<BioAssay, BioAssay> cormat );
}