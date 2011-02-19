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
package ubic.gemma.loader.entrez.pubmed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentBibRefFinderTest {

    private static Log log = LogFactory.getLog( ExpressionExperimentBibRefFinderTest.class.getName() );

    @SuppressWarnings("null")
    @Test
    public void testLocatePrimaryReference() throws Exception {
        ExpressionExperimentBibRefFinder finder = new ExpressionExperimentBibRefFinder();
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "GEO" );
        de.setAccession( "GSE3023" );
        de.setExternalDatabase( ed );
        ee.setAccession( de );
        try {
            BibliographicReference bibref = null;
            for ( int i = 0; i < 3; i++ ) {
                bibref = finder.locatePrimaryReference( ee );
                if ( bibref != null ) break;
                Thread.sleep( 1000 );
            }
            assertNotNull( bibref );
            assertEquals( "Differential gene expression in anatomical compartments of the human eye.", bibref
                    .getTitle() );
        } catch ( Exception e ) {
            checkCause( e );
            return;
        }

    }

    @Test
    public void testLocatePrimaryReferenceInvalidGSE() throws Exception {
        ExpressionExperimentBibRefFinder finder = new ExpressionExperimentBibRefFinder();
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "GEO" );
        de.setAccession( "GSE30231111111111111" );
        de.setExternalDatabase( ed );
        ee.setAccession( de );
        try {
            BibliographicReference bibref = finder.locatePrimaryReference( ee );
            assert ( bibref == null );
        } catch ( Exception e ) {
            checkCause( e );
            return;
        }
    }

    /**
     * @param e
     */
    private void checkCause( Exception e ) throws Exception {

        Throwable k = null;
        if ( e instanceof IOException ) {
            k = e;
        } else if ( e.getCause() instanceof IOException ) {
            k = e.getCause();
        } else {
            throw e;
        }

        if ( k instanceof IOException && k.getMessage().contains( "503" ) ) {
            log.warn( "Test skipped due to a 503 error from NCBI" );
            return;
        }
        if ( k instanceof IOException && k.getMessage().contains( "502" ) ) {
            log.warn( "Test skipped due to a 502 error from NCBI" );
            return;
        }
        if ( k instanceof IOException && k.getMessage().contains( "500" ) ) {
            log.warn( "Test skipped due to a 500 error from NCBI" );
            return;
        }
        throw e;

    }

}
