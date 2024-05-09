package ubic.gemma.rest;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.core.Response;
import java.util.List;

import static ubic.gemma.rest.util.Assertions.assertThat;

public class TaxaWebServiceTest extends BaseJerseyIntegrationTest {

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private TaxaWebService taxaWebService;

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
    public void testTaxonById() {
        ResponseDataObject<List<TaxonValueObject>> response = taxaWebService.getTaxaByIds( TaxonArrayArg.valueOf( String.valueOf( taxon.getId() ) ) );
        assertThat( response.getData() ).hasSize( 1 );
    }

    @Test
    public void testTaxonByNcbiId() {
        ResponseDataObject<List<TaxonValueObject>> response = taxaWebService.getTaxaByIds( TaxonArrayArg.valueOf( String.valueOf( taxon.getNcbiId() ) ) );
        assertThat( response.getData() ).hasSize( 1 );
    }

    @Test
    public void testTaxonByCommonName() {
        ResponseDataObject<List<TaxonValueObject>> response = taxaWebService.getTaxaByIds( TaxonArrayArg.valueOf( taxon.getCommonName() ) );
        assertThat( response.getData() ).hasSize( 1 );
    }

    @Test
    public void testTaxonByScientificName() {
        ResponseDataObject<List<TaxonValueObject>> response = taxaWebService.getTaxaByIds( TaxonArrayArg.valueOf( taxon.getScientificName() ) );
        assertThat( response.getData() ).hasSize( 1 );
    }

    @Test
    public void testTaxonDatasetsByNcbiId() {
        FilteredAndPaginatedResponseDataObject<ExpressionExperimentValueObject> response = taxaWebService.getTaxonDatasets(
                TaxonArg.valueOf( taxon.getNcbiId().toString() ),
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ) );
        assertThat( response.getData() ).isEmpty();
    }

    @Test
    public void testTaxonGenes() {
        assertThat( target( "/taxa/" + taxon.getCommonName() + "/genes" ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    public void testTaxonGenesByIds() {
        assertThat( target( "/taxa/" + taxon.getCommonName() + "/genes/" + gene.getOfficialSymbol() ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    public void testTaxonGeneProbes() {
        assertThat( target( "/taxa/" + taxon.getCommonName() + "/genes/" + gene.getOfficialSymbol() + "/probes" ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    public void testTaxonGeneGoTerms() {
        assertThat( target( "/taxa/" + taxon.getCommonName() + "/genes/" + gene.getOfficialSymbol() + "/goTerms" ).request().get() )
                .hasStatus( Response.Status.OK );
    }

    @Test
    public void testTaxonGeneLocations() {
        assertThat( target( "/taxa/" + taxon.getCommonName() + "/genes/" + gene.getOfficialSymbol() + "/locations" ).request().get() )
                .hasStatus( Response.Status.OK );
    }
}
