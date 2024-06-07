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

import org.junit.Test;
import org.junit.experimental.categories.Category;
import ubic.gemma.core.util.test.category.GeoTest;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;

import static org.junit.Assert.*;
import static ubic.gemma.core.util.test.Assumptions.assumeThatExceptionIsDueToNetworkIssue;
import static ubic.gemma.core.util.test.Assumptions.assumeThatResourceIsAvailable;

/**
 * @author pavlidis
 */
@Category(GeoTest.class)
public class ExpressionExperimentBibRefFinderTest {

    @Test
    public void testLocatePrimaryReference() {
        assumeThatResourceIsAvailable( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi" );
        ExpressionExperimentBibRefFinder finder = new ExpressionExperimentBibRefFinder();
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "GEO" );
        de.setAccession( "GSE3023" );
        de.setExternalDatabase( ed );
        ee.setAccession( de );
        BibliographicReference bibref = null;
        try {
            bibref = finder.locatePrimaryReference( ee );
        } catch ( IOException e ) {
            assumeThatExceptionIsDueToNetworkIssue( e );
        }
        assertNotNull( bibref );
        assertEquals( "Differential gene expression in anatomical compartments of the human eye.",
                bibref.getTitle() );
    }

    @Test
    public void testLocatePrimaryReferenceInvalidGSE() {
        assumeThatResourceIsAvailable( "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi" );
        ExpressionExperimentBibRefFinder finder = new ExpressionExperimentBibRefFinder();
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "GEO" );
        de.setAccession( "GSE30231111111111111" );
        de.setExternalDatabase( ed );
        ee.setAccession( de );
        BibliographicReference bibref = null;
        try {
            bibref = finder.locatePrimaryReference( ee );
        } catch ( IOException e ) {
            assumeThatExceptionIsDueToNetworkIssue( e );
        }
        assertNull( bibref );
    }
}
