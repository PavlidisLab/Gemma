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
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.auditAndSecurity.ContactDao;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.LocalFileDao;
import edu.columbia.gemma.loader.expression.PersisterHelper;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;

/**
 * Loads the database with ArrayDesigns.
 * This test is more representative of integration testing than unit testing as it tests both parsing and
 * loading.  
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignParserIntegrationTest extends BaseServiceTestCase {
    //TODO - refactor methods to separate junit tests from the integration test.  This is currently an integration test.
    protected static final Log log = LogFactory.getLog( ArrayDesignParserIntegrationTest.class );

    private ArrayDesignParserImpl arrayDesignParser = null;

    private ArrayDesignPersister arrayDesignLoader = null;

    private Map map = null;

    private Collection<Object> col = null;

    /**
     * set up
     */
    protected void setUp() throws Exception {
        super.setUp();

        arrayDesignParser = new ArrayDesignParserImpl();

        arrayDesignParser.setArrayDesignMappings( ( ArrayDesignMappings ) ctx.getBean( "arrayDesignMappings" ) );

        arrayDesignParser.setContactDao( ( ContactDao ) ctx.getBean( "contactDao" ) );

        arrayDesignParser.setLocalFileDao( ( LocalFileDao ) ctx.getBean( "localFileDao" ) );

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

        Method m = ParserAndLoaderTools.findParseLineMethod( arrayDesignParser.getArrayDesignMappings(), "array" );
        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/arraydesign/array.txt" );
        arrayDesignParser.parse( is, m );

        Method m2 = ParserAndLoaderTools.findParseLineMethod( arrayDesignParser.getArrayDesignMappings(), "mgu74a" );
        InputStream is2 = this.getClass().getResourceAsStream( "/data/loader/expression/arraydesign/MG-U74A.txt" );
        map = arrayDesignParser.parse( is2, m2 );

        Object[] dependencies = new Object[2];

        Contact contact = Contact.Factory.newInstance();
        contact.setName( "Affymetrix" );
        contact.setPhone( "888-362-2447" );
        contact.setURI( "http://www.affymetrix.com/index.affx" );

        LocalFile lf = LocalFile.Factory.newInstance();
        lf.setLocalURI( "/data/loader/expression/arraydesign/array.txt" );
        lf.setSize( 12177 );

        dependencies[0] = contact;
        dependencies[1] = lf;

        col = arrayDesignParser.createOrGetDependencies( dependencies, map );

        ParserAndLoaderTools.loadDatabase( arrayDesignLoader, col );

    }
    // TODO implement this.
    // public void testParseDelete() throws Exception {
    //        
    // }
}
