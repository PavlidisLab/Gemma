package ubic.gemma.rest;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;
import ubic.gemma.rest.util.FilteredAndPaginatedResponseDataObject;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.*;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TaxaWebServiceTest extends BaseJerseyIntegrationTest {

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private TaxaWebService taxaWebService;

    /* fixtures */
    private Taxon taxon;

    @Before
    public void createFixtures() {
        taxon = new Taxon();
        taxon.setNcbiId( RandomUtils.nextInt() );
        taxon.setCommonName( "common_name_" + RandomUtils.nextInt() );
        taxon.setScientificName( "scientific_name_" + RandomStringUtils.randomAlphabetic( 10 ) );
        taxon.setIsGenesUsable( false );
        taxon = taxonService.create( taxon );
    }

    @After
    public void removeFixtures() {
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
}
