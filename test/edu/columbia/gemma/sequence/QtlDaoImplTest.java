package edu.columbia.gemma.sequence;

import java.util.Collection;
import java.util.Iterator;

import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;

import edu.columbia.gemma.BaseDAOTestCase;
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
    private static final String TAXON = "mouse";
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
            tx.setCommonName( "mouse" );
            tx.setNcbiId( 9609 );
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
            pls[i].setNucleotide( new Integer( LOC_SPACING * i ) );

            pls[i] = ( PhysicalLocation ) plDao.create( pls[i] );

            pms[i] = PhysicalMarker.Factory.newInstance();
            pms[i].setPhysicalLocation( pls[i] );
            pms[i] = ( PhysicalMarker ) pmDao.create( pms[i] );
        }

        // create qtls - one for every two locations, so they might be 2000-4000, 4000-6000 etc.
        for ( int i = 0, j = 0; j < NUM_LOCS - 1; i++, j += 2 ) {
            Qtl q = Qtl.Factory.newInstance();
            q.setName( "qtl-" + i );
            q.setStartMarker( pms[j] );
            q.setEndMarker( pms[j + 1] );
            qtls[i] = ( Qtl ) qtlDao.create( q );
        }

    }

    protected void tearDown() throws Exception {
        super.tearDown();

        chromosomeDao.remove( chrom );
        taxonDao.remove( tx );
        for ( int i = 0; i < NUM_LOCS; i++ ) {
            pmDao.remove( pms[i] ); // cascade will delete physical location.
            // plDao.remove( pls[i] );
        }

        for ( int i = 0, j = 0; j < NUM_LOCS - 1; i++, j += 2 ) {
            qtlDao.remove( qtls[i] );
        }

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
