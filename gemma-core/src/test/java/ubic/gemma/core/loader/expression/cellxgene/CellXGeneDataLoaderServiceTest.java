package ubic.gemma.core.loader.expression.cellxgene;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableRule;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@NetworkAvailable(url = "https://api.cellxgene.cziscience.com")
public class CellXGeneDataLoaderServiceTest extends BaseIntegrationTest {

    @Rule
    public final NetworkAvailableRule networkAvailableRule = new NetworkAvailableRule();

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private CellXGeneDataLoaderService cellXGeneDataLoaderService;

    @Test
    public void test() throws IOException {
        Taxon taxon = taxonService.findByCommonName( "human" );
        ArrayDesign platform = new ArrayDesign();
        platform.setShortName( "test123123123" );
        platform.setName( "test" );
        platform.setPrimaryTaxon( taxon );
        platform = arrayDesignService.create( platform );

        ExpressionExperiment ee = cellXGeneDataLoaderService.fetchAndLoad( "f406a653-c079-4bf9-aab6-85846c27571d",
                "412352dd-a919-4d8e-9f74-e210627328b5", null, platform, "Clarence-2024",
                false, false, false );

        assertThat( ee.getAccession() ).isNotNull()
                .satisfies( accession -> {
                    assertThat( accession.getAccession() )
                            .isEqualTo( "412352dd-a919-4d8e-9f74-e210627328b5" );
                    assertThat( accession.getUri() )
                            .isEqualTo( "https://cellxgene.cziscience.com/collections/f406a653-c079-4bf9-aab6-85846c27571d" );
                    assertThat( accession.getExternalDatabase().getName() )
                            .isEqualTo( "CELLxGENE" );
                } );
    }
}