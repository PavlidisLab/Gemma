/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MageMLConverterTest extends AbstractMageTest {

    private MageMLParser mageMLParser = null;
    private MageMLConverter mageMLConverter = null;

    /**
     * @param mageMLConverter The mageMLConverter to set.
     */
    public void setMageMLConverter( MageMLConverter mageMLConverter ) {
        this.mageMLConverter = mageMLConverter;
    }

    /**
     * @param mageMLParser The mageMLParser to set.
     */
    public void setMageMLParser( MageMLParser mageMLParser ) {
        this.mageMLParser = mageMLParser;
    }

    @SuppressWarnings("null")
    public final void testWithDerivedBioAssays() throws Exception {
        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "jml_projID_4_E-HGMP-2.part.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        /* CONVERTING */
        log.info( "***** CONVERTING ***** " );

        ExpressionExperiment expressionExperiment = null;
        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assertNotNull( expressionExperiment );
        assertEquals( 1, numExpExp );
        assertEquals( 32, expressionExperiment.getBioAssays().size() );
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            assertTrue( ba.getName().contains( "DBA" ) );
        }

    }

    public void testConvertCollection() throws Exception {

        /* PARSING */
        log.info( "***** PARSING *****  " );

        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-MEXP-955.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        /* CONVERTING */
        log.info( "***** CONVERTING ***** " );

        mageMLConverter.addLocalExternalDataPath( ConfigUtils.getString( "gemma.home" ) + File.separatorChar
                + "gemma-core/src/test/resources" + MAGE_DATA_RESOURCE_PATH + "E-MEXP-955" );

        ExpressionExperiment expressionExperiment = null;
        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assert expressionExperiment != null && numExpExp == 1;
    }
}
