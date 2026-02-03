package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneFetcher;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneUtils;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAsset;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.util.SimpleRetryPolicy;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * @author poirigui
 */
public class CellXGeneDataDownloaderCli extends AbstractCLI {

    @Value("${cellxgene.local.singleCellData.basepath}")
    private Path cellByGeneDownloadDir;

    private String collectionId;
    @Nullable
    private String datasetId;
    @Nullable
    private String assetId;

    @Override
    public String getCommandName() {
        return "downloadCELLxGENEData";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addRequiredOption( "collectionId", "collection-id", true, "CELLxGENE collection identifier." );
        options.addOption( "datasetId", "dataset-id", true, "CELLxGENE dataset identifier." );
        options.addOption( "assetId", "asset-id", true, "CELLxGENE asset identifier." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        collectionId = commandLine.getOptionValue( "collectionId" );
        datasetId = commandLine.getOptionValue( "datasetId" );
        assetId = commandLine.getOptionValue( "assetId" );
    }

    @Override
    protected void doWork() throws Exception {
        CellXGeneFetcher fetcher = new CellXGeneFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ), cellByGeneDownloadDir );
        CollectionMetadata cm = fetcher.fetchCollectionMetadata( collectionId );
        assert cm.getDatasets() != null;
        DatasetMetadata dm;
        if ( datasetId != null ) {
            dm = cm.getDatasets().stream().filter( d -> d.getId().equals( datasetId ) )
                    .findFirst()
                    .orElseThrow( () -> new IllegalArgumentException( "Could not find dataset " + datasetId + " in collection." ) );
        } else {
            if ( cm.getDatasets().isEmpty() ) {
                throw new IllegalArgumentException( "No dataset found in collection " + collectionId + "." );
            } else if ( cm.getDatasets().size() > 1 ) {
                throw new IllegalArgumentException( "Multiple datasets found in the collection " + collectionId + ". Please specify a dataset ID." );
            }
            dm = cm.getDatasets().get( 0 );
        }
        DatasetAsset am;
        if ( assetId != null ) {
            am = dm.getDatasetAssets().stream().filter( a -> a.getId().equals( assetId ) )
                    .findFirst()
                    .orElseThrow( () -> new IllegalArgumentException( "Could not find asset " + assetId + " in dataset " + datasetId + "." ) );
        } else {
            List<DatasetAsset> found = dm.getDatasetAssets().stream()
                    .filter( CellXGeneUtils::isAnnData )
                    .collect( Collectors.toList() );
            if ( found.isEmpty() ) {
                throw new IllegalArgumentException( "No AnnData asset found in dataset " + dm.getId() + "." );
            } else if ( found.size() > 1 ) {
                throw new IllegalArgumentException( "Multiple AnnData assets found in dataset " + dm.getId() + ". Please specify an assetId." );
            }
            am = found.iterator().next();
        }
        fetcher.downloadDatasetAsset( dm.getId(), am.getId(), requireNonNull( am.getFiletype() ) );
    }
}
