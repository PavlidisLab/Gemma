package edu.columbia.gemma.loader.expression.arrayDesign;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.BeanFactory;

import edu.columbia.gemma.BaseServiceTestCase;
import edu.columbia.gemma.common.auditAndSecurity.Contact;
import edu.columbia.gemma.common.auditAndSecurity.ContactDao;
import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.common.description.LocalFileDao;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;
import edu.columbia.gemma.util.SpringContextUtil;

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

    private ArrayDesignLoaderImpl arrayDesignLoader = null;

    private Map map = null;

    private Map map2 = null;

    private Collection<ArrayDesign> col = null;

    /**
     * set up
     */
    protected void setUp() throws Exception {
        super.setUp();

        BeanFactory ctx = SpringContextUtil.getApplicationContext();

        arrayDesignParser = new ArrayDesignParserImpl();

        arrayDesignParser.setArrayDesignMappings( ( ArrayDesignMappings ) ctx.getBean( "arrayDesignMappings" ) );

        arrayDesignParser.setContactDao( ( ContactDao ) ctx.getBean( "contactDao" ) );

        arrayDesignParser.setLocalFileDao( ( LocalFileDao ) ctx.getBean( "localFileDao" ) );

        arrayDesignLoader = new ArrayDesignLoaderImpl();

        arrayDesignLoader.setArrayDesignDao( ( ArrayDesignDao ) ctx.getBean( "arrayDesignDao" ) );

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
//TODO implement this.    
//    public void testParseDelete() throws Exception {
//        
//    }
}
