/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.arrayDesign;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.loader.loaderutils.PersisterHelper;

/**
 * Loads the database with ArrayDesigns. This test is more representative of integration testing than unit testing as it
 * tests both parsing and loading.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignParserIntegrationTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( ArrayDesignParserIntegrationTest.class );

    private ArrayDesignParser arrayDesignParser = null;
    private ArrayDesignPersister arrayDesignLoader = null;
    private PersisterHelper persisterHelper;

    /**
     * set up
     */
    protected void setUp() throws Exception {
        super.setUp();
        persisterHelper = new PersisterHelper();
        arrayDesignParser = new ArrayDesignParser();
        arrayDesignLoader = new ArrayDesignPersister();
        arrayDesignLoader.setPersisterHelper( ( PersisterHelper ) ctx.getBean( "persisterHelper" ) );
    }

    /**
     * tear down
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since it's dependencies are
     * localized to the Gemma project it has been added to the test suite.
     * 
     * @throws Exception
     */
    public void testParseAndLoad() throws Exception {
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/arraydesign/array.txt" );
        arrayDesignParser.parse( is );
        persisterHelper.persist( arrayDesignParser.getResults() );
    }

}
