package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Value;
import ubic.gemma.cli.util.AbstractCLI;
import ubic.gemma.cli.util.ConsoleProgressReporterFactory;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneFetcher;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneUtils;
import ubic.gemma.core.loader.expression.cellxgene.model.CollectionMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetAsset;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetMetadata;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetVersion;
import ubic.gemma.core.util.SimpleRetryPolicy;

import org.springframework.lang.Nullable;
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
        options.addOption( "datasetId", "dataset-id", true, "CELLxGENE dataset, given as its permanent dataset ID, a dataset version ID or its title within the collection. Required if the collection holds more than one dataset." );
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
        if ( getCliContext().getConsole() != null ) {
            fetcher.setProgressReporterFactory( new ConsoleProgressReporterFactory( getCliContext().getConsole() ) );
        }
        CollectionMetadata cm = fetcher.fetchCollectionMetadata( collectionId );
        DatasetVersion version = fetcher.resolveDataset( collectionId, datasetId );
        DatasetMetadata dm = CellXGeneUtils.getDatasetMetadata( cm, version );
        log.info( String.format( "Resolved CELLxGENE dataset %s (version %s): %s", version.getDatasetId(),
                version.getDatasetVersionId(), version.getTitle() ) );
        DatasetAsset am;
        if ( assetId != null ) {
            am = dm.getDatasetAssets().stream().filter( a -> a.getId().equals( assetId ) )
                    .findFirst()
                    .orElseThrow( () -> new IllegalArgumentException( "Could not find asset " + assetId + " in dataset " + version.getDatasetId() + "." ) );
        } else {
            List<DatasetAsset> found = dm.getDatasetAssets().stream()
                    .filter( CellXGeneUtils::isAnnData )
                    .collect( Collectors.toList() );
            if ( found.isEmpty() ) {
                throw new IllegalArgumentException( "No AnnData asset found in dataset " + version.getDatasetId() + "." );
            } else if ( found.size() > 1 ) {
                throw new IllegalArgumentException( "Multiple AnnData assets found in dataset " + version.getDatasetId() + ". Please specify an assetId." );
            }
            am = found.iterator().next();
        }
        fetcher.downloadDatasetAsset( dm.getId(), am.getId(), requireNonNull( am.getFiletype() ) );
    }
}
