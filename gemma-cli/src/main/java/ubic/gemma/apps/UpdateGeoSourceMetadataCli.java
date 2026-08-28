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
        setAllIsLazy();
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
                if ( ee.getAccession() == null
                        || !ExternalDatabases.GEO.equals( ee.getAccession().getExternalDatabase().getName() ) ) {
                    // Not an error: plenty of experiments did not come from GEO, and there is no
                    // record for them to store. Counting them as failures would bury the real ones.
                    addSuccessObject( ee, "Not a GEO experiment, nothing to store." );
                    return;
                }
                if ( !force && expressionExperimentService.hasSourceMetadata( ee ) ) {
                    addSuccessObject( ee, "Already has a source metadata document, skipped." );
                    return;
                }
                geoService.updateFromGEO( ee, GeoService.GeoUpdateConfig.builder()
                        .sourceMetadata( true )
                        .build() );
                addSuccessObject( ee, "Stored the GEO source metadata document." );
            } catch ( Exception e ) {
                // One experiment failing to fetch must not end a corpus sweep: GEO withdraws series,
                // renames them, and rate-limits, and the next 20,000 are unaffected by any of that.
                addErrorObject( ee, "Failed to store GEO source metadata", e );
            }
        } );
    }
}
