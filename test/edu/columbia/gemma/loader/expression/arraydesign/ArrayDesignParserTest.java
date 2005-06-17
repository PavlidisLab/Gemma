package edu.columbia.gemma.loader.expression.arraydesign;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.loader.loaderutils.LoaderTools;

/**
 * This test is more representative of integration testing than unit testing as it tests multiple both parsing and
 * loading.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class ArrayDesignParserTest extends BaseServiceTestCase {
    protected static final Log log = LogFactory.getLog( ArrayDesignParserTest.class );

    private ArrayDesignParserImpl arrayDesignParser = null;

    // private ArrayDesignLoaderImpl arrayDesignLoader = null;

    private Map map = null;

    /**
     * Tests both the parser and the loader. This is more of an integration test, but since it's dependencies are
     * localized to the Gemma project it has been added to the test suite.
     * 
     * @throws Exception
     */
    public void testParseAndLoad() throws Exception {

        // TODO create dependencies

        Method m = LoaderTools.findParseLineMethod( arrayDesignParser.getArrayDesignMappings(), "put the file suffix" );

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/expression/arraydesign/HC-G110.txt" );

        map = arrayDesignParser.parse( is, m );

        // LoaderTools.loadDatabase( arrayDesignLoader, map.values() );

    }

    /**
     * set up
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * tear down
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

}
