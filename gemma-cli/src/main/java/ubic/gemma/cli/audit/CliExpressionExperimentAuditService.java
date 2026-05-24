/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
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
package ubic.gemma.cli.audit;

import ubic.gemma.core.analysis.preprocess.filter.FilteringException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Co-bean owning the audit-emission step for ExpressionExperiment-targeted
 * CLI tools. Mirrors {@link CliArrayDesignAuditService}; see that interface
 * for the proxy-boundary rationale.
 */
public interface CliExpressionExperimentAuditService {

    /**
     * Records a {@code MakePrivateEvent} on the experiment. Used by
     * {@code MakeExperimentPrivateCli}.
     */
    void recordMadePrivate( ExpressionExperiment ee, String note );

    /**
     * Records a {@code MakePublicEvent} on the experiment. Used by
     * {@code MakeExperimentsPublicCli}.
     */
    void recordMadePublic( ExpressionExperiment ee, String note );

    /**
     * Runs the sample-correlation compute step for an experiment. The
     * annotation surface records a {@code FailedSampleCorrelationAnalysisEvent}
     * if the call throws (either a {@link FilteringException} or any other
     * {@link Exception}). Used by {@code ExpressionDataCorrMatCli}.
     */
    void computeSampleCorrelation( ExpressionExperiment ee ) throws FilteringException;
}
