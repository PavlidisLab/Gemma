package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

/**
 * Housekeeping: store the GEO record each experiment was built from.
 * <p>
 * {@code Investigation.sourceMetadata} is written at import time only, so it exists for what has
 * been imported since that shipped and for nothing else — one experiment on production as of
 * 2026-08-28. This walks the corpus and fills it in.
 * <p>
 * 🛑 <b>It writes the document and nothing else.</b> {@code GeoUpdateConfig} gates every other
 * effect of a refetch — experiment tags, sample characteristics, publications — and this sets none
 * of them. That matters more here than in an ordinary refresh: GEO is not the authority on a
 * curated field, and a corpus-wide job that quietly took GEO's opinion on 23,000 datasets would be
 * very hard to undo.
 * <p>
 * One GEO fetch per experiment is the cost, so this is a slow job by construction. It is resumable:
 * an experiment that already has a document is skipped unless {@code --force} is given, so an
 * interrupted run continues where it stopped.
 */
public class UpdateGeoSourceMetadataCli extends ExpressionExperimentManipulatingCLI {

    private static final String FORCE_OPTION = "force";

    private boolean force;

    @Autowired
    private GeoService geoService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    public UpdateGeoSourceMetadataCli() {
        setDefaultToAll();
        // NOT setAllIsLazy(): that hands the whole corpus to processAllExpressionExperiments(),
        // whose default body is empty, so `-all` -- which is also the default when no options are
        // given -- reported success in 0 seconds having done nothing. Lazy-all is for the CLIs that
        // replace a per-experiment sweep with one set-based statement (UpdateEE2CCli, UpdateEe2AdCli);
        // this one has to visit every experiment because each needs its own GEO fetch.
        // References are enough to visit them: the task thaws before it reads anything.
        setUseReferencesIfPossible();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "updateGeoSourceMetadata";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Store the GEO record each experiment was imported from, for experiments that lack it.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( FORCE_OPTION, "force", false,
                "Refetch and replace the document even for experiments that already have one." );
        addThreadsOption( options );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        force = commandLine.hasOption( FORCE_OPTION );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        getBatchTaskExecutor().execute( () -> {
            try {
                // The batch task runs off the main thread, where there is no session and the
                // experiment is detached: reading ee.getAccession() directly threw
                // LazyInitializationException on the DatabaseEntry proxy for every experiment in
                // the run. Thawing first is the same opening move as
                // ExpressionExperimentDataUpdaterCli, and everything below reads the thawed one.
                ExpressionExperiment thawed = expressionExperimentService.thawLite( ee );
                if ( thawed.getAccession() == null
                        || !ExternalDatabases.GEO.equals( thawed.getAccession().getExternalDatabase().getName() ) ) {
                    // Not an error: plenty of experiments did not come from GEO, and there is no
                    // record for them to store. Counting them as failures would bury the real ones.
                    addSuccessObject( thawed, "Not a GEO experiment, nothing to store." );
                    return;
                }
                if ( !force && expressionExperimentService.hasSourceMetadata( thawed ) ) {
                    addSuccessObject( thawed, "Already has a source metadata document, skipped." );
                    return;
                }
                geoService.updateFromGEO( thawed, GeoService.GeoUpdateConfig.builder()
                        .sourceMetadata( true )
                        .build() );
                addSuccessObject( thawed, "Stored the GEO source metadata document." );
            } catch ( Exception e ) {
                // One experiment failing to fetch must not end a corpus sweep: GEO withdraws series,
                // renames them, and rate-limits, and the next 20,000 are unaffected by any of that.
                // formatExperiment(): ee may still be an uninitialized reference here -- if the
                // thaw is what failed, printing it directly would raise a second exception on the
                // way to reporting the first.
                addErrorObject( formatExperiment( ee ), "Failed to store GEO source metadata", e );
            }
        } );
    }
}
