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
package ubic.gemma.datastructure.matrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.loader.expression.arrayDesign.Reporter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.testing.PersistentDummyObjectHelper;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MatrixConversionTest extends TestCase {

    private static Log log = LogFactory.getLog( MatrixConversionTest.class.getName() );
    private static final int NUM_BIOMATERIALS = 40;
    private static final int NUM_CS = 200;

    /**
     * Creates an ugly (but not unusual) situation where there are two bioassay dimensions with different sizes,
     * referring to the same set of biomaterials.
     * 
     * @return
     */
    public Collection<DesignElementDataVector> getDesignElementDataVectors( Collection<QuantitationType> quantTypes ) {
        Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "junk" );
        List<CompositeSequence> sequences = getCompositeSequences( ad );

        ArrayDesign adb = ArrayDesign.Factory.newInstance();
        adb.setName( "bjunk" );
        List<CompositeSequence> sequencesb = getCompositeSequences( ad );

        List<BioMaterial> bioMaterials = getBioMaterials(); // resused

        for ( QuantitationType quantType : quantTypes ) {

            /*
             * Create two bioassay dimension which overlap; "A" does not use all the biomaterials.
             */
            BioAssayDimension baDimA = BioAssayDimension.Factory.newInstance();
            Iterator<BioMaterial> bmita = bioMaterials.iterator();
            for ( long i = 0; i < NUM_BIOMATERIALS - 20; i++ ) {
                BioAssay ba = ubic.gemma.model.expression.bioAssay.BioAssay.Factory.newInstance();
                ba.setName( RandomStringUtils.randomNumeric( 5 ) + "_testbioassay" );
                ba.setSampleUsed( bmita.next() );
                ba.setArrayDesignUsed( ad );
                ba.setId( i );
                baDimA.getBioAssays().add( ba );
            }
            baDimA.setName( RandomStringUtils.randomAlphanumeric( 10 ) );

            BioAssayDimension baDimB = BioAssayDimension.Factory.newInstance();
            Iterator<BioMaterial> bmitb = bioMaterials.iterator();
            for ( long i = 0; i < NUM_BIOMATERIALS; i++ ) {
                BioAssay ba = ubic.gemma.model.expression.bioAssay.BioAssay.Factory.newInstance();
                ba.setName( RandomStringUtils.randomNumeric( 15 ) + "_testbioassay" );
                ba.setSampleUsed( bmitb.next() );
                ba.setArrayDesignUsed( adb );
                ba.setId( i + 20 );
                baDimB.getBioAssays().add( ba );
            }
            baDimB.setName( RandomStringUtils.randomAlphanumeric( 10 ) );

            // bio.a gets cs 0-99, bio.b gets 100-199.
            long j = 0;
            for ( ; j < NUM_CS - 100; j++ ) {
                DesignElementDataVector vector = RawExpressionDataVector.Factory.newInstance();
                double[] data = new double[baDimA.getBioAssays().size()];
                for ( int k = 0; k < data.length; k++ ) {
                    data[k] = k;
                }
                ByteArrayConverter bconverter = new ByteArrayConverter();
                byte[] bdata = bconverter.doubleArrayToBytes( data );
                vector.setData( bdata );

                CompositeSequence cs = sequencesb.get( ( int ) j );
                vector.setDesignElement( cs );
                vector.setQuantitationType( quantType );
                vector.setBioAssayDimension( baDimA );
                vectors.add( vector );
            }

            for ( ; j < NUM_CS; j++ ) {
                DesignElementDataVector vector = RawExpressionDataVector.Factory.newInstance();
                double[] data = new double[baDimB.getBioAssays().size()];
                for ( int k = 0; k < data.length; k++ ) {
                    data[k] = k;
                }
                ByteArrayConverter bconverter = new ByteArrayConverter();
                byte[] bdata = bconverter.doubleArrayToBytes( data );
                vector.setData( bdata );

                CompositeSequence cs = sequences.get( ( int ) j );
                vector.setDesignElement( cs );
                vector.setQuantitationType( quantType );
                vector.setBioAssayDimension( baDimB );
                vectors.add( vector );
            }
        }
        return vectors;
    }

    public final void testColumnMapping() {
        Collection<QuantitationType> quantTypes = new HashSet<QuantitationType>();
        for ( int quantitationTypeNum = 0; quantitationTypeNum < 2; ) {
            QuantitationType quantType = PersistentDummyObjectHelper.getTestNonPersistentQuantitationType();
            quantType.setId( ( long ) quantitationTypeNum );
            quantTypes.add( quantType );
            break;
        }

        Collection<DesignElementDataVector> vectors = getDesignElementDataVectors( quantTypes );
        ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( vectors );
        log.debug( vectors.size() + " vectors" );

        assertEquals( NUM_CS, mat.rows() );
        assertEquals( NUM_BIOMATERIALS, mat.columns() );

        // System.err.print( "--" );
        // for ( int i = 0; i < mat.columns(); i++ ) {
        // System.err.print( "\t" + mat.getBioMaterialForColumn( i ) );
        // }
        // System.err.print( "\n" );
        for ( int j = 0; j < mat.rows(); j++ ) {
            // System.err.print( mat.getRowElement( j ) );
            for ( int i = 0; i < mat.columns(); i++ ) {
                Double r = mat.get( j, i );
                assertNotNull( "No value for at index " + i, r );
                assertTrue( "Expected " + i + ", got " + r, i == r.intValue() || r.equals( Double.NaN ) );
                // System.err.print( "\t" + r );
            }
            // System.err.print( "\n" );
        }

    }

    private List<BioMaterial> getBioMaterials() {
        List<BioMaterial> bioMaterials = new ArrayList<BioMaterial>();
        for ( long i = 0; i < NUM_BIOMATERIALS; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance();
            bm.setName( RandomStringUtils.randomNumeric( 15 ) + "_testbiomaterial" );
            bm.setId( i );
            bioMaterials.add( bm );
        }
        return bioMaterials;
    }

    private List<CompositeSequence> getCompositeSequences( ArrayDesign ad ) {
        List<CompositeSequence> sequences = new ArrayList<CompositeSequence>();
        for ( long i = 0; i < NUM_CS; i++ ) {

            Reporter reporter = Reporter.Factory.newInstance();
            CompositeSequence compositeSequence = CompositeSequence.Factory.newInstance();
            reporter.setName( RandomStringUtils.randomNumeric( 15 ) + "_testreporter" );

            compositeSequence.setName( RandomStringUtils.randomNumeric( 15 ) + "_testcs" );
            compositeSequence.setId( i );
            compositeSequence.setArrayDesign( ad );
            sequences.add( compositeSequence );
        }
        return sequences;
    }
}
