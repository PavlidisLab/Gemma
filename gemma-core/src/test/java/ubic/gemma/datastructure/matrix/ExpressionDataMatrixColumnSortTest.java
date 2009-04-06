/*
 * The Gemma project
 * 
 * Copyright (c) 2007-2008 University of British Columbia
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
import java.util.HashSet;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionDataMatrixColumnSortTest extends BaseSpringContextTest {
    private static Log log = LogFactory.getLog( ExpressionDataMatrixColumnSortTest.class );

    // ExpressionExperimentService expressionExperimentService;
    // DesignElementDataVectorService designElementDataVectorService;
    // ArrayDesignService adService;
    // protected AbstractGeoService geoService;
    // ExpressionDataDoubleMatrix matrix;
    //
    // /**
    // * Test method for
    // * {@link
    // ubic.gemma.datastructure.matrix.ExpressionDataMatrixColumnSort#orderByExperimentalDesign(ubic.gemma.datastructure.matrix.ExpressionDataMatrix)}
    // * .
    // */
    // @SuppressWarnings("unchecked")
    // public void testOrderByExperimentalDesign() throws Exception {
    // endTransaction();
    // expressionExperimentService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
    // adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
    // designElementDataVectorService = ( DesignElementDataVectorService ) this
    // .getBean( "designElementDataVectorService" );
    // geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
    // ExpressionExperiment newee = this.expressionExperimentService.findByShortName( "GSE611" );
    // if ( newee != null ) {
    // expressionExperimentService.delete( newee );
    // }
    // String path = ConfigUtils.getString( "gemma.home" );
    // assert path != null;
    // geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
    // + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT + "GSE611Short" ) );
    // Collection<ExpressionExperiment> results = ( Collection<ExpressionExperiment> ) geoService.fetchAndLoad(
    // "GSE611", false, true, false, false, true );
    // newee = results.iterator().next();
    //
    // expressionExperimentService.thaw( newee );
    // // make sure we really thaw them, so we can get the design element sequences.
    // Collection<RawExpressionDataVector> designElementDataVectors = newee.getRawExpressionDataVectors();
    // designElementDataVectorService.thaw( designElementDataVectors );
    //
    // ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( designElementDataVectors );
    // matrix = builder.getPreferredData();
    // List<BioMaterial> orderByExperimentalDesign = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( matrix );
    // assertEquals( 4, orderByExperimentalDesign.size() );
    // }

    public void testOrderByExperimentalDesignB() throws Exception {

        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();

        /*
         * Five factors. Factor4 is a measurmeent.
         */

        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        for ( int i = 0; i < 5; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setName( "factor" + i );
            if ( i == 4 ) {
                ef.setName( "mfact" + i );
            }
            ef.setId( ( long ) i );

            for ( int j = 0; j < 3; j++ ) {
                FactorValue fv = FactorValue.Factory.newInstance();
                fv.setValue( "fv" + j * ( i + 1 ) );
                fv.setId( ( long ) j * ( i + 1 ) );
                fv.setExperimentalFactor( ef );
                ef.getFactorValues().add( fv );

                if ( i == 4 ) {
                    Measurement m = Measurement.Factory.newInstance();
                    m.setId( ( long ) j * ( i + 1 ) );
                    m.setValue( j + ".00" );
                    m.setRepresentation( PrimitiveType.DOUBLE );
                    fv.setMeasurement( m );
                }
            }

            factors.add( ef );
        }

        for ( int i = 0; i < 100; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setName( "ba" + i );
            ba.setId( ( long ) i );

            bad.getBioAssays().add( ba );

            BioMaterial bm = BioMaterial.Factory.newInstance();
            bm.setId( ( long ) i );
            bm.setName( "bm" + i );
            ba.getSamplesUsed().add( bm );

            for ( ExperimentalFactor ef : factors ) {

                /*
                 * Note: if we use 4, then some of the biomaterials will not have a factorvalue for each factor. This is
                 * realistic. Use 3 to fill it in completely.
                 */
                int k = RandomUtils.nextInt( 4 );
                int m = 0;
                FactorValue toUse = null;
                for ( FactorValue fv : ef.getFactorValues() ) {
                    if ( m == k ) {
                        toUse = fv;
                        break;
                    }
                    m++;
                }

                if ( toUse != null ) bm.getFactorValues().add( toUse );
                // log.info( ba + " -> " + bm + " -> " + ef + " -> " + toUse );
            }
        }

        EmptyExpressionMatrix mat = new EmptyExpressionMatrix( bad );

        List<BioMaterial> ordered = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat );

        assertEquals( 100, ordered.size() );

    }

    // public void testOrderByExperimentalDesignC() throws Exception {
    //
    // BioAssayDimensionService bads = ( BioAssayDimensionService ) this.getBean( "bioAssayDimensionService" );
    // for ( long i = 1; i < 100; i++ ) {
    // BioAssayDimension bad = bads.load( i );
    // if ( bad != null ) {
    // log.info( bad.getId() );
    // EmptyExpressionMatrix mat = new EmptyExpressionMatrix( bad );
    // List<BioMaterial> ordered = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat );
    // assertTrue( ordered.size() > 0 );
    // }
    // }
    // }

}
