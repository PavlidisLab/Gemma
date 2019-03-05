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

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.Date;
import java.util.Map;

/**
 * @author paul
 */
public interface BatchInfoPopulationHelperService {

    /**
     * For RNA-seq, we based the batching on the available device/run/flowcell/lane information
     * 
     * @param  ee      experiment
     * @param  headers map of biomaterial to a string. If there was no usable FASTQ header, we just use the GPL ID
     * @return         factor
     */
    ExperimentalFactor createRnaSeqBatchFactor( ExpressionExperiment ee, Map<BioMaterial, String> headers );

    ExperimentalFactor createBatchFactor( ExpressionExperiment ee, Map<BioMaterial, Date> dates );

}