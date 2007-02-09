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

import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.gene.GeneService;
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

    //fixme:  I've commented out this test as it needs the database to be in a correct state, have genes, have ee, and i'm not even sure what the output should be. 
    //This is another situation where a test db dump would be really handy. 
    
//    public void testGetGeneCoexpressionPattern() {
//        DesignElementDataVectorService dedvs = ( DesignElementDataVectorService ) this
//                .getBean( "designElementDataVectorService" );
//
//        ExpressionExperimentService eeSrv = ( ExpressionExperimentService ) this
//                .getBean( "expressionExperimentService" );
//        GeneService geneSrv = ( GeneService ) this.getBean( "geneService" );
//        TaxonService taxonSrv = ( TaxonService ) this.getBean( "taxonService" );
//        QuantitationTypeService qtSrv = (QuantitationTypeService) this.getBean( "quantitationTypeService" );
//
//        Taxon mouse = taxonSrv.findByCommonName( "mouse" );
//
//        //Collection genes = geneSrv.getGenesByTaxon( mouse );
//        Collection genes;
//        
//        log.debug( "gene collection size: " + genes.size() );
//        Collection expressionExperimentService = eeSrv.getByTaxon( mouse );
//       // Collection qts = eeSrv.getQuantitationTypes( ( ExpressionExperiment ) expressionExperimentService.iterator().next() );
//        QuantitationType qt =  QuantitationType.Factory.newInstance();
//        //qt.setId( (long) 1 );
//        qt.setName( "VALUE" );
//        qt.setScale(ScaleType.LINEAR);
//        qt.setRepresentation(PrimitiveType.DOUBLE);
//        qt.setGeneralType(GeneralType.QUANTITATIVE);
//        qt.setType(StandardQuantitationType.MEASUREDSIGNAL);
//        qt = qtSrv.find(qt);        
//        dedvs.getGeneCoexpressionPattern( expressionExperimentService, genes, qt );
//
//    }

    public void testGetGenes() {
        // this test is not activated because it is not guaranteed that we will
        // have designElementDataVectors in a fresh database.
        // TODO a more valid test should be written.
        /*
         * Collection<DesignElementDataVector> dataVectors = new ArrayList<DesignElementDataVector>(); dedv =
         * DesignElementDataVector.Factory.newInstance(); dedv.setId( (long) 250351 ); dataVectors.add( dedv ); dedv =
         * DesignElementDataVector.Factory.newInstance(); dedv.setId( (long) 250357 ); dataVectors.add( dedv ); dedv =
         * DesignElementDataVector.Factory.newInstance(); dedv.setId( (long) 250360 ); dataVectors.add( dedv ); Map m =
         * designElementDataVectorDao.getGenes( dataVectors ); assertNotNull(m);
         */
    }
}