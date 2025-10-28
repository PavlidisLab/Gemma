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
package ubic.gemma.core.datastructure.matrix;

import junit.framework.TestCase;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.core.loader.expression.arrayDesign.Reporter;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;

import java.util.*;

/**
 * @author pavlidis
 */
public class MatrixConversionTest extends TestCase {

    private static final int NUM_BIOMATERIALS = 40;
    private static final int NUM_CS = 200;
    private static final Log log = LogFactory.getLog( MatrixConversionTest.class.getName() );

    public final void testColumnMapping() {
        Collection<QuantitationType> quantTypes = new HashSet<>();

        QuantitationType quantType = new PersistentDummyObjectHelper().getTestNonPersistentQuantitationType();
        quantType.setId( 0L );
        quantTypes.add( quantType );

        Collection<RawExpressionDataVector> vectors = this.getDesignElementDataVectors( quantTypes );
        ExpressionDataDoubleMatrix mat = new ExpressionDataDoubleMatrix( vectors );
        MatrixConversionTest.log.debug( vectors.size() + " vectors" );

        TestCase.assertEquals( MatrixConversionTest.NUM_CS, mat.rows() );
        TestCase.assertEquals( MatrixConversionTest.NUM_BIOMATERIALS, mat.columns() );

        for ( int j = 0; j < mat.rows(); j++ ) {
            // System.err.print( mat.getRowElement( j ) );
            for ( int i = 0; i < mat.columns(); i++ ) {
                Double r = mat.get( j, i );
                TestCase.assertNotNull( "No value for at index " + i, r );
                TestCase.assertTrue( "Expected " + i + ", got " + r, i == r.intValue() || r.equals( Double.NaN ) );
            }
        }

    }

    /**
     * Creates an ugly (but not unusual) situation where there are two bioassay dimensions with different sizes,
     * referring to the same set of biomaterials.
     *
     * @return design element data vectors
     */
    private Collection<RawExpressionDataVector> getDesignElementDataVectors( Collection<QuantitationType> quantTypes ) {
        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( "junk" );
        List<CompositeSequence> sequences = this.getCompositeSequences( ad );

        ArrayDesign adb = ArrayDesign.Factory.newInstance();
        adb.setName( "bjunk" );
        List<CompositeSequence> sequencesb = this.getCompositeSequences( ad );

        List<BioMaterial> bioMaterials = this.getBioMaterials(); // resused

        for ( QuantitationType quantType : quantTypes ) {

            /*
             * Create two bioassay dimension which overlap; "A" does not use all the biomaterials.
             */
            BioAssayDimension baDimA = BioAssayDimension.Factory.newInstance();
            Iterator<BioMaterial> bmita = bioMaterials.iterator();
            for ( long i = 0; i < MatrixConversionTest.NUM_BIOMATERIALS - 20; i++ ) {
                BioAssay ba = ubic.gemma.model.expression.bioAssay.BioAssay.Factory.newInstance();
                ba.setName( RandomStringUtils.insecure().nextNumeric( 5 ) + "_testbioassay" );
                ba.setSampleUsed( bmita.next() );
                ba.setArrayDesignUsed( ad );
                ba.setId( i );
                baDimA.getBioAssays().add( ba );
            }

            BioAssayDimension baDimB = BioAssayDimension.Factory.newInstance();
            Iterator<BioMaterial> bmitb = bioMaterials.iterator();
            for ( long i = 0; i < MatrixConversionTest.NUM_BIOMATERIALS; i++ ) {
                BioAssay ba = ubic.gemma.model.expression.bioAssay.BioAssay.Factory.newInstance();
                ba.setName( RandomStringUtils.insecure().nextNumeric( 15 ) + "_testbioassay" );
                ba.setSampleUsed( bmitb.next() );
                ba.setArrayDesignUsed( adb );
                ba.setId( i + 20 );
                baDimB.getBioAssays().add( ba );
            }

            // bio.a gets cs 0-99, bio.b gets 100-199.
            long j = 0;
            j = this.loopVectors( vectors, sequencesb, quantType, baDimA, j, MatrixConversionTest.NUM_CS - 100 );
            //noinspection UnusedAssignment // Better readability
            j = this.loopVectors( vectors, sequences, quantType, baDimB, j, MatrixConversionTest.NUM_CS );
        }
        return vectors;
    }

    private long loopVectors( Collection<RawExpressionDataVector> vectors, List<CompositeSequence> sequencesb,
            QuantitationType quantType, BioAssayDimension baDimA, long j, int i2 ) {
        for ( ; j < i2; j++ ) {
            RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
            CompositeSequence cs = sequencesb.get( ( int ) j );
            vector.setDesignElement( cs );
            vector.setQuantitationType( quantType );
            vector.setBioAssayDimension( baDimA );
            double[] data = new double[baDimA.getBioAssays().size()];
            for ( int k = 0; k < data.length; k++ ) {
                data[k] = k;
            }
            vector.setDataAsDoubles( data );
            vectors.add( vector );
        }
        return j;
    }

    private List<BioMaterial> getBioMaterials() {
        List<BioMaterial> bioMaterials = new ArrayList<>();
        for ( long i = 0; i < MatrixConversionTest.NUM_BIOMATERIALS; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance();
            bm.setName( RandomStringUtils.insecure().nextNumeric( 15 ) + "_testbiomaterial" );
            bm.setId( i );
            bioMaterials.add( bm );
        }
        return bioMaterials;
    }

    private List<CompositeSequence> getCompositeSequences( ArrayDesign ad ) {
        List<CompositeSequence> sequences = new ArrayList<>();
        for ( long i = 0; i < MatrixConversionTest.NUM_CS; i++ ) {

            Reporter reporter = Reporter.Factory.newInstance();
            CompositeSequence compositeSequence = CompositeSequence.Factory.newInstance();
            reporter.setName( RandomStringUtils.insecure().nextNumeric( 15 ) + "_testreporter" );

            compositeSequence.setName( RandomStringUtils.insecure().nextNumeric( 15 ) + "_testcs" );
            compositeSequence.setId( i );
            compositeSequence.setArrayDesign( ad );
            sequences.add( compositeSequence );
        }
        return sequences;
    }
}
