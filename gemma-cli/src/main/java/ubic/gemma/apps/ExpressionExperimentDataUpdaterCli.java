package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Update GEO data for an experiment.
 * @author poirigui
 */
public class ExpressionExperimentDataUpdaterCli extends ExpressionExperimentManipulatingCLI {

    @Autowired
    private GeoService geoService;

    private boolean updateExperimentTags;
    private boolean updateSampleCharacteristics;
    private boolean updatePublications;

    public ExpressionExperimentDataUpdaterCli() {
        setUseReferencesIfPossible();
    }

    @Override
    public String getCommandName() {
        return "updateGEOData";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        super.buildExperimentOptions( options );
        options.addOption( "updateExperimentTags", "update-experiment-tags", false, "Update experiment tags" );
        options.addOption( "updateSampleCharacteristics", "update-sample-characteristics", false, "Update sample characteristics" );
        options.addOption( "updatePublications", "update-publications", false, "Update publications" );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        updateExperimentTags = commandLine.hasOption( "updateExperimentTags" );
        updateSampleCharacteristics = commandLine.hasOption( "updateSampleCharacteristics" );
        updatePublications = commandLine.hasOption( "updatePublications" );
        if ( !updateExperimentTags && !updateSampleCharacteristics && !updatePublications ) {
            updateExperimentTags = true;
            updateSampleCharacteristics = true;
            updatePublications = true;
        }
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment expressionExperiment ) throws Exception {
        getBatchTaskExecutor().execute( () -> processExpressionExperimentInternal( expressionExperiment ) );
    }

    private void processExpressionExperimentInternal( ExpressionExperiment expressionExperiment ) {
        GeoService.GeoUpdateConfig updateConfig = GeoService.GeoUpdateConfig.builder()
                .experimentTags( updateExperimentTags )
                .sampleCharacteristics( updateSampleCharacteristics )
                .publications( updatePublications )
                .build();
        expressionExperiment = eeService.thawLite( expressionExperiment );
        if ( expressionExperiment.getAccession() == null
                || !expressionExperiment.getAccession().getExternalDatabase().getName().equals( ExternalDatabases.GEO ) ) {
            addWarningObject( expressionExperiment, "Ignoring because it is not from GEO." );
            return;
        }
        geoService.updateFromGEO( expressionExperiment, updateConfig );
        addSuccessObject( expressionExperiment, String.format( "Updated %s from GEO.",
                StringUtils.removeEnd( ( updateExperimentTags ? "experiment tags, " : "" )
                        + ( updateSampleCharacteristics ? "sample characteristics, " : "" )
                        + ( updatePublications ? "publications" : "" ), ", " ) ) );
        try {
            refreshExpressionExperimentFromGemmaWeb( expressionExperiment, false, false );
        } catch ( Exception e ) {
            addWarningObject( expressionExperiment, "Could not refresh experiment from from Gemma Web.", e );
        }
    }
}
