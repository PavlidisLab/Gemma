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
import java.util.Map;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.util.AlreadyExistsInSystemException;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author joseph
 * @version $Id$
 */
public class DesignElementDataVectorDaoImplTest extends BaseSpringContextTest {
    DesignElementDataVectorDao designElementDataVectorDao;
    ExpressionExperimentService expressionExperimentService;
    ExpressionExperiment newee = null;
    DesignElementDataVector dedv;
    protected AbstractGeoService geoService;

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
        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );

    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
    }

    /**
     * @deprecated the method under test is deprecated.
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

    protected void onTearDownAfterTransaction() throws Exception {
        super.onTearDownAfterTransaction();

        if ( newee != null && newee.getId() != null ) {
            expressionExperimentService.delete( newee );
        }

    }

    @SuppressWarnings("unchecked")
    public void testGetGeneCoexpressionPattern() {

        endTransaction();
        try {
            String path = ConfigUtils.getString( "gemma.home" );
            assert path != null;
            geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                    + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "gse432Short" ) );
            Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                    "GSE432", false, true, false );
            newee = results.iterator().next();
        } catch ( AlreadyExistsInSystemException e ) {
            newee = ( ExpressionExperiment ) e.getData();
        }

        DesignElementDataVectorService dedvs = ( DesignElementDataVectorService ) this
                .getBean( "designElementDataVectorService" );

        Collection<Gene> genes = new HashSet<Gene>();
        Gene g = this.getTestPeristentGene();
        genes.add( g );

        Collection<ExpressionExperiment> ees = new HashSet<ExpressionExperiment>();
        ees.add( newee );

        Map<DesignElementDataVector, Collection<Gene>> geneCoexpressionPattern = dedvs.getVectors( ees, genes );

        assertNotNull( geneCoexpressionPattern );

    }
}