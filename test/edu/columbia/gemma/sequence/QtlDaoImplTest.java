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

    PhysicalMarker[] pms = new PhysicalMarker[10];
    PhysicalLocation[] pls = new PhysicalLocation[10];
    Qtl[] qtls = new Qtl[5];

    Chromosome chrom;

    protected void setUp() throws Exception {
        super.setUp();

        qtlDao = ( QtlDao ) ctx.getBean( "qtlDao" );
        chromosomeDao = ( ChromosomeDao ) ctx.getBean( "chromosomeDao" );
        taxonDao = ( TaxonDao ) ctx.getBean( "taxonDao" );
        pmDao = ( PhysicalMarkerDao ) ctx.getBean( "physicalMarkerDao" );
        plDao = ( PhysicalLocationDao ) ctx.getBean( "physicalLocationDao" );
        sf = ( SessionFactory ) ctx.getBean( "sessionFactory" );

        // set up some dummy data. FIXME this should be put elsewhere.
        Taxon tx = taxonDao.findByCommonName( "mouse" );
        if ( tx == null ) {
            tx = Taxon.Factory.newInstance();
            tx.setCommonName( "mouse" );
            tx.setNcbiId( 9609 );
            tx = taxonDao.create( tx );
        }

        // need a chromosome
        chrom = Chromosome.Factory.newInstance();
        chrom.setName( "12" );
        chrom.setTaxon( tx );
        chrom = chromosomeDao.create( chrom );

        // need physical locations
        for ( int i = 0; i < pls.length; i++ ) {
            pls[i] = PhysicalLocation.Factory.newInstance();
            pls[i].setChromosome( chrom );
            pls[i].setNucleotide( new Integer( 1000 * i ) );

            pls[i] = ( PhysicalLocation ) plDao.create( pls[i] );
        }

        // create some markers.
        for ( int i = 0; i < pms.length; i++ ) {
            pms[i] = PhysicalMarker.Factory.newInstance();
            pms[i].setPhysicalLocation( pls[i] );
            pms[i] = ( PhysicalMarker ) pmDao.create( pms[i] );
        }

        // create qtls
        for ( int i = 0, j = 0; j < pms.length - 1; i++, j += 2 ) {
            Qtl q = Qtl.Factory.newInstance();
            q.setName( "qtl-" + i );
            q.setStartMarker( pms[j] );
            q.setEndMarker( pms[j + 1] );
            qtls[i] = ( Qtl ) qtlDao.create( q );
        }

    }

    public final void testFindByPhysicalLocation() {

        PhysicalLocation p1 = PhysicalLocation.Factory.newInstance();

        PhysicalLocation p2 = PhysicalLocation.Factory.newInstance();
        p2.setChromosome( chrom );
        p2.setNucleotide( new Integer( 10000 ) );

        PhysicalMarker m1 = PhysicalMarker.Factory.newInstance();
        m1.setPhysicalLocation( p1 );

        PhysicalMarker m2 = PhysicalMarker.Factory.newInstance();
        m2.setPhysicalLocation( p2 );

        Collection qs = qtlDao.findByPhysicalLocation( p1 );

        assertTrue( qs.size() != 0 );

        qs = qtlDao.findByPhysicalMarkers( m1, m2 );

        assertTrue( qs.size() != 0 );

    }

    /**
     * @throws Exception
     */
    public final void testFindByPhysicalLocationQuery() throws Exception {

        // String query = "from QtlImpl qtl where qtl.startMarker.physicalLocation.chromosome ="
        // + " :chrom and qtl.startMarker.physicalLocation.nucleotide >"
        // + " :start and qtl.endMarker.physicalLocation.nucleotide < :end";

        String query = "from QtlImpl qtl where qtl.startMarker.physicalLocation.chromosome = :chrom ";

        Session sess = sf.openSession();
        Transaction trans = sess.beginTransaction();

        Query q = sess.createQuery( query );

        q.setParameter( "chrom", pms[1].getPhysicalLocation().getChromosome() );
        // q.setParameter( "start", pms[1].getPhysicalLocation().getNucleotide() );
        // q.setParameter( "end", pms[3].getPhysicalLocation().getNucleotide() );
        // q.setParameter( "start", pms[1] );
        // q.setParameter( "end", pms[3] );

        for ( Iterator it = q.iterate(); it.hasNext(); ) {
            Qtl qtl = ( Qtl ) it.next();
            log.debug( "Qtl found: " + qtl.getName() );
        }
        trans.commit();
    }

}
