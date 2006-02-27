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
package edu.columbia.gemma.loader.expression.mage;

import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MageLoadTest extends MageBaseTest {
    private static Log log = LogFactory.getLog( MageLoadTest.class.getName() );
    MageMLConverter mageMLConverter = null;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void onSetUpBeforeTransaction() throws Exception {
        this.setMageMLConverter( ( MageMLConverter ) getBean( "mageMLConverter" ) );
    }

    // /*
    // * Class under test for void create(Collection)
    // */
    // public void testCreateCollection() throws Exception {
    // log.info( "Parsing MAGE Jamboree example" );
    //
    // MageMLParser mlp = new MageMLParser();
    //
    // zipXslSetup( mlp, "/data/mage/mageml-example.zip" );
    //
    // ZipInputStream istMageExamples = new ZipInputStream( MageMLParserTest.class
    // .getResourceAsStream( "/data/mage/mageml-example.zip" ) );
    // istMageExamples.getNextEntry();
    // mlp.parse( istMageExamples );
    //
    // Collection<Object> parseResult = mlp.getResults();
    //
    // MageMLConverter mlc = new MageMLConverter( mlp.getSimplifiedXml() );
    //
    // Collection<Object> result = mlc.convert( parseResult );
    //
    // log.info( result.size() + " Objects parsed from the MAGE file." );
    // log.info( "Tally:\n" + mlp );
    // istMageExamples.close();
    // ml.persist( result );
    //
    // }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    public void testCreateCollectionRealA() throws Exception {
        log.info( "Parsing MAGE from ArrayExpress (AFMX)" );

        MageMLParser mlp = new MageMLParser();

        xslSetup( mlp, "/data/mage/E-AFMX-13/E-AFMX-13.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-AFMX-13/E-AFMX-13.xml" );
        mlp.parse( istMageExamples );
        Collection<Object> parseResult = mlp.getResults();
        getMageMLConverter().setSimplifiedXml( mlp.getSimplifiedXml() );
        Collection<Object> result = getMageMLConverter().convert( parseResult );

        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
        // if we don't do this, we get stale data errors.
        setFlushModeCommit();
        persisterHelper.persist( result );
    }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    public void testCreateCollectionRealB() throws Exception {
        log.info( "Parsing MAGE from ArrayExpress (WMIT)" );

        MageMLParser mlp = new MageMLParser();
        xslSetup( mlp, "/data/mage/E-WMIT-4.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( "/data/mage/E-WMIT-4.xml" );
        mlp.parse( istMageExamples );
        Collection<Object> parseResult = mlp.getResults();

        getMageMLConverter().setSimplifiedXml( mlp.getSimplifiedXml() );

        Collection<Object> result = getMageMLConverter().convert( parseResult );
        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();
        // if we don't do this, we get stale data errors.
        setFlushModeCommit();
        persisterHelper.persist( result );
    }

    /**
     * @return Returns the mageMLConverter.
     */
    public MageMLConverter getMageMLConverter() {
        return mageMLConverter;
    }

    /**
     * @param mageMLConverter The mageMLConverter to set.
     */
    public void setMageMLConverter( MageMLConverter mageMLConverter ) {
        this.mageMLConverter = mageMLConverter;
    }

}
