/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

package ubic.gemma.model.association.coexpression;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author klc
 * @version $Id$
 */
public class Gene2GeneCoexpressionServiceTest extends BaseSpringContextTest {

    @Autowired
    private Gene2GeneCoexpressionService g2gCoexpressionS;

    @Autowired
    private GeneCoexpressionAnalysisService analysisS;

    @Autowired
    private ProtocolService protocolS;

    @Autowired
    private GeneService geneS;

    @Autowired
    private TaxonService taxonS;

    private GeneCoexpressionAnalysis analysis;
    private Gene firstGene;

    @Before
    public void setup()  {

        analysis = GeneCoexpressionAnalysis.Factory.newInstance();
        // analysis.setAnalyzedInvestigation( new HashSet<Investigation>( toUseEE ) );
        analysis.setDescription( "test" );

        analysis.setName( "Test: " + new Date() );

        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Stored Gene2GeneCoexpressions" );
        protocol.setDescription( "Test" );
        protocol = protocolS.findOrCreate( protocol );

        analysis.setProtocol( protocol );
        analysis.setEnabled( true );
        analysis = analysisS.create( analysis );

        Taxon mouseTaxon = taxonS.findByCommonName( "mouse" );

        firstGene = Gene.Factory.newInstance();
        firstGene.setName( "test_gene2geneCoexpression" );
        firstGene.setTaxon( mouseTaxon );
        firstGene = geneS.create( firstGene );

        Gene secondGene = Gene.Factory.newInstance();
        secondGene.setName( "test_gene2geneCoexpression2" );
        secondGene = geneS.create( secondGene );

        Gene2GeneCoexpression g2gCoexpression = MouseGeneCoExpression.Factory.newInstance();
        g2gCoexpression.setSourceAnalysis( analysis );
        g2gCoexpression.setFirstGene( firstGene );
        g2gCoexpression.setSecondGene( secondGene );
        g2gCoexpression.setNumDataSets( 3 );

        /*
         * This is just filler
         */
        g2gCoexpression.setEffect( 0.9 );
        g2gCoexpression.setPvalue( 0.0001 );
        g2gCoexpression.setDatasetsSupportingVector( new byte[] { 2, 3, 8 } );
        g2gCoexpression.setDatasetsTestedVector( new byte[] { 2, 9, 8 } );
        g2gCoexpression.setSpecificityVector( new byte[] { 2, 3, 8 } );

        g2gCoexpressionS.create( g2gCoexpression );
    }

    @Test
    public void testFindCoexpressionRelationships() {

        Collection<Gene2GeneCoexpression> results = g2gCoexpressionS.findCoexpressionRelationships( firstGene, 3, 100,
                analysis );
        assertEquals( 1, results.size() );

    }

}
