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
package ubic.gemma.core.datastructure.matrix;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalFactorService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * @author paul
 */
public class ExpressionDataMatrixColumnSortTest extends BaseSpringContextTest {

    @Autowired
    ExperimentalFactorService experimentalFactorService;

    @Test
    public void testOrderByExperimentalDesignB() {

        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();

        /*
         * Five factors. Factor4 is a measurmeent.
         */

        Collection<ExperimentalFactor> categoricalfactors = new HashSet<>();
        for ( int i = 0; i < 4; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setType( FactorType.CATEGORICAL );
            ef.setName( "factor" + i );
            if ( i == 4 ) {
                ef.setName( "mfact" + i );
            }
            ef.setId( ( long ) i );

            // make three factor values for each factor (other than the measurement)
            for ( int j = 0; j < 3; j++ ) {
                FactorValue fv = FactorValue.Factory.newInstance();
                fv.setValue( "fv" + ( j + 1 ) * ( i + 1 ) + "x" + ( i + 1 ) );
                fv.setId( ( long ) ( j + 1 ) * ( i + 1 ) );
                fv.setExperimentalFactor( ef );

                if ( j == 2 ) {
                    fv.setValue( "control_group" );
                }
                // log.info( fv );
                ef.getFactorValues().add( fv );
            }
        }

        ExperimentalFactor continuousFactor = ExperimentalFactor.Factory.newInstance();
        continuousFactor.setType( FactorType.CONTINUOUS );
        continuousFactor.setName( "measurement" );
        continuousFactor.setId( ( long ) 400 );

        Random random = new Random();
        Collection<BioMaterial> seen = new HashSet<>();
        for ( int i = 0; i < 100; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setName( "ba" + i );
            ba.setId( ( long ) i );

            bad.getBioAssays().add( ba );

            BioMaterial bm = BioMaterial.Factory.newInstance();
            bm.setId( ( long ) i );
            bm.setName( "bm" + i );
            ba.setSampleUsed( bm );

            if ( seen.contains( bm ) ) throw new IllegalStateException( "Duplicate biomaterial" );

            seen.add( bm );

            /// add the continuous factor
            FactorValue contfv = FactorValue.Factory.newInstance();
            contfv.setId( i + 1924904L );
            contfv.setExperimentalFactor( continuousFactor );
            Measurement measurement = Measurement.Factory.newInstance();
            measurement.setId( ( long ) 4289 * ( i + 1 ) );
            measurement.setValue( "" + random.nextDouble() );
            measurement.setRepresentation( PrimitiveType.DOUBLE );
            contfv.setMeasurement( measurement );
            continuousFactor.getFactorValues().add( contfv );
            bm.getFactorValues().add( contfv );

            // Add the other factors.
            for ( ExperimentalFactor ef : categoricalfactors ) {

                /*
                 * Note: if we use 4, then some of the biomaterials will not have a factorvalue for each factor. This is
                 * realistic. Use 3 to fill it in completely.
                 */
                int k = random.nextInt( 4 );
                int m = 0;
                FactorValue toUse = null;
                for ( FactorValue fv : ef.getFactorValues() ) {
                    if ( m == k ) {
                        toUse = fv;
                        break;
                    }
                    m++;
                }

                if ( toUse != null ) {
//                    Collection<FactorValue> fvs = bm.getFactorValues();
//                    for ( FactorValue fv : fvs ) {
//                        if ( fv.getExperimentalFactor().equals( ef ) ) {
//                            throw new IllegalStateException( bm + " already has a factor value for " + ef );
//                        }
//                    }

                    bm.getFactorValues().add( toUse );
                    // log.info( ba + " -> " + bm + " -> " + ef + " -> " + toUse );}
                }
            }
        }

        // check that each biomaterial has only one fator value for each experimental factor
//        for ( BioAssay ba : bad.getBioAssays() ) {
//            BioMaterial bm = ba.getSampleUsed();
//            for ( ExperimentalFactor ef : categoricalfactors ) {
//                int j = 0;
//                Collection<FactorValue> fvs = bm.getFactorValues();
//                for ( FactorValue fv : fvs ) {
//                    if ( fv.getExperimentalFactor().equals( ef ) ) {
//                        j++;
//                    }
//                }
//                assertEquals( bm + " had more than one factorvalue for " + ef, 1, j );
//            }
//        }

        EmptyExpressionMatrix mat = new EmptyExpressionMatrix( bad );

        assertEquals( 100, mat.columns() );

        List<BioMaterial> ordered = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat );

        assertEquals( 100, ordered.size() );

        // for ( BioMaterial bioMaterial : ordered ) {
        // log.info( bioMaterial + " .... " + StringUtils.join( bioMaterial.getFactorValues(), "  --- " ) );
        // }

    }

}
