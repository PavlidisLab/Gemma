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

    /**
     * Record a {@code SingleBatchDeterminationEvent} on {@code ee}. Exists so the
     * branch in {@code BatchInfoPopulationServiceImpl} that chooses between a
     * single-batch and a multi-batch event can dispatch through a Spring proxy
     * carrying a single, deterministic {@link ubic.gemma.core.security.audit.Audited}
     * annotation -- the {@code @Audited} aspect requires one event class per
     * method, so the original multi-branch private method had to be split.
     *
     * @param ee   experiment
     * @param note short summary stored in {@code AUDIT_EVENT.NOTE}
     */
    void recordSingleBatchDetermination( ExpressionExperiment ee, String note );

    /**
     * Record a {@code BatchInformationFetchingEvent} on {@code ee}. Companion to
     * {@link #recordSingleBatchDetermination(ExpressionExperiment, String)} for the
     * multi-batch branch; see that method's javadoc for rationale.
     *
     * @param ee   experiment
     * @param note short summary stored in {@code AUDIT_EVENT.NOTE}
     */
    void recordBatchInformationFetched( ExpressionExperiment ee, String note );

}