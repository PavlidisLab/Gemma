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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.nio.file.Path;

/**
 * Retrieve batch information from the data source, if possible, and populate it into experiments.
 *
 * @author paul
 */
public interface BatchInfoPopulationService {

    /**
     * Attempt to obtain batch information from the data provider and populate it into the given experiment. The method
     * used may vary. For GEO, the default method is to download the raw data files, and look in them for a date. This
     * is not implemented for every possible type of raw data file. For RNA-seq, we look for FASTQ headers under the
     * configured FASTQ_HEADERS_ROOT.
     *
     * @param  ee    the experiment
     * @param  force whether to force recomputation
     * @throws BatchInfoPopulationException describing the issue with populating batch information
     */
    void fillBatchInformation( ExpressionExperiment ee, boolean force ) throws BatchInfoPopulationException;

    /**
     * Set the path to use to resolve FASTQ headers.
     * <p>
     * Exposed for testing, do not use in production code.
     */
    void setFastqHeadersDir( Path fastqHeadersDir );
}