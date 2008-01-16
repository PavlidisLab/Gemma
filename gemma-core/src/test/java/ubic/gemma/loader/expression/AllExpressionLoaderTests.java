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
package ubic.gemma.loader.expression;

import ubic.gemma.loader.expression.arrayDesign.AllArrayDesignTests;
import ubic.gemma.loader.expression.arrayExpress.ArrayDesignFetcherIntegrationTest;
import ubic.gemma.loader.expression.arrayExpress.ArrayExpressLoadServiceIntegrationTest;
import ubic.gemma.loader.expression.arrayExpress.DataFileFetcherIntegrationTest;
import ubic.gemma.loader.expression.geo.AllGeoTests;
import ubic.gemma.loader.expression.mage.AllMageTests;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderServiceTest;
import ubic.gemma.loader.expression.smd.AllSmdTests;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author paul
 * @version $Id$
 */
public class AllExpressionLoaderTests {
    public static Test suite() {
        TestSuite suite = new TestSuite( "Tests for gemma-core expression loading" );
        suite.addTest( AllArrayDesignTests.suite() );
        suite.addTestSuite( ArrayDesignFetcherIntegrationTest.class );
        suite.addTestSuite( ArrayExpressLoadServiceIntegrationTest.class );
        suite.addTestSuite( DataFileFetcherIntegrationTest.class );
        suite.addTest( AllGeoTests.suite() );
        suite.addTest( AllMageTests.suite() );
        suite.addTest( AllSmdTests.suite() );
        suite.addTestSuite( SimpleExpressionDataLoaderServiceTest.class );
        return suite;
    }
}
