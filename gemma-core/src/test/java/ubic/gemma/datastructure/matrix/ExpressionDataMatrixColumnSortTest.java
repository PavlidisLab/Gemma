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

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorService;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author paul
 * @version $Id$
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

        Collection<ExperimentalFactor> factors = new HashSet<ExperimentalFactor>();
        for ( int i = 0; i < 5; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setType( FactorType.CATEGORICAL );
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

                if ( j == 2 && i != 4 ) {
                    fv.setValue( "control_group" );
                }

                if ( i == 4 ) {
                    ef.setType( FactorType.CONTINUOUS );
                    Measurement m = Measurement.Factory.newInstance();
                    m.setId( ( long ) j * ( i + 1 ) );
                    m.setValue( j + ".00" );
                    m.setRepresentation( PrimitiveType.DOUBLE );
                    fv.setMeasurement( m );
                }
            }

            factors.add( ef );
        }

        Random random = new Random();
        for ( int i = 0; i < 100; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setName( "ba" + i );
            ba.setId( ( long ) i );

            bad.getBioAssays().add( ba );

            BioMaterial bm = BioMaterial.Factory.newInstance();
            bm.setId( ( long ) i );
            bm.setName( "bm" + i );
            ba.setSampleUsed( bm );

            for ( ExperimentalFactor ef : factors ) {

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

                if ( toUse != null ) bm.getFactorValues().add( toUse );
                // log.info( ba + " -> " + bm + " -> " + ef + " -> " + toUse );
            }
        }

        EmptyExpressionMatrix mat = new EmptyExpressionMatrix( bad );

        assertEquals( 100, mat.columns() );

        List<BioMaterial> ordered = ExpressionDataMatrixColumnSort.orderByExperimentalDesign( mat );

        assertEquals( 100, ordered.size() );

        // for ( BioMaterial bioMaterial : ordered ) {
        // log.info( bioMaterial + " .... " + StringUtils.join( bioMaterial.getFactorValues(), "  --- " ) );
        // }

    }

}
