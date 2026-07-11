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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Tag;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author pavlidis
 */
@Tag("geo")
@ExtendWith(NetworkAvailableExtension.class)
public class ExpressionExperimentBibRefFinderTest {

    @Test
    @Tag("slow")
    @NetworkAvailable(url = "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi")
    public void testLocatePrimaryReference() throws IOException {
        ExpressionExperimentBibRefFinder finder = new ExpressionExperimentBibRefFinder( Settings.getString( "entrez.efetch.apikey" ) );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( ExternalDatabases.GEO );
        de.setAccession( "GSE3023" );
        de.setExternalDatabase( ed );
        ee.setAccession( de );
        BibliographicReference bibref = finder.locatePrimaryReference( ee );
        assertNotNull( bibref );
        assertEquals( "Differential gene expression in anatomical compartments of the human eye.",
                bibref.getTitle() );
    }

    @Test
    @Tag("slow")
    @NetworkAvailable(url = "https://www.ncbi.nlm.nih.gov/geo/query/acc.cgi")
    public void testLocatePrimaryReferenceInvalidGSE() throws IOException {
        ExpressionExperimentBibRefFinder finder = new ExpressionExperimentBibRefFinder( Settings.getString( "entrez.efetch.apikey" ) );
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( ExternalDatabases.GEO );
        de.setAccession( "GSE30231111111111111" );
        de.setExternalDatabase( ed );
        ee.setAccession( de );
        BibliographicReference bibref = finder.locatePrimaryReference( ee );
        assertNull( bibref );
    }

    private static int parse( String soft ) throws IOException {
        return ExpressionExperimentBibRefFinder.parseSeriesPubMedId( new BufferedReader( new StringReader( soft ) ), "GSE99114" );
    }

    @Test
    public void testParseSeriesPubMedIdFromSoft() throws IOException {
        // the acc.cgi ...&targ=self&form=text view: !Series_pubmed_id is the authoritative link
        String soft = "^SERIES = GSE99114\n"
                + "!Series_title = A dataset\n"
                + "!Series_pubmed_id = 38064339\n"
                + "!Series_summary = something\n";
        assertEquals( 38064339, parse( soft ) );
    }

    @Test
    public void testParseSeriesPubMedIdReturnsFirstWhenMultiple() throws IOException {
        // multi-paper series: the first is taken as primary (curator confirms), the rest logged
        String soft = "^SERIES = GSE99114\n"
                + "!Series_pubmed_id = 38064339\n"
                + "!Series_pubmed_id = 30255127\n";
        assertEquals( 38064339, parse( soft ) );
    }

    @Test
    public void testParseSeriesPubMedIdAbsent() throws IOException {
        // a series with no linked paper (or a bad accession's error page) yields -1
        String soft = "^SERIES = GSE99114\n"
                + "!Series_title = A dataset with no publication\n";
        assertEquals( -1, parse( soft ) );
    }
}
