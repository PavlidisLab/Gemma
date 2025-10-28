/*
 * The Gemma_sec1 project
 *
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.core.analysis.report;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;

import java.util.Collection;

/**
 * Methods for reading and creating reports on ExpressinExperiments. Reports are typically updated either on demand or
 * after an analysis; and retrieval is usually from the web interface.
 *
 * @author paul
 */
public interface ExpressionExperimentReportService {

    /**
     * Invalidate the cached 'report' for the experiment with the given id. If it is not cached nothing happens.
     *
     * @param id the id of the entity to evict
     */
    void evictFromCache( Long id );

    /**
     * Generate a value object that contain summary information about links, biomaterials, and datavectors
     *
     * @param id the id of the ee to generate summary for
     * @return details VO
     */
    ExpressionExperimentDetailsValueObject generateSummary( Long id );

    /**
     * Generates reports on ALL experiments, including 'private' ones. This should only be run by administrators as it
     * takes a while to run.
     */
    @Secured({ "GROUP_AGENT" })
    Collection<ExpressionExperimentDetailsValueObject> generateSummaryObjects();

    void getAnnotationInformation( Collection<ExpressionExperimentDetailsValueObject> vos );

    void populateEventInformation( Collection<ExpressionExperimentDetailsValueObject> vos );

    /**
     * Fills in link analysis and differential expression analysis summaries, and other info from the report.
     *
     * @param vos value objects
     */
    void populateReportInformation( Collection<ExpressionExperimentDetailsValueObject> vos );

    /**
     * retrieves a collection of cached value objects containing summary information
     *
     * @param ids the ids of ees for which the summary objects should be retrieved.
     * @return a collection of cached value objects
     */
    @SuppressWarnings("unused")
    // Possible external use
    Collection<ExpressionExperimentDetailsValueObject> retrieveSummaryObjects( Collection<Long> ids );

    /**
     * Recalculates the batch effect and batch confound information for the given dataset.
     * @param ee the experiment to recalculate the batch properties for.
     */
    @Secured({ "GROUP_AGENT" })
    void recalculateExperimentBatchInfo( ExpressionExperiment ee );

    /**
     * Generate a report that describes all the data processing that was performed on a given {@link ExpressionExperiment}
     * to ultimately produce its procssed data vectors.
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    ExpressionExperimentDataProcessingReport generateDataProcessingReport( ExpressionExperiment ee );
}