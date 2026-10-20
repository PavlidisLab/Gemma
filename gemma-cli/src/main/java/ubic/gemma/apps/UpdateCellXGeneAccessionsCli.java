package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneDataLoaderService;
import ubic.gemma.core.loader.expression.cellxgene.CellXGeneFetcher;
import ubic.gemma.core.loader.expression.cellxgene.model.DatasetVersion;
import ubic.gemma.core.util.SimpleRetryPolicy;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Housekeeping: replace CELLxGENE accessions that hold a dataset version ID by the permanent dataset ID.
 * <p>
 * Until permanent IDs were recorded, {@code addCELLxGENEData} stored the dataset version ID as the accession, and that
 * ID changes every time CELLxGENE revises the dataset. This resolves each stored version, current or superseded, to
 * its dataset through the CELLxGENE curation API and replaces the accession with one holding the permanent ID, with
 * the stored version kept as its accession version.
 * <p>
 * 🛑 Nothing is written unless {@code -apply} is given. Without it, every experiment is resolved and reported, so the
 * batch output can be reviewed before the real run.
 * <p>
 * The collection is read from the accession URI, which the loader set to the collection page. Experiments are visited
 * one at a time and each collection is fetched once, so the load on the CELLxGENE API is one request per collection
 * plus, for datasets revised since they were loaded, one request per dataset of that collection. Experiments whose
 * accession already has a version are skipped, so an interrupted run can simply be restarted.
 */
public class UpdateCellXGeneAccessionsCli extends ExpressionExperimentManipulatingCLI {

    private static final String APPLY_OPTION = "apply";
    private static final String COLLECTION_URI_PREFIX = "https://cellxgene.cziscience.com/collections/";

    @Autowired
    private CellXGeneDataLoaderService cellXGeneDataLoaderService;

    @Value("${cellxgene.local.singleCellData.basepath}")
    private Path cellXGeneDownloadPath;

    private boolean apply;

    private CellXGeneFetcher fetcher;
    private final Map<String, List<DatasetVersion>> datasetsByCollectionId = new HashMap<>();

    public UpdateCellXGeneAccessionsCli() {
        setDefaultToAll();
        setUseReferencesIfPossible();
        // troubled experiments have accessions too
        setForce();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "updateCELLxGENEAccessions";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Replace CELLxGENE accessions holding a dataset version ID by the permanent dataset ID.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( APPLY_OPTION, "apply", false,
                "Write the new accessions. Without this option, accessions are only resolved and reported." );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        apply = commandLine.hasOption( APPLY_OPTION );
    }

    @Override
    protected Collection<ExpressionExperiment> preprocessExpressionExperiments( Collection<ExpressionExperiment> expressionExperiments ) {
        Set<Long> cellXGeneIds = new HashSet<>( eeService.loadIds( Filters.by(
                eeService.getFilter( "accession.externalDatabase.name", Filter.Operator.eq, ExternalDatabases.CELLXGENE ) ), null ) );
        List<ExpressionExperiment> selected = expressionExperiments.stream()
                .filter( ee -> cellXGeneIds.contains( ee.getId() ) )
                .collect( Collectors.toList() );
        log.info( String.format( "%d of %d selected experiments have a CELLxGENE accession.", selected.size(), expressionExperiments.size() ) );
        if ( !apply ) {
            log.warn( "Dry run: nothing will be written; pass -apply to replace the accessions." );
        }
        fetcher = new CellXGeneFetcher( new SimpleRetryPolicy( 3, 1000, 1.5 ), cellXGeneDownloadPath );
        return selected;
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) throws IOException {
        ExpressionExperiment thawed = eeService.thawLite( ee );
        DatabaseEntry accession = thawed.getAccession();
        if ( accession == null || !ExternalDatabases.CELLXGENE.equals( accession.getExternalDatabase().getName() ) ) {
            addWarningObject( thawed, "Does not have a CELLxGENE accession, skipped." );
            return;
        }
        if ( accession.getAccessionVersion() != null ) {
            addWarningObject( thawed, String.format( "Already has the permanent ID %s (version %s), skipped.",
                    accession.getAccession(), accession.getAccessionVersion() ) );
            return;
        }
        String uri = accession.getUri();
        if ( uri == null || !uri.startsWith( COLLECTION_URI_PREFIX ) ) {
            addErrorObject( thawed, "Cannot determine the CELLxGENE collection of " + accession.getAccession() + " from its URI: " + uri + "." );
            return;
        }
        String collectionId = uri.substring( COLLECTION_URI_PREFIX.length() );
        List<DatasetVersion> datasets = datasetsByCollectionId.get( collectionId );
        if ( datasets == null ) {
            datasets = fetcher.fetchDatasetVersions( collectionId );
            datasetsByCollectionId.put( collectionId, datasets );
        }
        DatasetVersion dataset = fetcher.findDatasetByVersion( datasets, accession.getAccession() );
        if ( dataset == null ) {
            addErrorObject( thawed, String.format( "No dataset of CELLxGENE collection %s has version %s, left unchanged.",
                    collectionId, accession.getAccession() ) );
            return;
        }
        String resolution = String.format( "%s -> permanent ID %s, version %s (%s; %s)",
                accession.getAccession(), dataset.getDatasetId(), accession.getAccession(), dataset.getTitle(),
                dataset.getDatasetVersionId().equals( accession.getAccession() ) ? "current version" : "superseded, current is " + dataset.getDatasetVersionId() );
        if ( apply ) {
            cellXGeneDataLoaderService.replaceVersionAccession( thawed, dataset.getDatasetId() );
            addSuccessObject( thawed, "Replaced: " + resolution );
        } else {
            addSuccessObject( thawed, "Would replace: " + resolution );
        }
    }
}
