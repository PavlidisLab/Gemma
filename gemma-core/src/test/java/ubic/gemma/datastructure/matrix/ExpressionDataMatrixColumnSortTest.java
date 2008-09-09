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
package ubic.gemma.datastructure.matrix;

import java.util.Collection;
import java.util.List;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionDataMatrixColumnSortTest extends BaseSpringContextTest {
    ExpressionExperimentService expressionExperimentService;
    DesignElementDataVectorService designElementDataVectorService;
    ArrayDesignService adService;
    protected AbstractGeoService geoService;
    ExpressionDataDoubleMatrix matrix;

    // /**
    // * Test method for
    // * {@link
    // ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort#orderByName(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)}.
    // */
    // public void testOrderByName() throws Exception {
    // assertTrue( true );
    // }

    /**
     * Test method for
     * {@link ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort#orderByExperimentalDesign(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)}.
     */
    @SuppressWarnings("unchecked")
    public void testOrderByExperimentalDesign() throws Exception {
        expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
        adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        designElementDataVectorService = ( DesignElementDataVectorService ) this
                .getBean( "designElementDataVectorService" );
        geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
        ExpressionExperiment newee = this.expressionExperimentService.findByShortName( "GSE611" );
        if ( newee != null ) {
            expressionExperimentService.delete( newee );
        }
        String path = ConfigUtils.getString( "gemma.home" );
        assert path != null;
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "GSE611Short" ) );
        Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
                "GSE611", false, true, false, false, true );
        newee = results.iterator().next();

        expressionExperimentService.thaw( newee );
        // make sure we really thaw them, so we can get the design element sequences.

        Collection<RawExpressionDataVector> designElementDataVectors = newee.getRawExpressionDataVectors();
        designElementDataVectorService.thaw( designElementDataVectors );

        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( designElementDataVectors );
        matrix = builder.getPreferredData();
        List<BioMaterial> orderByExperimentalDesign = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( matrix );
        assertEquals( 4, orderByExperimentalDesign.size() );
    }

}
