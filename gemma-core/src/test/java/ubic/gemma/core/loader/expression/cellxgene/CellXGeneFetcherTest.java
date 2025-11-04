package ubic.gemma.core.loader.expression.cellxgene;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.config.SettingsConfig;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAssetDownloadMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
public class CellXGeneFetcherTest extends BaseTest {

    @Import(SettingsConfig.class)
    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public CellXGeneFetcher cellXGeneFetcher( @Value("${gemma.download.path}") Path downloadDir ) {
            return new CellXGeneFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ),
                    downloadDir.resolve( "singleCellData/CELLxGENE" ) );
        }
    }

    @Autowired
    private CellXGeneFetcher fetcher;

    @Test
    public void testFetchAllCollectionMetadata() throws IOException {
        List<CollectionMetadata> metadata = fetcher.fetchAllCollectionMetadata();
        assertThat( metadata ).isNotEmpty();
        for ( CollectionMetadata cm : metadata ) {
            System.out.println( cm );
        }
    }

    @Test
    public void testFetchCollectionMetadata() throws IOException {
        CollectionMetadata metadata = fetcher.fetchCollectionMetadata( "31937775-0602-4e52-a799-b6acdd2bac2e" );
        assertThat( metadata.getId() ).isEqualTo( "31937775-0602-4e52-a799-b6acdd2bac2e" );
    }

    @Test
    @Category(SlowTest.class)
    public void testFetchAllDatasetMetadata() throws IOException {
        List<DatasetMetadata> metadata = fetcher.fetchAllDatasetMetadata();
        assertThat( metadata ).isNotEmpty();
        for ( DatasetMetadata dm : metadata ) {
            if ( dm.getDatasetAssets().stream().anyMatch( CellXGeneUtils::isAnnData ) ) {
                System.out.println( dm.getId() );
            }
        }
    }

    @Test
    @Category(SlowTest.class)
    public void testFetchDatasetMetadata() throws IOException {
        DatasetAssetDownloadMetadata metadata = fetcher.fetchDatasetAssetDownloadMetadata( "860a9839-5d24-4073-9a67-6ad570f41da1", "ee04d5be-523b-4bde-af01-f892778e01d8" );
        assertThat( metadata.getDatasetId() ).isEqualTo( "860a9839-5d24-4073-9a67-6ad570f41da1" );
        assertThat( metadata.getUrl() ).isNotNull();
        assertThat( metadata.getFileSize() ).isGreaterThan( 1000 );
    }

    @Test
    public void testDownloadDatasetAsset() throws IOException {
        fetcher.downloadDatasetAsset( "03390dd0-fe16-4cef-b430-ab451e85c448", "4d947311-c9e6-45c7-9f3c-00176074912b", FileType.H5AD );
    }
}