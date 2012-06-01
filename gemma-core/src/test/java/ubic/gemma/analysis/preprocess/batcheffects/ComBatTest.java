/*
 * The GemmaAnalysis project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess.batcheffects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.ObjectMatrix;
import ubic.basecode.dataStructure.matrix.StringMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.io.reader.StringMatrixReader;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * @author paul
 * @version $Id$
 */
public class ComBatTest {

    @SuppressWarnings("unused")
    private static Log log = LogFactory.getLog( ComBatTest.class );

    @Test
    public void test1() throws Exception {
        DoubleMatrixReader f = new DoubleMatrixReader();
        DoubleMatrix<String, String> testMatrix = f.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/example.madata.small.txt" ) );
        StringMatrixReader of = new StringMatrixReader();
        StringMatrix<String, String> sampleInfo = of.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/example.metadata.small.txt" ) );
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ComBat<String, String> comBat = new ComBat( testMatrix, sampleInfo );
        DoubleMatrix2D X = comBat.getDesignMatrix();
        assertEquals( 1, X.get( 0, 0 ), 0.001 );
        assertEquals( 0, X.get( 3, 0 ), 0.001 );
        assertEquals( 1, X.get( 4, 2 ), 0.001 );
        DoubleMatrix2D y = new DenseDoubleMatrix2D( testMatrix.asArray() );

        DoubleMatrix2D sdata = comBat.standardize( y, X );
        assertEquals( -0.25074, sdata.get( 17, 1 ), 0.0001 );
        assertEquals( 0.54122, sdata.get( 8, 2 ), 0.001 );
        assertEquals( 0.22358, sdata.get( 0, 8 ), 0.001 );
        assertEquals( 0.25211, sdata.get( 3, 7 ), 0.001 );

        DoubleMatrix2D finalResult = comBat.run();
        assertEquals( 10.67558, finalResult.get( 7, 0 ), 0.0001 );
        assertEquals( 11.68505, finalResult.get( 7, 7 ), 0.0001 );
        assertEquals( 6.769583, finalResult.get( 10, 7 ), 0.0001 );

        // comBat.plot("test");
        // log.info( finalResult );

    }

    @Test
    public void test2WithMissingValues() throws Exception {
        DoubleMatrixReader f = new DoubleMatrixReader();
        DoubleMatrix<String, String> testMatrix = f.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/example.madata.withmissing.small.txt" ) );
        StringMatrixReader of = new StringMatrixReader();
        StringMatrix<String, String> sampleInfo = of.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/example.metadata.small.txt" ) );
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ComBat<String, String> comBat = new ComBat( testMatrix, sampleInfo );
        DoubleMatrix2D X = comBat.getDesignMatrix();
        assertEquals( 1, X.get( 0, 0 ), 0.001 );
        assertEquals( 0, X.get( 3, 0 ), 0.001 );
        assertEquals( 1, X.get( 4, 2 ), 0.001 );
        DoubleMatrix2D y = new DenseDoubleMatrix2D( testMatrix.asArray() );
        DoubleMatrix2D sdata = comBat.standardize( y, X );

        assertEquals( -0.23640626, sdata.get( 17, 1 ), 0.0001 );
        assertEquals( 0.51027241, sdata.get( 8, 2 ), 0.001 );
        assertEquals( 0.2107944, sdata.get( 0, 8 ), 0.001 );
        assertEquals( 0.23769649, sdata.get( 3, 7 ), 0.001 );
        assertEquals( Double.NaN, sdata.get( 7, 6 ), 0.001 );

        DoubleMatrix2D finalResult = comBat.run();

        assertEquals( 10.660466, finalResult.get( 7, 0 ), 0.0001 );
        assertEquals( 11.733197, finalResult.get( 7, 7 ), 0.0001 );
        assertEquals( Double.NaN, finalResult.get( 7, 6 ), 0.0001 );
        assertEquals( 6.802441, finalResult.get( 10, 7 ), 0.0001 );

        // log.info( finalResult );
        // X08.1 X54.1 X36.1 X23.1 X17.1 X40.1 X45.1 X55.1 X11.1
        // 1553129_at 3.861661 3.656498 3.891722 3.969015 3.928164 3.859776 3.885422 3.831730 3.853814
        // 213447_at 6.233625 5.400615 5.583825 6.034642 6.457188 6.173610 5.322877 4.591996 6.655735
        // 242039_at 8.155451 8.487645 7.512280 7.043722 7.570154 7.928574 8.138381 8.538423 7.937447
        // 223394_at 7.794531 8.178473 8.285406 8.316963 7.845536 8.255656 8.604694 8.184320 7.311231
        // 227758_at 3.813320 3.474997 NA 3.663592 3.701014 NA 3.648964 3.618175 3.985569
        // 207696_at 3.576939 3.525421 3.561366 3.506479 3.516473 3.593750 3.628095 3.676431 3.599589
        // 241107_at 6.264194 5.926644 5.654168 5.730628 6.185137 5.587933 5.527347 5.895269 6.441413
        // 228980_at 10.660466 11.090106 10.769495 10.990729 10.616753 11.819747 NA 11.733197 10.516411
        // 204452_s_at 6.038281 5.403597 5.950596 6.443812 5.676120 5.238702 5.616082 5.290953 5.543041
        // 1562443_at 4.618687 3.961298 4.671874 4.512624 4.829666 4.138126 4.232039 4.048561 4.696936
        // 232018_at 6.221217 6.882512 6.093883 5.937127 5.987227 6.502522 6.940522 6.802441 5.673800
        // 1561877_at 3.793029 3.751057 3.719922 3.866485 4.070190 3.658865 3.465794 3.854070 3.878497
        // 221183_at 6.800233 5.559318 6.247321 6.566830 6.731457 5.701761 6.062595 5.097052 7.117171
        // 206162_x_at 5.273091 5.238142 5.023724 4.886765 5.162352 5.564269 5.573007 5.980072 5.558662
        // 214502_at 4.047844 NA 3.841319 4.006797 NA 4.504433 3.992359 4.192473 3.773261
        // 234099_at 7.628902 6.875036 7.101699 6.929775 7.202759 6.431563 6.622195 6.751740 7.740300
        // 237400_at 4.396190 NA 4.978136 4.775859 5.379108 5.809133 4.611809 4.853239 4.734252
        // 240254_at 4.062600 3.851718 4.274175 4.153745 4.030111 6.324506 4.089158 3.739869 4.426321
        // 209053_s_at 5.970077 6.378914 6.241240 6.450990 5.944027 6.702078 6.463590 6.372133 5.964286

    }

    /**
     * Based on GSE13712
     */
    @Test
    public final void test3() throws Exception {
        DoubleMatrixReader f = new DoubleMatrixReader();
        DoubleMatrix<String, String> testMatrix = f.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/comat.test.data.txt" ) );
        StringMatrixReader of = new StringMatrixReader();
        StringMatrix<String, String> sampleInfo = of.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/combat.test.design.txt" ) );
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ComBat<String, String> comBat = new ComBat( testMatrix, sampleInfo );
        DoubleMatrix2D result = comBat.run();
        assertNotNull( result );
    }

    /**
     * Case where we only have batch, no other covariates
     * 
     * @throws Exception
     */
    @Test
    public void test3NoCovariate() throws Exception {
        DoubleMatrixReader f = new DoubleMatrixReader();
        DoubleMatrix<String, String> testMatrix = f.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/example.madata.small.txt" ) );
        StringMatrixReader of = new StringMatrixReader();
        StringMatrix<String, String> sampleInfo = of.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/example.metadata.nocov.small.txt" ) );
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ComBat<String, String> comBat = new ComBat( testMatrix, sampleInfo );
        DoubleMatrix2D X = comBat.getDesignMatrix();
        assertEquals( 1, X.get( 0, 0 ), 0.001 );
        assertEquals( 0, X.get( 3, 0 ), 0.001 );
        DoubleMatrix2D y = new DenseDoubleMatrix2D( testMatrix.asArray() );
        DoubleMatrix2D sdata = comBat.standardize( y, X );

        assertEquals( -0.57397393, sdata.get( 17, 1 ), 0.0001 );
        assertEquals( 1.10827459, sdata.get( 8, 2 ), 0.001 );
        assertEquals( 0.45359207, sdata.get( 0, 8 ), 0.001 );
        assertEquals( 0.15470664, sdata.get( 3, 7 ), 0.001 );
        DoubleMatrix2D finalResult = comBat.run();

        assertEquals( 10.678412, finalResult.get( 7, 0 ), 0.0001 );
        assertEquals( 11.677158, finalResult.get( 7, 7 ), 0.0001 );
        assertEquals( 6.735682, finalResult.get( 10, 7 ), 0.0001 );

        // log.info( finalResult );
        // X08.1 X54.1 X36.1 X23.1 X17.1 X40.1 X45.1 X55.1 X11.1
        // 1553129_at 3.862883 3.666102 3.891911 3.968716 3.920277 3.854683 3.879281 3.837050 3.858994
        // 213447_at 6.233228 5.393208 5.601587 6.008113 6.456638 6.170673 5.312780 4.707229 6.568215
        // 242039_at 8.164096 8.502608 7.550607 7.146657 7.549429 7.914666 8.128463 8.437540 7.919430
        // 223394_at 7.785804 8.172181 8.258501 8.286628 7.847273 8.259993 8.611244 8.164748 7.386591
        // 227758_at 3.816278 3.475673 3.642967 3.670807 3.694009 3.641609 3.641609 3.633687 3.958491
        // 207696_at 3.580712 3.527869 3.565008 3.518815 3.510254 3.589519 3.624747 3.666631 3.601962
        // 241107_at 6.256698 5.922966 5.679003 5.749597 6.179683 5.589234 5.529333 5.900868 6.405107
        // 228980_at 10.678412 11.107177 10.773365 10.974208 10.592659 11.793201 10.935671 11.677158 10.572522
        // 204452_s_at 6.021874 5.366750 5.931940 6.337327 5.712018 5.260514 5.650047 5.355390 5.562588
        // 1562443_at 4.609301 3.964679 4.663442 4.516068 4.818594 4.140485 4.232574 4.084472 4.684495
        // 232018_at 6.226565 6.878310 6.097034 5.956156 5.989567 6.497420 6.929095 6.735682 5.721358
        // 1561877_at 3.789971 3.749341 3.728529 3.867675 4.058129 3.659954 3.473056 3.856079 3.879270
        // 221183_at 6.790431 5.543900 6.253127 6.541754 6.737387 5.703031 6.065498 5.210347 7.035212
        // 206162_x_at 5.294347 5.257304 5.090348 4.979353 5.118390 5.544392 5.553653 5.884388 5.542866
        // 214502_at 4.047479 3.865124 3.805045 3.963706 3.906569 4.478499 3.972879 4.154100 3.752158
        // 234099_at 7.596681 6.861572 7.109911 6.946028 7.208003 6.455995 6.641884 6.761660 7.703987
        // 237400_at 4.437312 5.820944 5.026344 4.825555 5.319276 5.731938 4.582957 4.979099 4.860988
        // 240254_at 4.106446 3.903017 4.238474 4.121097 4.000663 6.213980 4.057623 3.815918 4.484964
        // 209053_s_at 5.979671 6.378071 6.241459 6.440983 5.946471 6.685171 6.452771 6.374475 5.986512

    }

    @Test
    public void test4() throws Exception {
        DoubleMatrixReader f = new DoubleMatrixReader();
        DoubleMatrix<String, String> testMatrix = f.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/GSE492.test.dat.txt" ) );
        StringMatrixReader of = new StringMatrixReader();
        ObjectMatrix<String, String, String> sampleInfo = of.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/100_GSE492_expdesign.data.txt" ) );
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ComBat<String, String> comBat = new ComBat( testMatrix, sampleInfo );

        DoubleMatrix2D X = comBat.getDesignMatrix();

        // log.info( X );

        assertEquals( 1, X.get( 0, 0 ), 0.001 );
        assertEquals( 0, X.get( 1, 0 ), 0.001 );
        assertEquals( 0, X.get( 4, 3 ), 0.001 );
        DoubleMatrix2D y = new DenseDoubleMatrix2D( testMatrix.asArray() );

        DoubleMatrix2D sdata = comBat.standardize( y, X );
        // log.info( sdata.viewPart( 0, 0, 10, 12 ) );
        assertEquals( -1.85175902, sdata.get( 7, 1 ), 0.0001 );
        assertEquals( 0.2479669, sdata.get( 8, 2 ), 0.001 );
        assertEquals( -0.56259384, sdata.get( 0, 8 ), 0.001 );
        assertEquals( 1.07168246, sdata.get( 3, 11 ), 0.001 );

        DoubleMatrix2D finalResult = comBat.run();
        assertEquals( 12.026468, finalResult.get( 7, 0 ), 0.0001 );
        assertEquals( 11.640057, finalResult.get( 7, 7 ), 0.0001 );
        assertEquals( 12.932352, finalResult.get( 9, 7 ), 0.0001 );
    }

    @Test
    public void test5NonParametric() throws Exception {
        DoubleMatrixReader f = new DoubleMatrixReader();
        DoubleMatrix<String, String> testMatrix = f.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/GSE492.test.dat.txt" ) );
        StringMatrixReader of = new StringMatrixReader();
        StringMatrix<String, String> sampleInfo = of.read( this.getClass().getResourceAsStream(
                "/data/analysis/preprocess/batcheffects/100_GSE492_expdesign.data.txt" ) );
        @SuppressWarnings({ "unchecked", "rawtypes" })
        ComBat<String, String> comBat = new ComBat( testMatrix, sampleInfo );
        DoubleMatrix2D X = comBat.getDesignMatrix();

        // log.info( X );

        assertEquals( 1, X.get( 0, 0 ), 0.001 );
        assertEquals( 0, X.get( 1, 0 ), 0.001 );
        assertEquals( 0, X.get( 4, 3 ), 0.001 );
        DoubleMatrix2D y = new DenseDoubleMatrix2D( testMatrix.asArray() );

        DoubleMatrix2D sdata = comBat.standardize( y, X );
        // log.info( sdata.viewPart( 0, 0, 10, 12 ) );
        assertEquals( -1.85175902, sdata.get( 7, 1 ), 0.0001 );
        assertEquals( 0.2479669, sdata.get( 8, 2 ), 0.001 );
        assertEquals( -0.56259384, sdata.get( 0, 8 ), 0.001 );
        assertEquals( 1.07168246, sdata.get( 3, 11 ), 0.001 );

        DoubleMatrix2D finalResult = comBat.run( false );
        assertEquals( 12.026930, finalResult.get( 7, 0 ), 0.001 );
        assertEquals( 11.635157, finalResult.get( 7, 7 ), 0.01 );
        assertEquals( 12.930425, finalResult.get( 9, 7 ), 0.01 );
    }
}
