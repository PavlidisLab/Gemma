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
package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.HashSet;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author joseph
 * @version $Id$
 */
public class DesignElementDataVectorDaoImplTest extends BaseSpringContextTest {
    DesignElementDataVectorDao designElementDataVectorDao;

    DesignElementDataVector dedv;

    /**
     * @param designElementDataVectorDao the designElementDataVectorDao to set
     */
    public void setDesignElementDataVectorDao( DesignElementDataVectorDao designElementDataVectorDao ) {
        this.designElementDataVectorDao = designElementDataVectorDao;
    }

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        dedv = DesignElementDataVector.Factory.newInstance();
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
    }

    /**
     * 
     *
     */
    public void testQueryByGeneSymbolAndSpecies() {
        designElementDataVectorDao = ( DesignElementDataVectorDao ) this.getBean( "designElementDataVectorDao" );
        Collection<ExpressionExperiment> expressionExperiments = new HashSet<ExpressionExperiment>();
        for ( long i = 0; i < 3; i++ ) {
            ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
            ee.setId( i );
            ee.setName( "test_ee_" + i + " from DesignElementDataVectorDaoTest" );
            expressionExperiments.add( ee );
        }

        Collection objects = designElementDataVectorDao.queryByGeneSymbolAndSpecies( "GRIN1", "mouse",
                expressionExperiments );
        assertNotNull( objects );
    }

    public void testGetGenes() {
        designElementDataVectorDao = ( DesignElementDataVectorDao ) this.getBean( "designElementDataVectorDao" );
        DesignElementDataVector dedv = DesignElementDataVector.Factory.newInstance();
        dedv.setId( ( long ) 1 );
        Collection objects = designElementDataVectorDao.getGenes( dedv );
        assertNotNull( objects );
    }

    public void testGetGenesById() {
        designElementDataVectorDao = ( DesignElementDataVectorDao ) this.getBean( "designElementDataVectorDao" );
        DesignElementDataVector dedv = DesignElementDataVector.Factory.newInstance();
        dedv.setId( ( long ) 1 );
        Collection objects = designElementDataVectorDao.getGenesById( ( long ) 1 );
        assertNotNull( objects );
    }

    // fixme: I've commented out this test as it needs the database to be in a correct state, have genes, have ee, and
    // i'm not even sure what the output should be.
    // This is another situation where a test db dump would be really handy.

    // public void testGetGeneCoexpressionPattern() {
    // DesignElementDataVectorService dedvs = ( DesignElementDataVectorService ) this
    // .getBean( "designElementDataVectorService" );
    //
    // ExpressionExperimentService eeSrv = ( ExpressionExperimentService ) this
    // .getBean( "expressionExperimentService" );
    // GeneService geneSrv = ( GeneService ) this.getBean( "geneService" );
    // TaxonService taxonSrv = ( TaxonService ) this.getBean( "taxonService" );
    // QuantitationTypeService qtSrv = (QuantitationTypeService) this.getBean( "quantitationTypeService" );
    //
    // Taxon mouse = taxonSrv.findByCommonName( "mouse" );
    //
    // //Collection genes = geneSrv.getGenesByTaxon( mouse );
    // Collection genes;
    //        
    // log.debug( "gene collection size: " + genes.size() );
    // Collection expressionExperimentService = eeSrv.getByTaxon( mouse );
    // // Collection qts = eeSrv.getQuantitationTypes( ( ExpressionExperiment )
    // expressionExperimentService.iterator().next() );
    // QuantitationType qt = QuantitationType.Factory.newInstance();
    // //qt.setId( (long) 1 );
    // qt.setName( "VALUE" );
    // qt.setScale(ScaleType.LINEAR);
    // qt.setRepresentation(PrimitiveType.DOUBLE);
    // qt.setGeneralType(GeneralType.QUANTITATIVE);
    // qt.setType(StandardQuantitationType.MEASUREDSIGNAL);
    // qt = qtSrv.find(qt);
    // dedvs.getGeneCoexpressionPattern( expressionExperimentService, genes, qt );
    //
    // }

}