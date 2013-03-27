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

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @version $Id$
 */
public interface GeneCoexpressionAnalysisService extends
        ubic.gemma.model.analysis.AnalysisService<GeneCoexpressionAnalysis> {

    /**
     * 
     */
    @Secured({ "GROUP_ADMIN" })
    public GeneCoexpressionAnalysis create( GeneCoexpressionAnalysis analysis );

    /**
     * <p>
     * This is required only to allow security filtering of the expression experiment collections.
     * </p>
     */
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_COLLECTION_READ" })
    public Collection<ExpressionExperiment> getDatasetsAnalyzed( GeneCoexpressionAnalysis analysis );

    /**
     * Get the number of data sets analyzed .
     */
    public int getNumDatasetsAnalyzed( GeneCoexpressionAnalysis analysis );

    /**
     * 
     */
    public void thaw( GeneCoexpressionAnalysis geneCoexpressionAnalysis );

    public GeneCoexpressionAnalysis findCurrent( Taxon taxon );

    /**
     * 
     */
    @Secured({ "GROUP_ADMIN" })
    public void update( GeneCoexpressionAnalysis geneCoExpressionAnalysis );

    /*
     * Note lack of security on findByParentTaxon and findByTaxon - just not needed.
     */
    
    /**
     * @param taxon
     * @return
     */
    public java.util.Collection<GeneCoexpressionAnalysis> findByParentTaxon( Taxon taxon );

    /**
     * @param taxon
     * @return
     */
    public Collection<GeneCoexpressionAnalysis> findByTaxon( Taxon taxon );
}
