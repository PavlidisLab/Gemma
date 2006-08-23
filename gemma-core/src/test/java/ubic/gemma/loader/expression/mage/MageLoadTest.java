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
package ubic.gemma.loader.expression.mage;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MageLoadTest extends AbstractMageTest {

    private static Log log = LogFactory.getLog( MageLoadTest.class.getName() );
    MageMLConverter mageMLConverter = null;
    PersisterHelper persisterHelper;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        persisterHelper = ( PersisterHelper ) this.getBean( "persisterHelper" );
        this.setMageMLConverter( ( MageMLConverter ) getBean( "mageMLConverter" ) );
    }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    public void testCreateCollectionRealA() throws Exception {
        log.info( "Parsing MAGE from ArrayExpress (AFMX)" );

        MageMLParser mlp = new MageMLParser();

        xslSetup( mlp, MAGE_DATA_RESOURCE_PATH + "E-AFMX-13/E-AFMX-13.xml" );

        mageMLConverter.addLocalExternalDataPath( ConfigUtils.getString( "gemma.home" ) + File.separatorChar
                + "gemma-core/src/test/resources" + MAGE_DATA_RESOURCE_PATH + "E-AFMX-13" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-AFMX-13/E-AFMX-13.xml" );
        mlp.parse( istMageExamples );
        Collection<Object> parseResult = mlp.getResults();
        getMageMLConverter().setSimplifiedXml( mlp.getSimplifiedXml() );
        Collection<Object> result = getMageMLConverter().convert( parseResult );

        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();

        for ( Object object : result ) {
            if ( object instanceof ExpressionExperiment ) {
                ExpressionExperiment ee = ( ExpressionExperiment ) persisterHelper.persist( object );
                assertNotNull( ee.getId() );
                assertEquals( 12, ee.getBioAssays().size() );
            }
        }
    }

    /**
     * A real example of an experimental package.
     * 
     * @throws Exception
     */
    public void testCreateCollectionRealB() throws Exception {
        log.info( "Parsing MAGE from ArrayExpress (WMIT)" );

        MageMLParser mlp = new MageMLParser();
        xslSetup( mlp, MAGE_DATA_RESOURCE_PATH + "E-WMIT-4.xml" );

        InputStream istMageExamples = MageMLParserTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-WMIT-4.xml" );

        mageMLConverter.addLocalExternalDataPath( ConfigUtils.getString( "gemma.home" ) + File.separatorChar
                + "gemma-core/src/test/resources" + MAGE_DATA_RESOURCE_PATH + "E-WMIT-4" );

        mlp.parse( istMageExamples );
        Collection<Object> parseResult = mlp.getResults();

        getMageMLConverter().setSimplifiedXml( mlp.getSimplifiedXml() );

        Collection<Object> result = getMageMLConverter().convert( parseResult );
        log.info( result.size() + " Objects parsed from the MAGE file." );
        log.info( "Tally:\n" + mlp );
        istMageExamples.close();

        for ( Object object : result ) {
            if ( object instanceof ExpressionExperiment ) {
                ExpressionExperiment ee = ( ExpressionExperiment ) persisterHelper.persist( object );
                assertNotNull( ee.getId() );
                assertEquals( 12, ee.getBioAssays().size() );
            }
        }

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
