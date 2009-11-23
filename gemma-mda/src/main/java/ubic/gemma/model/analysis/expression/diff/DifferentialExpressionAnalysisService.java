/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.analysis.expression.diff;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author kelsey
 * @version $Id$
 */
public interface DifferentialExpressionAnalysisService extends
        ubic.gemma.model.analysis.AnalysisService<DifferentialExpressionAnalysis> {

    /**
     * @param ExpressionAnalysisResultSet
     * @param threshold (double)
     * @return an integer count of all the probes that met the given threshold in the given expressionAnalysisResultSet
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public long countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold );

    /**
     * 
     */

    @Secured( { "GROUP_USER" })
    public DifferentialExpressionAnalysis create( DifferentialExpressionAnalysis analysis );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<DifferentialExpressionAnalysis> find( ubic.gemma.model.genome.Gene gene,
            ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet, double threshold );

    /**
     * <p>
     * Given a collection of ids, return a map of id -> differential expression analysis (one per id).
     * </p>
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Map<Long, DifferentialExpressionAnalysis> findByInvestigationIds(
            java.util.Collection<Long> investigationIds );

    /**
     * <p>
     * Return a collection of experiments in which the given gene was analyzed.
     * </p>
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public java.util.Collection<ExpressionExperiment> findExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene );

    /**
     * @param resultSetIds
     * @return
     */
    public java.util.Collection<ExpressionAnalysisResultSet> getResultSets( java.util.Collection<Long> resultSetIds );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public java.util.Collection<ExpressionAnalysisResultSet> getResultSets(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_COLLECTION_READ" })
    public void thaw( java.util.Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public void thaw(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis );

    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( DifferentialExpressionAnalysis o );

    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ExpressionAnalysisResultSet a );

}
