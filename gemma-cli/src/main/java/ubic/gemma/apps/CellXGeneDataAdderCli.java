package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.ConsoleProgressReporterFactory;
import ubic.gemma.cli.util.EntityLocator;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneDataLoaderService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;

import static ubic.gemma.cli.util.EntityOptionsUtils.addGenericPlatformOption;

/**
 * @author poirigui
 */
public class CellXGeneDataAdderCli extends AbstractAuthenticatedCLI {

    @Autowired
    private CellXGeneDataLoaderService cellXGeneDataLoaderService;

    @Autowired
    private EntityLocator entityLocator;

    private String collectionId;
    @Nullable
    private String datasetId;
    @Nullable
    private String assetId;
    private String datasetShortName;
    private String platformIdentifier;
    private boolean skipData;
    private boolean keepPooledSample;
    private boolean keepUnknownSample;
    private boolean dryRun;

    @Override
    public String getCommandName() {
        return "addCELLxGENEData";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    public String getShortDesc() {
        return "Load a single-cell dataset from CELLxGENE.";
    }

    @Override
    protected void buildOptions( Options options ) {
        options.addRequiredOption( "collectionId", "collection-id", true, "CELLxGENE collection identifier." );
        options.addOption( "datasetId", "dataset-id", true, "CELLxGENE dataset identifier." );
        options.addOption( "assetId", "asset-id", true, "CELLxGENE asset identifier." );
        addGenericPlatformOption( options, "a", "array", "Target platform to use for the dataset. Note that Ensembl IDs will be used to match design elements, so the platform must have genes with Ensembl IDs." );
        options.addRequiredOption( "shortName", "short-name", true, "Short name to use for the resulting dataset." );
        options.addOption( "skipData", "skip-data", false, "Only load experiment metadata." );
        options.addOption( "keepPooledSample", "keep-pooled-sample", false, "Keep the pooled sample." );
        options.addOption( "keepUnknownSample", "keep-unknown-sample", false, "Keep the unknown sample." );
        options.addOption( "dryRun", "dry-run", false, "Don't upload anything to gemma" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        collectionId = commandLine.getOptionValue( "collectionId" );
        datasetId = commandLine.getOptionValue( "datasetId" );
        assetId = commandLine.getOptionValue( "assetId" );
        platformIdentifier = commandLine.getOptionValue( "a" );
        datasetShortName = commandLine.getOptionValue( "shortName" );
        skipData = commandLine.hasOption( "skipData" );
        keepPooledSample = commandLine.hasOption( "keepPooledSample" );
        keepUnknownSample = commandLine.hasOption( "keepUnknownSample" );
        dryRun = commandLine.hasOption( "dryRun" );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        if ( getCliContext().getConsole() != null ) {
            cellXGeneDataLoaderService.setProgressReporterFactory( new ConsoleProgressReporterFactory( getCliContext().getConsole() ) );
        }
        ArrayDesign platform = entityLocator.locateArrayDesign( platformIdentifier );
        ExpressionExperiment ee = cellXGeneDataLoaderService.fetchAndLoad( collectionId, datasetId, assetId, platform,
                datasetShortName, !skipData, keepPooledSample, keepUnknownSample, dryRun );
        addSuccessObject( ee.getShortName(), "Added" );
    }
}
