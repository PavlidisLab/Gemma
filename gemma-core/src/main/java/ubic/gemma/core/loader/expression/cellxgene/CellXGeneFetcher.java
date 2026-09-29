package ubic.gemma.core.loader.expression.cellxgene;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAssetDownloadMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetVersion;
import ubic.gemma.core.loader.util.fetcher2.AbstractFetcher;
import ubic.gemma.core.loader.util.hdf5.H5File;
import ubic.gemma.core.loader.util.hdf5.TruncatedH5FileException;
import ubic.gemma.core.util.SimpleDownloader;
import ubic.gemma.core.util.SimpleRetryPolicy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fetch data from CELLxGENE.
 *
 * @author poirigui
 */
public class CellXGeneFetcher extends AbstractFetcher {

    private final ObjectMapper objectMapper;
    private final Path downloadPath;

    public CellXGeneFetcher( SimpleRetryPolicy retryPolicy, Path downloadPath ) {
        super( new SimpleDownloader( retryPolicy ) );
        this.objectMapper = new ObjectMapper()
                .configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false )
                .configure( DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true )
                .setPropertyNamingStrategy( PropertyNamingStrategies.SNAKE_CASE );
        this.downloadPath = downloadPath;
    }

    /**
     * Fetch the metadata for all CELLxGENE collections.
     * <p>
     * The collection metadata is shallow and does not include {@link CollectionMetadata#getDatasets()} or
     * {@link CollectionMetadata#getLinks()}. Use {@link #fetchCollectionMetadata(String)} to get the full metadata.
     */
    public List<CollectionMetadata> fetchAllCollectionMetadata() throws IOException {
        return objectMapper.readValue( new URL( "https://api.cellxgene.cziscience.com/dp/v1/collections/index" ),
                objectMapper.getTypeFactory().constructCollectionLikeType( List.class, CollectionMetadata.class ) );
    }

    /**
     * Fetch the full metadata for a specific CELLxGENE collection.
     */
    public CollectionMetadata fetchCollectionMetadata( String collectionId ) throws IOException {
        return objectMapper.readValue( new URL( "https://api.cellxgene.cziscience.com/dp/v1/collections/" + collectionId ), CollectionMetadata.class );
    }

    /**
     * Fetch the permanent and version identifiers of the datasets in a collection.
     * <p>
     * This uses the curation API because the data portal API used by {@link #fetchCollectionMetadata(String)} only
     * exposes dataset version IDs.
     */
    public List<DatasetVersion> fetchDatasetVersions( String collectionId ) throws IOException {
        JsonNode collection = objectMapper.readTree( new URL( "https://api.cellxgene.cziscience.com/curation/v1/collections/" + collectionId ) );
        return objectMapper.convertValue( collection.path( "datasets" ),
                objectMapper.getTypeFactory().constructCollectionLikeType( List.class, DatasetVersion.class ) );
    }

    /**
     * Fetch the permanent and version identifiers of all public CELLxGENE datasets, in a single request.
     */
    public List<DatasetVersion> fetchAllDatasetVersions() throws IOException {
        return objectMapper.readValue( new URL( "https://api.cellxgene.cziscience.com/curation/v1/datasets" ),
                objectMapper.getTypeFactory().constructCollectionLikeType( List.class, DatasetVersion.class ) );
    }

    /**
     * Resolve a dataset in a collection from its permanent ID, its version ID or its title.
     * <p>
     * A superseded version ID resolves to the current version of its dataset, since only the current version is
     * described by {@link #fetchCollectionMetadata(String)}.
     *
     * @param identifier a dataset identifier, or {@code null} to select the only dataset of the collection
     * @throws IllegalArgumentException if the identifier does not resolve to exactly one dataset, or if it is
     *                                  {@code null} and the collection does not hold exactly one dataset
     * @see CellXGeneUtils#resolveDataset(String, List, String)
     */
    public DatasetVersion resolveDataset( String collectionId, @Nullable String identifier ) throws IOException {
        List<DatasetVersion> datasets = fetchDatasetVersions( collectionId );
        if ( identifier == null ) {
            if ( datasets.size() == 1 ) {
                return datasets.get( 0 );
            }
            throw new IllegalArgumentException( String.format( "CELLxGENE collection %s has %d datasets, specify one by ID or title.",
                    collectionId, datasets.size() ) );
        }
        try {
            return CellXGeneUtils.resolveDataset( collectionId, datasets, identifier );
        } catch ( IllegalArgumentException e ) {
            // the identifier might be a version that has since been superseded, as stored by older Gemma loads
            DatasetVersion current = findDatasetBySupersededVersion( datasets, identifier );
            if ( current != null ) {
                // the data portal API only describes the current version, so that is the one we can load
                log.warn( String.format( "%s is a superseded version of CELLxGENE dataset %s (%s), using its current version %s instead.",
                        identifier, current.getDatasetId(), current.getTitle(), current.getDatasetVersionId() ) );
                return current;
            }
            throw e;
        }
    }

    /**
     * Fetch all the published version IDs of a dataset, most recent first.
     */
    public List<String> fetchDatasetVersionIds( String datasetId ) throws IOException {
        List<DatasetVersion> versions = objectMapper.readValue( new URL( "https://api.cellxgene.cziscience.com/curation/v1/datasets/" + datasetId + "/versions" ),
                objectMapper.getTypeFactory().constructCollectionLikeType( List.class, DatasetVersion.class ) );
        return versions.stream().map( DatasetVersion::getDatasetVersionId ).collect( Collectors.toList() );
    }

    @Nullable
    private DatasetVersion findDatasetBySupersededVersion( List<DatasetVersion> datasets, String versionId ) throws IOException {
        // only reached when an identifier did not resolve, so the one request per dataset is paid on the error path
        for ( DatasetVersion dv : datasets ) {
            if ( fetchDatasetVersionIds( dv.getDatasetId() ).contains( versionId ) ) {
                return dv;
            }
        }
        return null;
    }

    private List<DatasetMetadata> cachedDatasetMetadata = null;
    private Map<String, DatasetMetadata> cachedDatasetMetadataByDatasetId = null;
    private long lastDatasetMetadataContentLength = -1;

    /**
     * Fetch the metadata for all CELLxGENE datasets.
     */
    public List<DatasetMetadata> fetchAllDatasetMetadata() throws IOException {
        URLConnection connection = new URL( "https://api.cellxgene.cziscience.com/dp/v1/datasets/index" ).openConnection();
        try {
            if ( cachedDatasetMetadata == null || connection.getContentLengthLong() == -1 || connection.getContentLengthLong() != lastDatasetMetadataContentLength ) {
                log.warn( "Fetching metadata for all CELLxGENE datasets, this might take a moment..." );
                cachedDatasetMetadata = objectMapper.readValue( connection.getInputStream(),
                        objectMapper.getTypeFactory().constructCollectionLikeType( List.class, DatasetMetadata.class ) );
                cachedDatasetMetadataByDatasetId = cachedDatasetMetadata.stream()
                        .collect( Collectors.toMap( DatasetMetadata::getId, Function.identity() ) );
                lastDatasetMetadataContentLength = connection.getContentLengthLong();
            }
        } finally {
            if ( connection instanceof HttpURLConnection ) {
                ( ( HttpURLConnection ) connection ).disconnect();
            }
        }
        return cachedDatasetMetadata;
    }

    /**
     * Retrieves the metadata for a specific CELLxGENE dataset.
     * <p>
     * There is no dedicated endpoint for fetching a single dataset's metadata, so this method relies on cache the
     * output of {@link #fetchAllDatasetMetadata()}.
     */
    public DatasetMetadata fetchDatasetMetadata( String datasetId ) throws IOException {
        fetchAllDatasetMetadata();
        if ( cachedDatasetMetadataByDatasetId.containsKey( datasetId ) ) {
            return cachedDatasetMetadataByDatasetId.get( datasetId );
        } else {
            throw new FileNotFoundException( "No dataset with ID " + datasetId + " found in CELLxGENE." );
        }
    }

    /**
     * Fetch the download metadata for a specific dataset asset.
     */
    public DatasetAssetDownloadMetadata fetchDatasetAssetDownloadMetadata( String datasetId, String assetId ) throws IOException {
        return objectMapper.readValue( new URL( "https://api.cellxgene.cziscience.com/dp/v1/datasets/" + datasetId + "/asset/" + assetId ),
                DatasetAssetDownloadMetadata.class );
    }

    public Path downloadDatasetAsset( String datasetId, String assetId, FileType fileType ) throws IOException {
        Assert.isTrue( fileType == FileType.H5AD, "Only H5AD file type is supported currently." );
        DatasetAssetDownloadMetadata meta = fetchDatasetAssetDownloadMetadata( datasetId, assetId );
        Path dest = downloadPath.resolve( datasetId + ".h5ad" );
        simpleDownloader.download( new URL( meta.getUrl() ),
                dest, false );
        try ( H5File ignored = H5File.open( dest ) ) {
            // TODO: do some checks?
        } catch ( TruncatedH5FileException e ) {
            Files.delete( dest );
            throw e;
        }
        return dest;
    }
}
