/*
 * The Gemma project
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
package ubic.gemma.core.analysis.preprocess;

import org.apache.commons.lang3.exception.ExceptionUtils;
import ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationException;
import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Allows us to catch preprocessing errors and handle them correctly.
 * <p>
 * The main kind of preprocessing exceptions are:
 * <ul>
 *  <li>{@link QuantitationTypeDetectionRelatedPreprocessingException} when QT type cannot be detected from data or when
 *  the detected one disagrees with the assigned one</li>
 *  <li>{@link QuantitationTypeConversionRelatedPreprocessingException} when a desired QT conversion is not possible</li>
 *  <li>{@link FilteringException} when processed data cannot be filtered</li>
 *  <li>{@link BatchInfoPopulationException} when batch info cannot be detected, populated, etc.</li>
 *  <li>{@link SVDRelatedPreprocessingException} when singular value decomposition fails</li>
 *  </ul>
 * @author Paul
 */
public class PreprocessingException extends RuntimeException {

    private static final long serialVersionUID = -8463478950898408838L;

    public PreprocessingException( ExpressionExperiment ee, String message ) {
        super( String.format( "Failed to pre-process %s: %s", ee.getShortName(), message ) );
    }

    public PreprocessingException( ExpressionExperiment ee, String message, Throwable cause ) {
        super( String.format( "Failed to pre-process %s: %s", ee.getShortName(), message ), cause );
    }

    public PreprocessingException( ExpressionExperiment ee, Throwable cause ) {
        super( String.format( "Failed to pre-process %s: %s", ee.getShortName(), ExceptionUtils.getRootCauseMessage( cause ) ), cause );
    }
}
