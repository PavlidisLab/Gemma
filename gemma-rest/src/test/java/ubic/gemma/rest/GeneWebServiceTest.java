package ubic.gemma.rest;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;

import javax.ws.rs.core.Response;

import static ubic.gemma.rest.util.Assertions.assertThat;

public class GeneWebServiceTest extends BaseJerseyIntegrationTest {

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    /* fixtures */
    private Taxon taxon;
    private Gene gene;

    @Before
    public void createFixtures() {
        taxon = new Taxon();
        taxon.setNcbiId( RandomUtils.nextInt() );
        taxon.setCommonName( "common_name_" + RandomUtils.nextInt() );
        taxon.setScientificName( "scientific_name_" + RandomStringUtils.randomAlphabetic( 10 ) );
        taxon.setIsGenesUsable( false );
        taxon = taxonService.create( taxon );
        gene = new Gene();
        gene.setTaxon( taxon );
        gene.setNcbiGeneId( RandomUtils.nextInt() );
        gene.setEnsemblId( "ensembl_id_" + RandomStringUtils.randomAlphabetic( 10 ) );
        gene.setOfficialSymbol( "official_symbol_" + RandomStringUtils.randomAlphabetic( 10 ) );
        gene = geneService.create( gene );
    }

    @After
    public void removeFixtures() {
        geneService.remove( gene );
        taxonService.remove( taxon );
    }

    @Test
    public void testGenes() {
        assertThat( target( "/genes" ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    public void testGenesByIds() {
        assertThat( target( "/genes/" + gene.getOfficialSymbol() ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    public void testGeneProbes() {
        assertThat( target( "/genes/" + gene.getOfficialSymbol() + "/probes" ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    public void testGeneGoTerms() {
        assertThat( target( "/genes/" + gene.getOfficialSymbol() + "/goTerms" ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    public void testGeneLocations() {
        assertThat( target( "/genes/" + gene.getOfficialSymbol() + "/locations" ).request().get() )
                .hasStatus( Response.Status.OK );
    }
}