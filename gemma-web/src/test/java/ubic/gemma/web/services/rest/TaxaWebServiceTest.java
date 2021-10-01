package ubic.gemma.web.services.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonValueObject;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.web.services.rest.util.PaginatedResponseDataObject;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.*;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TaxaWebServiceTest extends BaseSpringWebTest {

    @Autowired
    private TaxaWebService taxaWebService;

    /* fixtures */
    private Taxon taxon;

    @Before
    public void setUp() {
        taxon = testHelper.getTestPersistentTaxon();
    }

    @After
    public void tearDown() {
        // TODO: we should remove it, but the taxon is actually a singleton used in other tests
        // taxonService.remove( taxon );
    }

    @Test
    public void testTaxonById() {
        ResponseDataObject<List<TaxonValueObject>> response = taxaWebService.taxa( TaxonArrayArg.valueOf( String.valueOf( taxon.getId() ) ), new MockHttpServletResponse() );
        assertThat( response.getData() ).hasSize( 1 );
    }

    @Test
    public void testTaxonByNcbiId() {
        ResponseDataObject<List<TaxonValueObject>> response = taxaWebService.taxa( TaxonArrayArg.valueOf( String.valueOf( taxon.getNcbiId() ) ), new MockHttpServletResponse() );
        assertThat( response.getData() ).hasSize( 1 );
    }

    @Test
    public void testTaxonByCommonName() {
        ResponseDataObject<List<TaxonValueObject>> response = taxaWebService.taxa( TaxonArrayArg.valueOf( taxon.getCommonName() ), new MockHttpServletResponse() );
        assertThat( response.getData() ).hasSize( 1 );
    }

    @Test
    @Ignore("Filtering by scientific name is not yet supported.")
    public void testTaxonByScientificName() {
        ResponseDataObject<List<TaxonValueObject>> response = taxaWebService.taxa( TaxonArrayArg.valueOf( taxon.getScientificName() ), new MockHttpServletResponse() );
        assertThat( response.getData() ).hasSize( 1 );
    }

    @Test
    public void testTaxonDatasetsByNcbiId() {
        PaginatedResponseDataObject<ExpressionExperimentValueObject> response = taxaWebService.taxonDatasets(
                TaxonArg.valueOf( taxon.getNcbiId().toString() ),
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "20" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response.getData() ).isEmpty();
    }
}
