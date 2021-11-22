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
package ubic.gemma.core.loader.entrez.pubmed;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author pavlidis
 */
public class ExpressionExperimentBibRefFinderTest {

    private static final Log log = LogFactory.getLog( ExpressionExperimentBibRefFinderTest.class.getName() );

    @Test
    @Category(SlowTest.class)
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
                if ( bibref != null )
                    break;
                Thread.sleep( 1000 );
            }
            assertNotNull( bibref );
            assertEquals( "Differential gene expression in anatomical compartments of the human eye.",
                    bibref.getTitle() );
        } catch ( Exception e ) {
            checkCause( e );
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
        }
    }

    private void checkCause( Exception e ) throws Exception {

        Throwable k;
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
