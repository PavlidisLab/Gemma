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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.entrez.EntrezUtils;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The over-the-wire half of {@link ExpressionExperimentBibRefFinderTest}, which stubs
 * {@link ubic.gemma.core.loader.expression.geo.service.GeoBrowser} and never leaves the JVM.
 *
 * <p>Split into its own class so the unit tests stay in the default suite: a class-level
 * {@code @Tag("slow")} filters every method in the class, so the two cannot live together.</p>
 *
 * <p>These reach Entrez twice per method — {@code esearch}/{@code esummary} on {@code db=gds} to
 * resolve the accession, then {@code efetch} on {@code db=pubmed} to build the reference. They are
 * the only remaining check that the accession-to-UID mapping is done the supported way rather than
 * by assuming GEO's internal {@code 2 + zero-padded} UID encoding.</p>
 *
 * @author pavlidis
 */
@Tag("pubmed")
@Tag("geo")
// paired with the descriptive markers above so the class is filtered from the default suite: geo
// and pubmed are not excluded on their own (tag taxonomy in pom.xml).
@Tag("slow")
@NetworkAvailable(url = EntrezUtils.ESEARCH)
@ExtendWith(NetworkAvailableExtension.class)
public class ExpressionExperimentBibRefFinderNetworkTest {

    private final ExpressionExperimentBibRefFinder finder =
            new ExpressionExperimentBibRefFinder( Settings.getString( "entrez.efetch.apikey" ) );

    private static ExpressionExperiment geoExperiment( String accession ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( ExternalDatabases.GEO );
        de.setAccession( accession );
        de.setExternalDatabase( ed );
        ee.setAccession( de );
        return ee;
    }

    @Test
    public void testLocatePrimaryReference() throws IOException {
        BibliographicReference bibref = finder.locatePrimaryReference( geoExperiment( "GSE3023" ) );
        assertNotNull( bibref );
        assertEquals( "Differential gene expression in anatomical compartments of the human eye.",
                bibref.getTitle() );
    }

    /**
     * The id behind the assertion above, without the second PubMed round trip. Kept separate because
     * this is the hop that moved off {@code acc.cgi}: if Entrez resolution regresses, this fails
     * first and names the reason.
     */
    @Test
    public void testLocatePubMedId() throws IOException {
        assertEquals( 16168081, finder.locatePubMedId( "GSE3023" ) );
    }
}
