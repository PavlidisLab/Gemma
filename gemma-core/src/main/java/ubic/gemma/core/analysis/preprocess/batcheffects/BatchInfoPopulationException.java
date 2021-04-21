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
package ubic.gemma.core.analysis.preprocess.batcheffects;

/**
 * Used to indicate a problem with the population of batch information for a given {@link ubic.gemma.model.expression.experiment.ExpressionExperiment}.
 */
public class BatchInfoPopulationException extends Exception {

    public BatchInfoPopulationException( String message ) {
        super( message );
    }

    public BatchInfoPopulationException( String message, Throwable cause ) {
        super( message, cause );
    }
}
