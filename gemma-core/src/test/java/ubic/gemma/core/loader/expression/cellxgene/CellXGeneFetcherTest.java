package ubic.gemma.core.loader.expression.cellxgene;

import org.junit.Rule;
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
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAsset;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAssetDownloadMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableRule;
import ubic.gemma.core.util.test.category.SlowTest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration
@NetworkAvailable(url = "https://api.cellxgene.cziscience.com")
public class CellXGeneFetcherTest extends BaseTest {

    @Rule
    public final NetworkAvailableRule networkAvailableRule = new NetworkAvailableRule();

    @Import(SettingsConfig.class)
    @Configuration
    @TestComponent
    static class CC {

        @Bean
        public CellXGeneFetcher cellXGeneFetcher( @Value("${cellxgene.local.singleCellData.basepath}") Path downloadDir ) {
            return new CellXGeneFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ),
                    downloadDir );
        }
    }

    @Autowired
    private CellXGeneFetcher fetcher;

    @Test
    public void testFetchAllCollectionMetadata() throws IOException {
        List<CollectionMetadata> metadata = fetcher.fetchAllCollectionMetadata();
        assertThat( metadata ).isNotEmpty();
    }

    @Test
    @Category(SlowTest.class)
    public void testFetchCollectionMetadata() throws IOException {
        String collectionId = pickDataset().getCollectionId();
        CollectionMetadata metadata = fetcher.fetchCollectionMetadata( collectionId );
        assertThat( metadata.getId() ).isEqualTo( collectionId );
    }

    @Test
    @Category(SlowTest.class)
    public void testFetchAllDatasetMetadata() throws IOException {
        List<DatasetMetadata> metadata = fetcher.fetchAllDatasetMetadata();
        assertThat( metadata ).isNotEmpty();
    }

    /**
     * Pick a dataset from the index to exercise the fetcher against.
     * <p>
     * CELLxGENE re-versions and removes datasets, rotating their IDs, so hard-coding a dataset ID makes these tests
     * rot. Instead, select one dynamically: the earliest-published dataset that has an AnnData (H5AD) asset (so it can
     * be downloaded), falling back to the first dataset in the index if no publication date is available.
     */
    private DatasetMetadata pickDataset() throws IOException {
        List<DatasetMetadata> all = fetcher.fetchAllDatasetMetadata();
        assertThat( all ).isNotEmpty();
        return all.stream()
                .filter( d -> d.getDatasetAssets() != null && d.getDatasetAssets().stream().anyMatch( CellXGeneUtils::isAnnData ) )
                .min( Comparator.comparing( DatasetMetadata::getPublishedAt, Comparator.nullsLast( Comparator.naturalOrder() ) ) )
                .orElse( all.get( 0 ) );
    }

    private DatasetAsset annDataAsset( DatasetMetadata dataset ) {
        return dataset.getDatasetAssets().stream()
                .filter( CellXGeneUtils::isAnnData )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "Expected an AnnData asset for " + dataset.getId() + "." ) );
    }

    @Test
    @Category(SlowTest.class)
    public void testFetchDatasetMetadata() throws IOException {
        String datasetId = pickDataset().getId();

        DatasetMetadata datasetMetadata = fetcher.fetchDatasetMetadata( datasetId );
        assertThat( datasetMetadata.getId() ).isEqualTo( datasetId );

        // try re-fetching, should hit the cache
        datasetMetadata = fetcher.fetchDatasetMetadata( datasetId );
        assertThat( datasetMetadata.getId() ).isEqualTo( datasetId );

        DatasetAsset asset = annDataAsset( datasetMetadata );
        DatasetAssetDownloadMetadata metadata = fetcher.fetchDatasetAssetDownloadMetadata( datasetId, asset.getId() );
        assertThat( metadata.getDatasetId() ).isEqualTo( datasetId );
        assertThat( metadata.getUrl() ).isNotNull();
        assertThat( metadata.getFileSize() ).isGreaterThan( 1000 );
    }

    @Test
    @Category(SlowTest.class)
    public void testDownloadDatasetAsset() throws IOException {
        DatasetMetadata dataset = pickDataset();
        fetcher.downloadDatasetAsset( dataset.getId(), annDataAsset( dataset ).getId(), FileType.H5AD );
    }
}