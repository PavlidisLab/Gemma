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

import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;

import ubic.gemma.model.analysis.expression.coexpression.CoexpressionCollectionValueObject;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.protocol.ProtocolService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author klc
 */
public class Gene2GeneCoexpressionServiceTest extends BaseSpringContextTest {

    private Gene2GeneCoexpressionService g2gCoexpressionS;
    private ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService analysisS;
    private ProtocolService protocolS;
    private GeneService geneS;
    private TaxonService taxonS;

    private GeneCoexpressionAnalysis analysis;
    private Gene firstGene;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        this.endTransaction();

        g2gCoexpressionS = ( Gene2GeneCoexpressionService ) this.getBean( "gene2GeneCoexpressionService" );
        analysisS = ( GeneCoexpressionAnalysisService ) this.getBean( "geneCoexpressionAnalysisService" );
        protocolS = ( ProtocolService ) this.getBean( "protocolService" );
        geneS = ( GeneService ) this.getBean( "geneService" );
        taxonS = ( TaxonService ) this.getBean( "taxonService" );

        analysis = GeneCoexpressionAnalysis.Factory.newInstance();
        // analysis.setAnalyzedInvestigation( new HashSet<Investigation>( toUseEE ) );
        analysis.setDescription( "test" );

        Calendar cal = new GregorianCalendar();

        analysis.setName( "Test: " + cal.get( Calendar.YEAR ) + " " + cal.get( Calendar.MONTH ) + " "
                + cal.get( Calendar.DAY_OF_MONTH ) + " " + cal.get( Calendar.HOUR_OF_DAY ) + ":"
                + cal.get( Calendar.MINUTE ) );

        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Stored Gene2GeneCoexpressions" );
        protocol.setDescription( "Test" );
        protocol = protocolS.findOrCreate( protocol );

        analysis.setProtocol( protocol );
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
        g2gCoexpression.setDatasetsSupportingVector( new byte[] { 2, 3, 8 } );
        g2gCoexpression.setDatasetsTestedVector( new byte[] { 2, 9, 8 } );
        g2gCoexpressionS.create( g2gCoexpression );
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();

    }

    public void testFindCoexpressionRelationships() {

        Collection results = g2gCoexpressionS.findCoexpressionRelationships( firstGene, 3, 100 );
        assertEquals( 1, results.size() );

    }

    // Notice this test requires that the gene2genecoepressionGeneratorCLI has be run with analysis
    // name = checkResult, brain for the expression experiment keyword, grin1 as the only gene in the gene list
    // and mouse as the taxon

    // public void testValidResults() {
    //       
    // Collection<ExpressionExperiment> ees = searchS.compassExpressionSearch( "brain" );
    // Collection<ExpressionExperiment> allMouseEEs = new HashSet<ExpressionExperiment>();
    //       
    // //Filter out all the ee that are not of mouse taxon
    // for ( ExpressionExperiment ee : ees ) {
    // Taxon t = eeS.getTaxon( ee.getId() );
    // if ( t.getCommonName().equalsIgnoreCase( "mouse" ))
    // allMouseEEs.add( ee );
    // }
    //       
    // Collection grins = geneS.findByOfficialSymbol( "grin1" );
    //        
    // Gene grin1 = null;
    // for(Object obj : grins){
    // Gene g = (Gene) obj;
    // if (g.getTaxon().getCommonName().equalsIgnoreCase( "mouse" )){
    // grin1 = g;
    // break;
    // }
    // }
    // CoexpressionCollectionValueObject coexpressions = ( CoexpressionCollectionValueObject ) geneS
    // .getCoexpressedGenes(grin1 , null, 4 );
    //  
    // Analysis check = analysisS.findByName( "resultCheck31" );
    // Collection geneCoexpressions = this.g2gCoexpressionS.findCoexpressionRelationships( grin1, check, 4 );
    //        
    // assertTrue( validate(geneCoexpressions, coexpressions));
    // }
    //    
    //    
    private boolean validate( Collection g2g, CoexpressionCollectionValueObject p2p ) {

        int actualLinkCount = p2p.getKnownGeneCoexpression().getNegativeStringencyLinkCount()
                + p2p.getKnownGeneCoexpression().getPositiveStringencyLinkCount();

        if ( g2g.size() == actualLinkCount ) return true;

        return false;
    }

}
