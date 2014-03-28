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
package ubic.gemma.model.analysis.expression.coexpression;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.analysis.AnalysisService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * Deals with the Analysis objects for Coexpression - not the coexpression results themselves.
 * 
 * @author kelsey
 * @version $Id$
 */
public interface CoexpressionAnalysisService extends AnalysisService<CoexpressionAnalysis> {

    /**
     * 
     */
    @Secured({ "GROUP_USER" })
    public CoexpressionAnalysis create( CoexpressionAnalysis probeCoexpressionAnalysis );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<CoexpressionAnalysis> findByParentTaxon( Taxon taxon );

    /**
     * 
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<CoexpressionAnalysis> findByTaxon( Taxon taxon );

    /**
     * Not secured: for internal use only
     * 
     * @param taxon
     * @return ids of bioassaysets from the given taxon that have a coexpression analysis
     */
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon );

    /**
     * Not secured: for internal use only
     * 
     * @param idsToFilter starting list of bioassayset ids.
     * @return the ones which have a coexpression analysis.
     */
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter );

    /**
     * @param o
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( CoexpressionAnalysis o );

    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_READ" })
    public CoexpCorrelationDistribution getCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment );

    /**
     * For backfilling of the coexpression distributions from flat files - remove when no longer needed.
     * 
     * @param expressionExperiment
     * @param coexpd
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "ACL_SECURABLE_EDIT" })
    public void addCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment,
            CoexpCorrelationDistribution coexpd );

}
