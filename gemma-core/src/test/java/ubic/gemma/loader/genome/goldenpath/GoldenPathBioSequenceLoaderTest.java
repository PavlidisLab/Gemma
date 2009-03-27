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
package ubic.gemma.loader.genome.goldenpath;

import java.io.InputStream;

import ubic.gemma.externalDb.GoldenPathDumper;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class GoldenPathBioSequenceLoaderTest extends BaseSpringContextTest {

    public void testGetTranscriptBioSequences() throws Exception {

        Taxon taxon = taxonService.findByCommonName( "mouse" );

        GoldenPathBioSequenceLoader gp = new GoldenPathBioSequenceLoader( taxon );

        ExternalDatabaseService externalDatabaseService = ( ExternalDatabaseService ) this
                .getBean( "externalDatabaseService" );
        BioSequenceService bioSequenceService = ( BioSequenceService ) this.getBean( "bioSequenceService" );

        gp.setExternalDatabaseService( externalDatabaseService );
        gp.setBioSequenceService( bioSequenceService );

        InputStream is = this.getClass().getResourceAsStream( "/data/loader/genome/goldenPathSequenceTest.txt" );
        gp.load( is );

    }

    public void testGetTranscriptBioSequencesFromDatabase() throws Exception {

        Taxon taxon = taxonService.findByCommonName( "mouse" );

        GoldenPathBioSequenceLoader gp = new GoldenPathBioSequenceLoader( taxon );

        GoldenPathDumper dumper = new GoldenPathDumper( taxon );

        ExternalDatabaseService externalDatabaseService = ( ExternalDatabaseService ) this
                .getBean( "externalDatabaseService" );

        BioSequenceService bioSequenceService = ( BioSequenceService ) this.getBean( "bioSequenceService" );

        gp.setExternalDatabaseService( externalDatabaseService );
        gp.setBioSequenceService( bioSequenceService );

        gp.setLimit( 20 );
        gp.load( dumper );
        
    }

}
