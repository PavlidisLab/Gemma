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
package ubic.gemma.analysis.expression.diff;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao;
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

    private DifferentialExpressionResultDao differentialExpressionResultDao = null;

    private GeneDao geneDao = null;

    private String officialSymbol = "ACAA1";

    /**
     * @param differentialExpressionResultDao the differentialExpressionResultDao to set
     */
    public void setDifferentialExpressionResultDao( DifferentialExpressionResultDao differentialExpressionResultDao ) {
        this.differentialExpressionResultDao = differentialExpressionResultDao;
    }

    /**
     * 
     */
    public void testFind() {

        Collection<Gene> genes = geneDao.findByOfficalSymbol( officialSymbol );

        if ( genes == null || genes.isEmpty() ) {
            log.error( "Problems obtaining genes. Skipping test ..." );
            return;
        }

        for ( Gene g : genes ) {
            Collection<ExpressionExperiment> experiments = differentialExpressionAnalysisDao
                    .findExperimentsWithAnalyses( g );
            assertNotNull( experiments );
            log.info( experiments.size() );
        }

    }

    /**
     * 
     */
    public void testFindResults() {
        Collection<Gene> genes = geneDao.findByOfficalSymbol( officialSymbol );

        if ( genes == null || genes.isEmpty() ) {
            log.error( "Problems obtaining genes. Skipping test ..." );
            return;
        }

        for ( Gene g : genes ) {
            Collection<ExpressionExperiment> experiments = differentialExpressionAnalysisDao
                    .findExperimentsWithAnalyses( g );

            log.info( "num experiments for " + g.getOfficialSymbol() + ": " + experiments.size() );

            Map<ExpressionExperiment, Collection<ProbeAnalysisResult>> results = differentialExpressionResultDao.find(
                    g, experiments );

            for ( ExpressionExperiment e : results.keySet() ) {

                log.debug( "num results for gene " + g.getOfficialSymbol() + " and experiment " + e.getName() + ": "
                        + results.size() );

                assertNotNull( results );

                for ( ProbeAnalysisResult r : results.get( e ) ) {
                    double pval = r.getPvalue();
                    log.debug( "pval: " + pval );
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
