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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Allows us to catch preprocessing errors and handle them correctly.
 * <p>
 * The main kind of preprocessing exceptions are {@link ubic.gemma.core.analysis.preprocess.filter.FilteringException}
 * and {@link ubic.gemma.core.analysis.preprocess.batcheffects.BatchInfoPopulationException}.
 *
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
