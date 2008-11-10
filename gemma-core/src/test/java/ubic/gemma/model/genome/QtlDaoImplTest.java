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

import java.util.Collection;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * @author pavlidis
 * @author daq?
 * @version $Id$
 */
public class QtlDaoImplTest extends BaseSpringContextTest {

    BaseQtlDao qtlDao = null;
    PhysicalLocationDao physicalLocationDao = null;
    PhysicalMarkerDao physicalMarkerDao = null;
    ChromosomeDao chromosomeDao = null;
    TaxonDao taxonDao = null;
    SessionFactory sf = null;

    private static final int NUM_LOCS = 10;
    private static final int LOC_SPACING = 1000;
    private static final String CHROM_NAME = "12";
    private static final String TAXON = "thingy";
    private static final int LEFT_TEST_MARKER = 2;
    private static final int RIGHT_TEST_MARKER = 5;
    PhysicalMarker[] pms = new PhysicalMarker[NUM_LOCS];
    PhysicalLocation[] pls = new PhysicalLocation[NUM_LOCS];
    Qtl[] qtls = new Qtl[NUM_LOCS / 2];

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        sf = ( SessionFactory ) getBean( "sessionFactory" );

        Taxon tx = Taxon.Factory.newInstance();
        tx.setCommonName( TAXON );
        tx.setNcbiId( 4994949 );
        tx = taxonDao.findOrCreate( tx );

        // need a chromosome
        Chromosome chrom = Chromosome.Factory.newInstance();
        chrom.setName( CHROM_NAME );
        chrom.setTaxon( tx );
        chrom = chromosomeDao.create( chrom );

        // need physical locations
        for ( int i = 0; i < NUM_LOCS; i++ ) {
            pls[i] = PhysicalLocation.Factory.newInstance();
            pls[i].setChromosome( chrom );
            pls[i].setNucleotide( new Long( LOC_SPACING * i ) );

            pls[i] = ( PhysicalLocation ) physicalLocationDao.create( pls[i] );

            pms[i] = PhysicalMarker.Factory.newInstance();
            pms[i].setPhysicalLocation( pls[i] );
            AuditTrail ad = AuditTrail.Factory.newInstance();
            pms[i].setAuditTrail( ( AuditTrail ) persisterHelper.persist( ad ) );
            pms[i] = ( PhysicalMarker ) physicalMarkerDao.create( pms[i] );
        }

        // create qtls - one for every two locations, so they might be 2000-4000, 4000-6000 etc.
        for ( int i = 0, j = 0; j < NUM_LOCS - 1; i++, j += 2 ) {
            Qtl q = Qtl.Factory.newInstance();
            q.setName( "qtl-" + i );
            q.setStartMarker( pms[j] );
            q.setEndMarker( pms[j + 1] );
            AuditTrail ad = AuditTrail.Factory.newInstance();
            q.setAuditTrail( ( AuditTrail ) persisterHelper.persist( ad ) );
            qtls[i] = ( Qtl ) qtlDao.create( q );
        }

    }

    /**
     * Deferences the nucleotides for direct comparisons.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public final void testFindByPhysicalLocationNucleotideQuery() throws Exception {

        String query = "from QtlImpl qtl where qtl.startMarker.physicalLocation.chromosome ="
                + " :chrom and qtl.startMarker.physicalLocation.nucleotide >= "
                + " :start and qtl.endMarker.physicalLocation.nucleotide <= :end";

        Session sess = sf.openSession();
        // Transaction trans = sess.beginTransaction(); // we're already in a transaction in this type of test.

        Query q = sess.createQuery( query );

        q.setParameter( "chrom", pms[LEFT_TEST_MARKER].getPhysicalLocation().getChromosome() );
        q.setParameter( "start", pms[LEFT_TEST_MARKER].getPhysicalLocation().getNucleotide() );
        q.setParameter( "end", pms[RIGHT_TEST_MARKER].getPhysicalLocation().getNucleotide() );

        for ( Qtl qtl : ( Collection<Qtl> ) q.list() ) {
            log.debug( "Qtl found by nucleotide: " + qtl.getName() + " with start location "
                    + qtl.getStartMarker().getPhysicalLocation().getNucleotide() );
        }
        // sess.flush();
        // trans.commit();
        // sess.close();
    }

    /**
     * Query uses the physical locations of the markers without dereferencing the nucleotides.
     * 
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public final void testFindByPhysicalLocationQuery() throws Exception {

        String query = "from QtlImpl qtl where qtl.startMarker.physicalLocation.chromosome ="
                + " :chrom and qtl.startMarker.physicalLocation >= "
                + " :start and qtl.endMarker.physicalLocation <= :end";

        Session sess = sf.openSession();
        // Transaction trans = sess.beginTransaction();

        Query q = sess.createQuery( query );

        q.setParameter( "chrom", pms[LEFT_TEST_MARKER].getPhysicalLocation().getChromosome() );
        q.setParameter( "start", pms[LEFT_TEST_MARKER].getPhysicalLocation() );
        q.setParameter( "end", pms[RIGHT_TEST_MARKER].getPhysicalLocation() );

        for ( Qtl qtl : ( Collection<Qtl> ) q.list() ) {
            log.debug( "Qtl found: " + qtl.getName() + " with start location "
                    + qtl.getStartMarker().getPhysicalLocation().getNucleotide() );
        }
        // sess.flush();
        // trans.commit();
        // sess.close();

    }

    /**
     * @param chromosomeDao The chromosomeDao to set.
     */
    public void setChromosomeDao( ChromosomeDao chromosomeDao ) {
        this.chromosomeDao = chromosomeDao;
    }

    /**
     * @param physicalLocationDao The physicalLocationDao to set.
     */
    public void setPhysicalLocationDao( PhysicalLocationDao physicalLocationDao ) {
        this.physicalLocationDao = physicalLocationDao;
    }

    /**
     * @param physicalMarkerDao The physicalMarkerDao to set.
     */
    public void setPhysicalMarkerDao( PhysicalMarkerDao physicalMarkerDao ) {
        this.physicalMarkerDao = physicalMarkerDao;
    }

    /**
     * @param qtlDao The qtlDao to set.
     */
    public void setQtlDao( BaseQtlDao qtlDao ) {
        this.qtlDao = qtlDao;
    }

    /**
     * @param taxonDao The taxonDao to set.
     */
    public void setTaxonDao( TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
    }
}
