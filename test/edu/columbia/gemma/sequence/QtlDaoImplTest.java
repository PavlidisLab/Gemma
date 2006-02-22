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
package edu.columbia.gemma.sequence;

import java.util.Iterator;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.genome.Chromosome;
import edu.columbia.gemma.genome.ChromosomeDao;
import edu.columbia.gemma.genome.PhysicalLocation;
import edu.columbia.gemma.genome.PhysicalLocationDao;
import edu.columbia.gemma.genome.PhysicalMarker;
import edu.columbia.gemma.genome.PhysicalMarkerDao;
import edu.columbia.gemma.genome.Qtl;
import edu.columbia.gemma.genome.QtlDao;
import edu.columbia.gemma.genome.Taxon;
import edu.columbia.gemma.genome.TaxonDao;

/**
 * @author pavlidis
 * @author daq?
 * @version $Id$
 */
public class QtlDaoImplTest extends BaseDAOTestCase {

    QtlDao qtlDao = null;
    PhysicalLocationDao plDao = null;
    PhysicalMarkerDao pmDao = null;
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

    Taxon tx;
    Chromosome chrom;

    protected void setUp() throws Exception {
        super.setUp();

        qtlDao = ( QtlDao ) ctx.getBean( "qtlDao" );
        chromosomeDao = ( ChromosomeDao ) ctx.getBean( "chromosomeDao" );
        taxonDao = ( TaxonDao ) ctx.getBean( "taxonDao" );
        pmDao = ( PhysicalMarkerDao ) ctx.getBean( "physicalMarkerDao" );
        plDao = ( PhysicalLocationDao ) ctx.getBean( "physicalLocationDao" );
        sf = ( SessionFactory ) ctx.getBean( "sessionFactory" );

        tx = taxonDao.findByCommonName( TAXON );
        if ( tx == null ) {
            tx = Taxon.Factory.newInstance();
            tx.setCommonName( TAXON );
            tx.setNcbiId( 4994949 );
            tx = taxonDao.create( tx );
        }

        // need a chromosome
        chrom = Chromosome.Factory.newInstance();
        chrom.setName( CHROM_NAME );
        chrom.setTaxon( tx );
        chrom = chromosomeDao.create( chrom );

        // need physical locations
        for ( int i = 0; i < NUM_LOCS; i++ ) {
            pls[i] = PhysicalLocation.Factory.newInstance();
            pls[i].setChromosome( chrom );
            pls[i].setNucleotide( new Long( LOC_SPACING * i ) );

            pls[i] = ( PhysicalLocation ) plDao.create( pls[i] );

            pms[i] = PhysicalMarker.Factory.newInstance();
            pms[i].setPhysicalLocation( pls[i] );
            AuditTrail ad = AuditTrail.Factory.newInstance();
            pms[i].setAuditTrail( ( AuditTrail ) this.getPersisterHelper().persist( ad ) );
            pms[i] = ( PhysicalMarker ) pmDao.create( pms[i] );
        }

        // create qtls - one for every two locations, so they might be 2000-4000, 4000-6000 etc.
        for ( int i = 0, j = 0; j < NUM_LOCS - 1; i++, j += 2 ) {
            Qtl q = Qtl.Factory.newInstance();
            q.setName( "qtl-" + i );
            q.setStartMarker( pms[j] );
            q.setEndMarker( pms[j + 1] );
            AuditTrail ad = AuditTrail.Factory.newInstance();
            q.setAuditTrail( ( AuditTrail ) this.getPersisterHelper().persist( ad ) );
            qtls[i] = ( Qtl ) qtlDao.create( q );
        }

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        for ( int i = 0, j = 0; j < NUM_LOCS - 1; i++, j += 2 ) {
            qtlDao.remove( qtls[i] );
        }
        for ( int i = 0; i < NUM_LOCS; i++ ) {
            pmDao.remove( pms[i] ); // cascade will delete physical location.
        }

        chromosomeDao.remove( chrom );
 //       taxonDao.remove( tx ); // FIXME put this back when possible.
    }

    /**
     * Deferences the nucleotides for direct comparisons.
     * 
     * @throws Exception
     */
    public final void testFindByPhysicalLocationNucleotideQuery() throws Exception {

        String query = "from QtlImpl qtl where qtl.startMarker.physicalLocation.chromosome ="
                + " :chrom and qtl.startMarker.physicalLocation.nucleotide >= "
                + " :start and qtl.endMarker.physicalLocation.nucleotide <= :end";

        Session sess = sf.openSession();
        Transaction trans = sess.beginTransaction();

        Query q = sess.createQuery( query );

        q.setParameter( "chrom", pms[LEFT_TEST_MARKER].getPhysicalLocation().getChromosome() );
        q.setParameter( "start", pms[LEFT_TEST_MARKER].getPhysicalLocation().getNucleotide() );
        q.setParameter( "end", pms[RIGHT_TEST_MARKER].getPhysicalLocation().getNucleotide() );

        for ( Iterator it = q.iterate(); it.hasNext(); ) {
            Qtl qtl = ( Qtl ) it.next();
            log.debug( "Qtl found by nucleotide: " + qtl.getName() + " with start location "
                    + qtl.getStartMarker().getPhysicalLocation().getNucleotide() );
        }
        sess.flush();
        trans.commit();
        sess.close();
    }

    /**
     * Query uses the physical locations of the markers without dereferencing the nucleotides.
     * 
     * @throws Exception
     */
    public final void testFindByPhysicalLocationQuery() throws Exception {

        String query = "from QtlImpl qtl where qtl.startMarker.physicalLocation.chromosome ="
                + " :chrom and qtl.startMarker.physicalLocation >= "
                + " :start and qtl.endMarker.physicalLocation <= :end";

        Session sess = sf.openSession();
        Transaction trans = sess.beginTransaction();

        Query q = sess.createQuery( query );

        q.setParameter( "chrom", pms[LEFT_TEST_MARKER].getPhysicalLocation().getChromosome() );
        q.setParameter( "start", pms[LEFT_TEST_MARKER].getPhysicalLocation() );
        q.setParameter( "end", pms[RIGHT_TEST_MARKER].getPhysicalLocation() );

        for ( Iterator it = q.iterate(); it.hasNext(); ) {
            Qtl qtl = ( Qtl ) it.next();
            log.debug( "Qtl found: " + qtl.getName() + " with start location "
                    + qtl.getStartMarker().getPhysicalLocation().getNucleotide() );
        }
        sess.flush();
        trans.commit();
        sess.close();

    }
}
