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

import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * This test relies on having genes in the system ... pretty useless.
 * 
 * @author keshav
 * @author paul
 * @version $Id$
 */
public class DifferentialExpressionAnalysisServiceTest extends BaseSpringContextTest {

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService = null;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService = null;

    @Autowired
    private GeneDao geneDao = null;

    private String officialSymbol = "ACAA1";

    /**
     * 
     */
    @Test
    public void testFind() {

        Collection<Gene> genes = geneDao.findByOfficalSymbol( officialSymbol );

        if ( genes == null || genes.isEmpty() ) {
            log.error( "Problems obtaining genes. Skipping test ..." );
            return;
        }

        for ( Gene g : genes ) {
            Collection<BioAssaySet> experiments = differentialExpressionAnalysisService.findExperimentsWithAnalyses( g );
            assertNotNull( experiments );
            log.info( experiments.size() );
        }

    }

    /**
     * 
     */
    @Test
    public void testFindResults() {
        Collection<Gene> genes = geneDao.findByOfficalSymbol( officialSymbol );

        if ( genes == null || genes.isEmpty() ) {
            log.error( "Problems obtaining genes. Skipping test ..." );
            return;
        }

        for ( Gene g : genes ) {
            Collection<BioAssaySet> experiments = differentialExpressionAnalysisService.findExperimentsWithAnalyses( g );

            log.info( "num experiments for " + g.getOfficialSymbol() + ": " + experiments.size() );

            Map<BioAssaySet, List<DifferentialExpressionAnalysisResult>> results = differentialExpressionResultService
                    .find( g, experiments );

            for ( BioAssaySet e : results.keySet() ) {

                log.debug( "num results for gene " + g.getOfficialSymbol() + " and experiment " + e.getName() + ": "
                        + results.size() );

                assertNotNull( results );

                for ( DifferentialExpressionAnalysisResult r : results.get( e ) ) {
                    double pval = r.getPvalue();
                    log.debug( "pval: " + pval );
                }
            }
        }
    }

}
