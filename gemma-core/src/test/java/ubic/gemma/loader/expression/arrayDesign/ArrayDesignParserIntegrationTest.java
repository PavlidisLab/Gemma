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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Loads the database with ArrayDesigns. This test is more representative of integration testing than unit testing as it
 * tests both parsing and loading.
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignParserIntegrationTest extends BaseSpringContextTest {
    protected static final Log log = LogFactory.getLog( ArrayDesignParserIntegrationTest.class );

    private ArrayDesignParser arrayDesignParser = null;

    private Collection<ArrayDesign> result;

    /**
     * set up
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        arrayDesignParser = new ArrayDesignParser();
    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        // for ( ArrayDesign ad : result ) {
        // ( ( ArrayDesignService ) this.getBean( "arrayDesignService" ) ).remove( ad );
        // }
    }

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since it's dependencies are
     * localized to the Gemma project it has been added to the test suite.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void testParseAndLoad() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/array.txt" );

        assert is != null : "Resource /data/loader/expression/arrayDesign/array.txt not available";

        arrayDesignParser.parse( is );
        assertTrue( "No results", arrayDesignParser.getResults().size() > 0 );

        result = ( Collection<ArrayDesign> ) persisterHelper.persist( arrayDesignParser.getResults() );
        assertTrue( result.size() > 0 );
        for ( Object object : result ) {
            assertTrue( object instanceof ArrayDesign );
            assertTrue( object + " did not have an id", ( ( ArrayDesign ) object ).getId() != null );
            log.debug( object );
        }
    }

}
