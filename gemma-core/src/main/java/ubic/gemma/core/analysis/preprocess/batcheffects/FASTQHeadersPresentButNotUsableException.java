/*
 * The gemma-core project
 *
 * Copyright (c) 2021 University of British Columbia
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

package ubic.gemma.core.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 *
 * Indicates that FASTQ headers were present, but were not in a format that yields usable information on batches.
 *
 * @author paul
 */
public class FASTQHeadersPresentButNotUsableException extends BatchInfoPopulationException {

    /**
     */
    public FASTQHeadersPresentButNotUsableException( ExpressionExperiment ee, String message ) {
        super( ee, message );
    }

    private static final long serialVersionUID = 1917074433646650950L;

}
