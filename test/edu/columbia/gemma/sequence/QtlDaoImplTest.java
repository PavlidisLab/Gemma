package edu.columbia.gemma.sequence;

import java.util.Collection;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.sequence.gene.Taxon;

public class QtlDaoImplTest extends BaseDAOTestCase {

    // FIXME should be QtlDao, not base. This is an andromda problem.
    QtlDaoBase dao = null;

    protected void setUp() throws Exception {
        super.setUp();

        dao = ( QtlDaoBase ) ctx.getBean( "qtlDaoBase" );

    }

    /*
     * Class under test for java.util.Collection findByLocation(edu.columbia.gemma.sequence.PhysicalLocation)
     */
    public final void testFindByLocationPhysicalLocation() {
        Chromosome chrom = Chromosome.Factory.newInstance();
        chrom.setName( "12" );

        Taxon tx = Taxon.Factory.newInstance();
        tx.setCommonName( "mouse" );
        tx.setNcbiId( 9609 );

        chrom.setTaxon( tx );

        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        pl.setChromosome( chrom );
        pl.setNucleotide( new Integer( 1000 ) );
        Collection qtls = dao.findByLocation( pl );

        log.debug( "We made it" );

    }

}
