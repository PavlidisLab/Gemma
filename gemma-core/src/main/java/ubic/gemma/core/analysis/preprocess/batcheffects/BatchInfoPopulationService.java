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

import java.io.File;
import java.util.Map;

/**
 * Retrieve batch information from the data source, if possible, and populate it into experiments.
 *
 * @author paul
 */
public interface BatchInfoPopulationService {

    /**
     * Attempt to obtain batch information from the data provider and populate it into the given experiment. The method
     * used may vary. For GEO, the default method is to download the raw data files, and look in them for a date. This
     * is not implemented for every possible type of raw data file.
     *
     * @param ee    the experiment
     * @param force whether to force recomputation
     * @return true if information was successfully obtained
     */
    boolean fillBatchInformation( ExpressionExperiment ee, boolean force );

    /**
     * Attempt to obtain batch information from the data provider and populate it into the given experiment. The method
     * used may vary. For GEO, the default method is to download the raw data files, and look in them for a date. This
     * is not implemented for every possible type of raw data file.
     *
     * @param ee     the experiment
     * @param force  whether to force recomputation
     * @param rnaSeq if true, will look for fastq header files instead.
     * @return true if information was successfully obtained
     */
    boolean fillBatchInformation( ExpressionExperiment ee, boolean force, boolean rnaSeq );

    /**
     * Traverses given folder and returns a list of immediate sub-folders.
     * @param folder the folder to browse.
     * @return a map of folder name to the File object representing the filesystem folder.
     */
    Map<String, File> getFoldersForFolder( File folder );
}