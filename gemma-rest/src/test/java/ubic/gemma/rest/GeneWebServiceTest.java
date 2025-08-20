package ubic.gemma.rest;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;

import javax.ws.rs.core.Response;
import java.util.Random;

import static ubic.gemma.rest.util.Assertions.assertThat;

public class GeneWebServiceTest extends BaseJerseyIntegrationTest {

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    /* fixtures */
    private Taxon taxon;
    private Gene gene, gene2;

    @Before
    public void createFixtures() {
        Random random = new Random( 123L );
        taxon = new Taxon();
        taxon.setNcbiId( random.nextInt() );
        taxon.setCommonName( "common_name_" + random.nextInt() );
        taxon.setScientificName( "scientific_name_" + RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        taxon.setIsGenesUsable( false );
        taxon = taxonService.create( taxon );
        gene = new Gene();
        gene.setTaxon( taxon );
        gene.setNcbiGeneId( random.nextInt() );
        gene.setEnsemblId( "ensembl_id_" + RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        gene.setOfficialSymbol( "official_symbol_" + RandomStringUtils.insecure().nextAlphabetic( 10 ) );
        gene = geneService.create( gene );
    }

    @After
    public void removeFixtures() {
        geneService.remove( gene );
        if ( gene2 != null ) {
            geneService.remove( gene2 );
        }
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
    public void testGeneProbesWhenIdentifierIsAmbiguous() {
        gene2 = new Gene();
        gene2.setOfficialSymbol( gene.getOfficialSymbol() );
        gene2 = geneService.create( gene2 );
        assertThat( target( "/genes/" + gene.getOfficialSymbol() + "/probes" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST )
                .entity()
                .hasFieldOrPropertyWithValue( "error.message", "Gene identifier " + gene.getOfficialSymbol() + " matches more than one gene, supply a taxon to disambiguate or use a different type of identifier such as an NCBI or Ensembl ID." );
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