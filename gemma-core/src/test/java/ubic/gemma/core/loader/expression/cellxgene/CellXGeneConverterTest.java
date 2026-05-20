package ubic.gemma.core.loader.expression.cellxgene;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.entrez.pubmed.PubMedSearch;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoader;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellDataTransformationFactory;
import ubic.gemma.core.loader.expression.singleCell.transform.SingleCellTransformationConfig;
import ubic.gemma.core.loader.util.mapper.SimpleDesignElementMapper;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.test.BaseTest5;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonReadService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ubic.gemma.core.util.test.Assumptions.assumeThatFreeMemoryIsGreaterOrEqualTo;

@ContextConfiguration
@ExtendWith(NetworkAvailableExtension.class)
@NetworkAvailable(url = "https://api.cellxgene.cziscience.com")
public class CellXGeneConverterTest extends BaseTest5 {

    @Configuration
    @TestComponent
    @Import({ SettingsConfig.class, SingleCellTransformationConfig.class })
    static class Config {
    }

    @Autowired
    private SingleCellDataTransformationFactory singleCellDataTransformationFactory;

    @Value("${cellxgene.local.singleCellData.basepath}")
    private Path downloadDir;

    @Value("${entrez.efetch.apikey}")
    private String ncbiApiKey;

    private CellXGeneConverter cellxgeneConverter;
    private CellXGeneFetcher fetcher;
    private Taxon human;

    @BeforeEach
    public void setUp() throws IOException {
        ExternalDatabaseService eds = mock();
        ExternalDatabase cellxGeneDatabase = ExternalDatabase.Factory.newInstance( "CELLxGENE", DatabaseType.EXPRESSION );
        when( eds.findByName( "CELLxGENE" ) ).thenReturn( cellxGeneDatabase );
        TaxonReadService taxonReadService = mock();
        human = new Taxon();
        when( taxonReadService.findByScientificName( "Homo sapiens" ) ).thenReturn( human );
        cellxgeneConverter = new CellXGeneConverter( eds, taxonReadService, new PubMedSearch( ncbiApiKey ) );
        fetcher = new CellXGeneFetcher( new SimpleRetryPolicy( 3, 500, 1.5 ),
                downloadDir );
    }

    @Test
    public void test() throws IOException {
        ArrayDesign platform = new ArrayDesign();
        platform.setPrimaryTaxon( human );
        CollectionMetadata cm = fetcher.fetchCollectionMetadata( "f406a653-c079-4bf9-aab6-85846c27571d" );
        DatasetMetadata dm = fetcher.fetchDatasetMetadata( "412352dd-a919-4d8e-9f74-e210627328b5" );
        Path dataPath = fetcher.downloadDatasetAsset( "412352dd-a919-4d8e-9f74-e210627328b5", "e470eb54-b3ce-4b30-b5e1-24d388479353", FileType.H5AD );
        AnnDataSingleCellDataLoader dataLoader = new CellXGeneAnnDataSingleCellDataConfigurer( dataPath, singleCellDataTransformationFactory )
                .configureLoader( SingleCellDataLoaderConfig.builder()
                        // this will skip the transpose and sort by sample steps
                        .ignoreDataVectors( true )
                        .build() );
        ExpressionExperiment ee = cellxgeneConverter.convert( cm, dm, platform, "Clarence-2025", dataLoader, false );
        assertThat( ee.getAccession() ).isNotNull().satisfies( accession -> {
            assertThat( accession.getAccession() ).isEqualTo( "412352dd-a919-4d8e-9f74-e210627328b5" );
            assertThat( accession.getUri() ).isEqualTo( "https://cellxgene.cziscience.com/collections/f406a653-c079-4bf9-aab6-85846c27571d" );
            assertThat( accession.getExternalDatabase().getName() ).isEqualTo( "CELLxGENE" );
        } );
        assertThat( ee.getPrimaryPublication() ).isNotNull()
                .satisfies( p -> {
                    assertThat( p.getTitle() ).isEqualTo( "Multiomic single-cell profiling identifies critical regulators of postnatal brain." );
                    assertThat( p.getPubAccession().getAccession() ).isEqualTo( "39962241" );
                } );
        assertThat( ee.getOtherRelevantPublications() ).isEmpty();
        assertThat( ee.getCharacteristics() )
                .extracting( Characteristic::getCategory )
                .contains( "assay", "development stage", "cell type", "disease", "organism part" );
        assertThat( ee.getCharacteristics() )
                .extracting( Characteristic::getValueUri )
                .containsExactlyInAnyOrder(
                        "http://purl.obolibrary.org/obo/CL_0000127",
                        "http://purl.obolibrary.org/obo/CL_0000128",
                        "http://purl.obolibrary.org/obo/CL_4023051",
                        "http://purl.obolibrary.org/obo/UBERON_0001873",
                        "http://purl.obolibrary.org/obo/CL_4033054",
                        "http://purl.obolibrary.org/obo/PATO_0000461",
                        "http://purl.obolibrary.org/obo/CL_0000129",
                        "http://purl.obolibrary.org/obo/CL_0000669",
                        "http://purl.obolibrary.org/obo/HsapDv_0000261",
                        "http://purl.obolibrary.org/obo/UBERON_0009835",
                        "http://purl.obolibrary.org/obo/UBERON_0009834",
                        "http://purl.obolibrary.org/obo/UBERON_0002421",
                        "http://purl.obolibrary.org/obo/HsapDv_0000100",
                        "http://purl.obolibrary.org/obo/HsapDv_0000108",
                        "http://purl.obolibrary.org/obo/CL_0000115",
                        "http://purl.obolibrary.org/obo/CL_0002453",
                        "http://purl.obolibrary.org/obo/CL_0000617",
                        "http://www.ebi.ac.uk/efo/EFO_0030059",
                        "http://purl.obolibrary.org/obo/CL_0000679",
                        "http://purl.obolibrary.org/obo/HsapDv_0000098",
                        "http://purl.obolibrary.org/obo/HsapDv_0000156",
                        "http://purl.obolibrary.org/obo/HsapDv_0000133",
                        "http://purl.obolibrary.org/obo/HsapDv_0000155",
                        "http://purl.obolibrary.org/obo/HsapDv_0000114",
                        "http://purl.obolibrary.org/obo/CL_0000065"
                );
        assertThat( ee.getBioAssays() )
                .hasSize( 10 );
        assertThat( ee.getBioAssays() )
                .allSatisfy( ba -> {
                    assertThat( ba.getSampleUsed().getCharacteristics() )
                            .hasSize( 7 )
                            .extracting( Characteristic::getCategory )
                            .containsExactlyInAnyOrder(
                                    "sex",
                                    "assay",
                                    "suspension_type",
                                    "development_stage",
                                    "tissue_type",
                                    "disease",
                                    "is_primary_data" );
                } );
    }

    @Test
    public void testLoadData() throws IOException {
        assumeThatFreeMemoryIsGreaterOrEqualTo( 8 * 1024 * 1024 * 1024L, false );
        ArrayDesign platform = new ArrayDesign();
        platform.setPrimaryTaxon( human );
        Collection<CompositeSequence> designElements = Arrays.asList(
                CompositeSequence.Factory.newInstance( "ENSG00000215203", platform ),
                CompositeSequence.Factory.newInstance( "ENSG00000215206", platform ) );
        platform.getCompositeSequences().addAll( designElements );
        CollectionMetadata cm = fetcher.fetchCollectionMetadata( "f406a653-c079-4bf9-aab6-85846c27571d" );
        DatasetMetadata dm = fetcher.fetchDatasetMetadata( "412352dd-a919-4d8e-9f74-e210627328b5" );
        Path dataPath = fetcher.downloadDatasetAsset( "412352dd-a919-4d8e-9f74-e210627328b5", "e470eb54-b3ce-4b30-b5e1-24d388479353", FileType.H5AD );
        AnnDataSingleCellDataLoader dataLoader = new CellXGeneAnnDataSingleCellDataConfigurer( dataPath, singleCellDataTransformationFactory )
                .configureLoader( SingleCellDataLoaderConfig.builder().build() );
        dataLoader.setDesignElementToGeneMapper( new SimpleDesignElementMapper( designElements ) );
        ExpressionExperiment ee = cellxgeneConverter.convert( cm, dm, platform, "Clarence-2025", dataLoader, true );
        assertThat( ee.getQuantitationTypes() ).hasSize( 1 );
        assertThat( ee.getSingleCellExpressionDataVectors() ).hasSize( 2 );
    }
}