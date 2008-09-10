/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.analysis.preprocess;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.util.RegressionTesting;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.datastructure.matrix.TestExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.designElement.DesignElement;
import junit.framework.TestCase;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionDataSVDTest extends TestCase {

    ExpressionDataDoubleMatrix testData = null;
    ExpressionDataSVD svd = null;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();

        testData = new TestExpressionDataDoubleMatrix();
        svd = new ExpressionDataSVD( testData, false );
    }

    /**
     * Test method for {@link ubic.gemma.analysis.preprocess.ExpressionDataSVD#getS()}.
     */
    public void testGetS() {
        DoubleMatrix<Integer, Integer> s = svd.getS();
        assertNotNull( s );
    }

    public void testUMatrixAsExpressionDataUnnormalized() throws Exception {
        try {
            svd.uMatrixAsExpressionData();
            fail( "Should have gotten an exception" );
        } catch ( IllegalStateException e ) {
            //
        }
    }

    public void testUMatrixAsExpressionData() throws Exception {
        svd = new ExpressionDataSVD( testData, true );
        ExpressionDataDoubleMatrix matrixAsExpressionData = svd.uMatrixAsExpressionData();
        assertNotNull( matrixAsExpressionData );
    }

    public void testWinnow() throws Exception {
        ExpressionDataDoubleMatrix winnow = svd.winnow( 0.5 );
        assertEquals( 100, winnow.rows() );
    }

    /**
     * Test method for {@link ubic.gemma.analysis.preprocess.ExpressionDataSVD#getU()}.
     */
    public void testGetU() {
        DoubleMatrix<DesignElement, Integer> u = svd.getU();
        assertNotNull( u );
    }

    /**
     * Test method for {@link ubic.gemma.analysis.preprocess.ExpressionDataSVD#svdNormalize()}.
     */
    public void testMatrixReconstruct() {
        ExpressionDataDoubleMatrix svdNormalize = svd.removeHighestComponents( 0 );
        assertNotNull( svdNormalize );
        RegressionTesting.closeEnough( testData.getMatrix(), svdNormalize.getMatrix(), 0.001 );
    }

}
