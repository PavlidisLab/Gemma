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
package ubic.gemma.model.genome;

import static org.junit.Assert.assertNotNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BioSequencePersistTest extends BaseSpringContextTest {

    BioSequence bs;

    @Before
    public void onSetUpInTransaction() {

        bs = BioSequence.Factory.newInstance();

        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setIsSpecies( true );
        t.setIsGenesUsable( true );
        bs.setTaxon( t );

        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( "Genbank" );

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();
        de.setExternalDatabase( ed );
        de.setAccession( RandomStringUtils.randomAlphanumeric( 10 ) );

        bs.setName( RandomStringUtils.randomAlphanumeric( 10 ) );
        bs.setSequenceDatabaseEntry( de );
    }

    @After
    public void onTearDownInTransaction() {
        BioSequenceService bss = this.getBean( BioSequenceService.class );
        bss.remove( bs );
    }

    @Test
    public final void testPersistBioSequence() {
        bs = ( BioSequence ) persisterHelper.persist( bs );
        assertNotNull( bs.getId() );
    }

}
