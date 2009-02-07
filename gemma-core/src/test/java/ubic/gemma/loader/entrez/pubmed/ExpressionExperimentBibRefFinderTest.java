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

import java.io.IOException;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ExpressionExperimentBibRefFinderTest extends TestCase {

    private static Log log = LogFactory.getLog( ExpressionExperimentBibRefFinderTest.class.getName() );

    public void testLocatePrimaryReference() {
        ExpressionExperimentBibRefFinder finder = new ExpressionExperimentBibRefFinder();
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "GEO" );
        de.setAccession( "GSE3023" );
        de.setExternalDatabase( ed );
        ee.setAccession( de );
        try {
            BibliographicReference bibref = finder.locatePrimaryReference( ee );
            assertNotNull( bibref );
            assertEquals( "Differential gene expression in anatomical compartments of the human eye.", bibref
                    .getTitle() );
        } catch ( RuntimeException e ) {
            checkCause( e );
            return;
        }

    }

    public void testLocatePrimaryReferenceInvalidGSE() {
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
        } catch ( RuntimeException e ) {
            checkCause( e );
            return;
        }
    }

    /**
     * @param e
     */
    private void checkCause( RuntimeException e ) {
        if ( e.getCause() instanceof IOException && e.getMessage().contains( "503" ) ) {
            log.warn( "Test skipped due to a 503 error from NCBI" );
            return;
        }
        if ( e.getCause() instanceof IOException && e.getMessage().contains( "502" ) ) {
            log.warn( "Test skipped due to a 502 error from NCBI" );
            return;
        }
        throw e;

    }

}
