package edu.columbia.gemma.sequence;

import java.util.Collection;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.sequence.gene.Taxon;
import edu.columbia.gemma.sequence.gene.TaxonDao;

public class QtlDaoImplTest extends BaseDAOTestCase {

    QtlDao qtlDao = null;
    ChromosomeDao chromosomeDao = null;
    TaxonDao taxonDao = null;

    protected void setUp() throws Exception {
        super.setUp();

        qtlDao = ( QtlDao ) ctx.getBean( "qtlDao" );
        chromosomeDao = ( ChromosomeDao ) ctx.getBean( "chromosomeDao" );
        taxonDao = ( TaxonDao ) ctx.getBean( "taxonDao" );
    }

    /*
     * Class under test for java.util.Collection findByLocation(edu.columbia.gemma.sequence.PhysicalLocation)
     */
    public final void testFindByPhysicalLocation() {

        // set up some dummy data. FIXME this should be put elsewhere.F
        Taxon tx = Taxon.Factory.newInstance();
        tx.setCommonName( "mouse" );
        tx.setNcbiId( 9609 );
        tx = ( Taxon ) taxonDao.create( tx );

        Chromosome chrom = Chromosome.Factory.newInstance();
        chrom.setName( "12" );
        chrom.setTaxon( tx );
        chrom = ( Chromosome ) chromosomeDao.create( chrom );

        // create some markers.
        PhysicalMarker mka = PhysicalMarker.Factory.newInstance();
        PhysicalMarker mkb = PhysicalMarker.Factory.newInstance();
        PhysicalMarker mkc = PhysicalMarker.Factory.newInstance();
        PhysicalMarker mkd = PhysicalMarker.Factory.newInstance();

        // create a qtl
        Qtl qtla = Qtl.Factory.newInstance();
        Qtl qtlb = Qtl.Factory.newInstance();

        PhysicalLocation pl = PhysicalLocation.Factory.newInstance();
        pl.setChromosome( chrom );
        pl.setNucleotide( new Integer( 1000 ) );

        Collection qtls = qtlDao.findByPhysicalMarkers( pl );
        
        assertTrue(qtls.size() != 0);

    }
}
