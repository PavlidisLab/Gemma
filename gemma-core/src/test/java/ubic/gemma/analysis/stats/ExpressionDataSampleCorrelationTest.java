/*
 * The Gemma project
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
package ubic.gemma.analysis.stats;

import junit.framework.TestCase;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public class ExpressionDataSampleCorrelationTest extends TestCase {

    public void testFileName() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( "WNT/FOO" );
        String actual = ExpressionDataSampleCorrelation.getMatrixFileBaseName( ee );
        assertTrue( " got " + actual, !actual.contains( "/" ) );
        assertEquals( "WNT.FOO_corrmat", actual );

        ee.setShortName( "WNT/FOO[]()*" );
        actual = ExpressionDataSampleCorrelation.getMatrixFileBaseName( ee );
        assertEquals( "WNT.FOO_corrmat", actual );

        ee.setShortName( "^%%WNT/FOO[]()*" );
        actual = ExpressionDataSampleCorrelation.getMatrixFileBaseName( ee );
        assertEquals( "WNT.FOO_corrmat", actual );

        ee.setShortName( "^9999%%WNT/FOO[]()*" );
        actual = ExpressionDataSampleCorrelation.getMatrixFileBaseName( ee );
        assertEquals( "9999.WNT.FOO_corrmat", actual );

        ee.setShortName( "  ^%%WNT/   F OO[ ]()*" );
        actual = ExpressionDataSampleCorrelation.getMatrixFileBaseName( ee );
        assertEquals( "WNT.F.OO_corrmat", actual );
    }

}
