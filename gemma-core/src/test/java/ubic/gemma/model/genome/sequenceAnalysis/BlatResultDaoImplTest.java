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
package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BlatResultDaoImplTest extends BaseSpringContextTest {
    String testSequence = RandomStringUtils.random( 100, "ATCG" );
    String testSequenceName = RandomStringUtils.randomAlphabetic( 6 );
    BlatResultDao blatResultDao;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseTransactionalSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {

        super.onSetUpInTransaction();
        blatResultDao = ( BlatResultDao ) getBean( "blatResultDao" );
        for ( int i = 0; i < 20; i++ ) {
            BioSequence bs = this.getTestPersistentBioSequence();
            if ( i == 10 ) {
                bs.setSequence( testSequence );
                bs.setName( testSequenceName );
            }
            BlatResult br = this.getTestPersistentBlatResult( bs );

            br.setQuerySequence( bs );
        }

    }

    /**
     * Test method for
     * {@link ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoImpl#find(ubic.gemma.model.genome.biosequence.BioSequence)}.
     */
    public final void testFindBioSequence() {
        BioSequence bs = BioSequence.Factory.newInstance();
        bs.setSequence( testSequence );
        bs.setName( testSequenceName );

        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" ); // has to match what is used in the getTestPersistentBioSequence method.
        t.setScientificName( "Mus musculus" );
        t.setIsSpecies( true );
        t.setIsGenesUsable( true );
        bs.setTaxon( t );

        Collection res = this.blatResultDao.findByBioSequence( bs );
        assertEquals( 1, res.size() );
    }

}
