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

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceDao;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.gene.GeneProductDao;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @version $Id$
 */
public class BlatAssociationDaoImplTest extends BaseSpringContextTest {

    private String testGeneIdentifier = RandomStringUtils.randomAlphabetic( 4 );

    String testSequence = RandomStringUtils.random( 35, "ATGC" );
    String testSequenceName = RandomStringUtils.randomAlphabetic( 6 );
    private BlatAssociationDao blatAssociationDao;

    BlatResultDao blatResultDao;
    BioSequenceDao bioSequenceDao;
    GeneProductDao geneProductDao;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseTransactionalSpringContextTest#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        blatResultDao = ( BlatResultDao ) getBean( "blatResultDao" );
        bioSequenceDao = ( BioSequenceDao ) getBean( "bioSequenceDao" );
        geneProductDao = ( GeneProductDao ) getBean( "geneProductDao" );

        int numSequencesToCreate = 20;
        for ( int i = 0; i < numSequencesToCreate; i++ ) {
            BioSequence bs = this.getTestPersistentBioSequence();
            if ( i == 11 ) {
                bs.setSequence( testSequence );
                bs.setName( testSequenceName );
                this.bioSequenceDao.update( bs );
            }

            BlatResult br = this.getTestPersistentBlatResult( bs );
            br.setQuerySequence( bs );
            blatResultDao.update( br );

            BlatAssociation ba = BlatAssociation.Factory.newInstance();

            Gene g = this.getTestPeristentGene();

            GeneProduct gp = this.getTestPersistentGeneProduct( g );
            if ( i == 10 ) {
                g.setOfficialName( testGeneIdentifier ); // fixme need taxon too.
                gp.setGene( g );
                gp.setName( testGeneIdentifier );
                this.geneProductDao.update( gp );
            }

            ba.setGeneProduct( gp );
            ba.setBlatResult( br );
            ba.setBioSequence( bs );

            blatAssociationDao.create( ba );

        }
        this.flushSession();
    }

    /**
     * Test method for
     * {@link ubic.gemma.model.genome.sequenceAnalysis.BlatResultDaoImpl#find(ubic.gemma.model.genome.biosequence.BioSequence)}.
     */
    public final void testFindBioSequence() {
        BioSequence bs = BioSequence.Factory.newInstance();
        Taxon t = Taxon.Factory.newInstance();
        t.setScientificName( "Mus musculus" ); // has to match what the testpersistent object is.
        t.setIsSpecies( true );
        t.setIsGenesUsable( true );
        bs.setSequence( testSequence );
        bs.setTaxon( t );
        bs.setName( testSequenceName );
       
        BioSequence bsIn = this.bioSequenceDao.find( bs );
        assertNotNull( "Did not find " + bs, bsIn );

        Collection res = this.blatAssociationDao.find( bs );
        assertEquals( "Was seeking blatresults for sequence " + testSequenceName, 1, res.size() );
    }

    public final void testFindGene() {
        Gene g = Gene.Factory.newInstance();
        g.setOfficialName( testGeneIdentifier );

        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setName( testGeneIdentifier );

        g.getProducts().add( gp );
        gp.setGene( g );

        Collection res = this.blatAssociationDao.find( g );
        assertEquals( 1, res.size() );
    }

    /**
     * @param blatAssociationDao the blatAssociationDao to set
     */
    public void setBlatAssociationDao( BlatAssociationDao blatAssociationDao ) {
        this.blatAssociationDao = blatAssociationDao;
    }

}
