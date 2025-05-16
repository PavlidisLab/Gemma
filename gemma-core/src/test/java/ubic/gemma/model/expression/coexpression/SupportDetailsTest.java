/*
 * The gemma project
 *
 * Copyright (c) 2014 University of British Columbia
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

package ubic.gemma.model.expression.coexpression;

import com.googlecode.javaewah.EWAHCompressedBitmap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import ubic.basecode.util.RegressionTesting;
import ubic.gemma.model.analysis.expression.coexpression.MouseCoexpressionSupportDetails;
import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.genome.Gene;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Paul
 */
public class SupportDetailsTest {

    /*
     * We store the bitmaps as blobs in the database. This just tests the correctness of these.
     *
     */
    @Test
    public void testSerializeEWAHCompressedBitmap() throws Exception {
        // select HEX(BYTES) from OTHER_LINK_SUPPORT_DETAILS WHERE ID=1
        // 000006AA00000001000000000000003600000000 = empty after removal.
        Byte[] or = new Byte[] { 0, 0, 6, ( byte ) 0xAA, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 36, 0, 0, 0, 0 };
        System.err.println( StringUtils.join( or, "," ) );
        DataInput c = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) ) );
        EWAHCompressedBitmap ff = new EWAHCompressedBitmap();
        ff.deserialize( c );
        assertTrue( ff.getPositions().isEmpty() );

        // 0000000200000001000000000000000200000000 = also empty after removal.
        or = new Byte[] { 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0 };
        System.err.println( StringUtils.join( or, "," ) );
        c = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) ) );
        ff = new EWAHCompressedBitmap();
        ff.deserialize( c );
        assertTrue( ff.getPositions().isEmpty() );

        Byte[] or2 = new Byte[] { 0, 0, 4, 22, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 32, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0,
                0 };
        DataInput di = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or2 ) ) );
        EWAHCompressedBitmap f = new EWAHCompressedBitmap();
        f.deserialize( di );
        RegressionTesting.closeEnough( new Integer[] { 1045 }, f.getPositions().toArray( new Integer[] {} ) );

        Byte[] or3 = new Byte[] { 0x00, 0x00, 0x19, 0x78, 0x00, 0x00, 0x00, 0x04, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00,
                0x00, 0x3E, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, ( byte ) 0xC0, 0x00, 0x00, 0x00, 0x00, 0x02, 0x00, 0x00,
                0x00, ( byte ) 0x8A, 0x00, ( byte ) 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x02 };
        di = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or3 ) ) );
        f = new EWAHCompressedBitmap();
        f.deserialize( di );
        // expect : 1998,1999,6519
        RegressionTesting
                .closeEnough( new Integer[] { 1998, 1999, 6519 }, f.getPositions().toArray( new Integer[] {} ) );

        // empty after initialization
        Byte[] or4 = new Byte[] { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        di = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or4 ) ) );
        f = new EWAHCompressedBitmap();
        f.deserialize( di );
        assertTrue( f.getPositions().isEmpty() );

        // 00000002000000020000000200000000000000000000000200000000: 1
        // or = new Byte[] { 00, 00, 00, 02, 00, 00, 00, 02, 00, 00, 00, 02, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00, 00,
        // 02, 00, 00, 00, 00 };
        // System.err.println( StringUtils.join( or, "," ) );
        // c = new DataInputStream(new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) );
        // ff = new EWAHCompressedBitmap();
        // ff.deserialize( c );
        // assertTrue( "" + ff.getPositions(), ff.getPositions().isEmpty() );

        // 00000003000000020000000200000000000000000000000600000000: 1,2
        or = new Byte[] { 0, 0, 0, 3, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0 };
        System.err.println( StringUtils.join( or, "," ) );
        c = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) ) );
        ff = new EWAHCompressedBitmap();
        ff.deserialize( c );
        // assertTrue( "" + ff.getPositions(), ff.getPositions().isEmpty() );

        // 00000003000000020000000200000000000000000000000400000000: 2
        or = new Byte[] { 0, 0, 0, 3, 0, 0, 0, 2, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 0, 0, 0 };
        System.err.println( StringUtils.join( or, "," ) );
        c = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) ) );
        ff = new EWAHCompressedBitmap();
        ff.deserialize( c );
        // assertTrue( "" + ff.getPositions(), ff.getPositions().isEmpty() );

        // 0000003E00000001000000000000000200000000
        or = new Byte[] { 0, 0, 0, ( byte ) 0x3E, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0 };
        System.err.println( StringUtils.join( or, "," ) );
        c = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) ) );
        ff = new EWAHCompressedBitmap();
        ff.deserialize( c );
        assertTrue( ff.getPositions().isEmpty() );

        // 0000003F00000001000000000000000200000000
        or = new Byte[] { 0, 0, 0, ( byte ) 0x3F, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0 };
        System.err.println( StringUtils.join( or, "," ) );
        c = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) ) );
        ff = new EWAHCompressedBitmap();
        ff.deserialize( c );
        assertTrue( ff.getPositions().isEmpty() );

        // 0000000500000001000000000000000200000000
        or = new Byte[] { 0, 0, 0, 5, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0 };
        System.err.println( StringUtils.join( or, "," ) );
        c = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) ) );
        ff = new EWAHCompressedBitmap();
        ff.deserialize( c );
        assertTrue( ff.getPositions().isEmpty() );

        // 0000015300000001000000000000000C00000000
        or = new Byte[] { 0, 0, 1, 53, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, ( byte ) 0x0C, 0, 0, 0, 0 };
        System.err.println( StringUtils.join( or, "," ) );
        c = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) ) );
        ff = new EWAHCompressedBitmap();
        ff.deserialize( c );
        assertTrue( ff.getPositions().isEmpty() );

        // 000002BB00000001000000000000001600000000
        or = new Byte[] { 0, 0, 2, ( byte ) 0xBB, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0 };
        System.err.println( StringUtils.join( or, "," ) );
        c = new DataInputStream( new ByteArrayInputStream( ArrayUtils.toPrimitive( or ) ) );
        ff = new EWAHCompressedBitmap();
        ff.deserialize( c );
        assertTrue( ff.getPositions().isEmpty() );
    }

    @Test
    public void test() {
        Gene g1 = Gene.Factory.newInstance();
        g1.setId( 3L );

        Gene g2 = Gene.Factory.newInstance();
        g2.setId( 4L );

        SupportDetails sd1 = new MouseCoexpressionSupportDetails( g1, g2, true );
        SupportDetails sd2 = new MouseCoexpressionSupportDetails( g1, g2, true );

        sd1.addEntity( 1L );
        sd2.addEntity( 2L );

        sd2.removeEntity( 2L );
        sd2.addEntity( 1L );

        sd1.addEntity( 10L );
        sd2.addEntity( 10L );

        assertEquals( 2, sd2.getNumIds() );

    }

}
