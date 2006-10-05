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
package ubic.gemma.loader.description;

import java.util.Collection;

import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.OntologyEntry;
// import ubic.gemma.model.common.description.OntologyEntryService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GeneOntologyLoaderIntegrationTest extends BaseSpringContextTest {

    /**
     * Big test - don't run routinely.
     * 
     * @throws Exception
     */
    public void testLoad() throws Exception {
        GeneOntologyFetcher fetcher = new GeneOntologyFetcher();
        Collection<LocalFile> files = fetcher.fetch( "GO" );
        assertEquals( 1, files.size() );
        LocalFile f = files.iterator().next();
        GeneOntologyLoader loader = new GeneOntologyLoader();
        loader.setPersisterHelper( ( PersisterHelper ) this.getBean( "persisterHelper" ) );
        // OntologyEntryService ontologyEntryService = ( OntologyEntryService ) this.getBean( "ontologyEntryService" );
        Collection<OntologyEntry> results = loader.load( f.asFile() );

        // assertEquals( 22385, results.size() ); // true as of 10/3/06
        assertTrue( results.size() > 22000 && results.size() < 30000 ); // sanity; should be true for a while.
        boolean ok = false;
        for ( OntologyEntry entry : results ) {
            /*
             * SELECT oe.accession, oe2.accession FROM `gemdtest`.`database_entry` as oe inner join
             * ontology_entry_associations as oea on oea.ontology_entries_fk=oe.id inner join database_entry oe2 on
             * oe2.id=oea.associations_fk where oe.accession='GO:0045182'
             */
            if ( entry.getAccession().equals( "GO:0045182" ) ) {
                Collection<OntologyEntry> associates = entry.getAssociations();
                assertNotNull( associates );
                for ( OntologyEntry entry2 : associates ) {
                    if ( entry2.getAccession().equals( "GO:0030371" ) ) {
                        Collection<OntologyEntry> associates2 = entry2.getAssociations();
                        assertEquals( 2, associates2.size() );
                        for ( OntologyEntry entry3 : associates2 ) {
                            if ( !( entry3.getAccession().equals( "GO:0000901" ) || entry3.getAccession().equals(
                                    "GO:0000900" ) ) ) {
                                fail( "Got" + entry3.getAccession() );
                            }
                        }
                        ok = true;
                    }
                }
                // # GO:0030371 : translation repressor activity ( 38 )
                //
                // * GO:0000901 : translation repressor activity, non-nucleic acid binding ( 1 )
                // * GO:0000900 : translation repressor activity, nucleic acid binding ( 17 )

            }
        }
        assertTrue( ok );
    }
}
