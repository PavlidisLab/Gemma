/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.analysis.diff;

import java.util.Collection;

import ubic.gemma.model.analysis.DifferentialExpressionAnalysisDao;
import ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author keshav
 * @author paul
 * @version $Id$
 */
public class DifferentialExpressionAnalysisDaoImplTest extends BaseSpringContextTest {

    /* to load data, use mini-gemma */

    private DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao = null;

    private GeneDao geneDao = null;

    private String officialSymbol = "ACAA1";

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public void testFind() {

        Collection<Gene> genes = geneDao.findByOfficalSymbol( officialSymbol );

        if ( genes == null || genes.isEmpty() ) {
            log.error( "Problems obtaining genes. Skipping test ..." );
            return;
        }

        for ( Gene g : genes ) {
            Collection<ExpressionExperiment> experiments = differentialExpressionAnalysisDao.find( g );
            assertNotNull( experiments );
            log.info( experiments.size() );
        }

    }

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    public void testFindResults() {
        Collection<Gene> genes = geneDao.findByOfficalSymbol( officialSymbol );

        if ( genes == null || genes.isEmpty() ) {
            log.error( "Problems obtaining genes. Skipping test ..." );
            return;
        }

        for ( Gene g : genes ) {
            Collection<ExpressionExperiment> experiments = differentialExpressionAnalysisDao.find( g );
            log.info( "num experiments for " + g.getOfficialSymbol() + ": " + experiments.size() );
            for ( ExpressionExperiment e : experiments ) {
                Collection<DifferentialExpressionAnalysisResult> results = differentialExpressionAnalysisDao
                        .find( g, e );
                log.info( "num results for gene " + g.getOfficialSymbol() + " and experiment " + e.getName() + ": "
                        + results.size() );

                assertNotNull( results );

                for ( DifferentialExpressionAnalysisResult r : results ) {
                    double pval = r.getPvalue();
                    log.info( "pval: " + pval );
                }
            }
        }
    }

    /**
     * @param differentialExpressionAnalysisDao
     */
    public void setDifferentialExpressionAnalysisDao(
            DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao ) {
        this.differentialExpressionAnalysisDao = differentialExpressionAnalysisDao;
    }

    /**
     * @param geneDao
     */
    public void setGeneDao( GeneDao geneDao ) {
        this.geneDao = geneDao;
    }
}
