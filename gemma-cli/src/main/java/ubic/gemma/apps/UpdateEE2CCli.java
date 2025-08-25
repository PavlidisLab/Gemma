package ubic.gemma.apps;

import lombok.Getter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.util.AbstractAuthenticatedCLI;
import ubic.gemma.cli.util.CLI;
import ubic.gemma.core.util.GemmaRestApiClient;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil;

import javax.annotation.Nullable;
import java.util.Date;

import static ubic.gemma.cli.util.OptionsUtils.addDateOption;
import static ubic.gemma.cli.util.OptionsUtils.addEnumOption;

public class UpdateEE2CCli extends AbstractAuthenticatedCLI {

    private static final String
            LEVEL_OPTION = "l",
            SINCE_OPTION = "s",
            TRUNCATE_OPTION = "truncate";

    @Getter
    private enum Level {
        EXPRESSION_EXPERIMENT( ExpressionExperiment.class ),
        BIO_MATERIAL( BioMaterial.class ),
        CELL_TYPE_ASSIGNMENT( CellTypeAssignment.class ),
        CELL_LEVEL_CHARACTERISTICS( CellLevelCharacteristics.class ),
        EXPERIMENTAL_DESIGN( ExperimentalDesign.class );

        private final Class<?> levelClass;

        Level( Class<?> levelClass ) {
            this.levelClass = levelClass;
        }
    }

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private GemmaRestApiClient gemmaRestApiClient;

    private Level level;
    private Date sinceLastUpdate;
    private boolean truncate;

    @Nullable
    @Override
    public String getCommandName() {
        return "updateEe2c";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Update the EXPRESSION_EXPERIMENT2CHARACTERISTIC table";
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CLI.CommandGroup.EXPERIMENT;
    }

    @Override
    protected void buildOptions( Options options ) {
        addEnumOption( options, LEVEL_OPTION, "level", "Only update characteristic at the given level.", Level.class );
        addDateOption( SINCE_OPTION, "since", "Only update characteristics from experiments updated since the given date", options );
        options.addOption( TRUNCATE_OPTION, "truncate", false, "Truncate the table before updating it" );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        level = commandLine.getParsedOptionValue( LEVEL_OPTION );
        if ( commandLine.hasOption( SINCE_OPTION ) ) {
            sinceLastUpdate = commandLine.getParsedOptionValue( SINCE_OPTION );
        } else {
            sinceLastUpdate = null;
        }
        truncate = commandLine.hasOption( TRUNCATE_OPTION );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        int updated;
        if ( level != null ) {
            updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( level.getLevelClass(), sinceLastUpdate, truncate );
        } else {
            updated = tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( sinceLastUpdate, truncate );
        }
        if ( updated > 0 ) {
            try {
                gemmaRestApiClient.perform( "/datasets/annotations/refresh" );
                log.info( "Refreshed all EE2C associations from " + gemmaRestApiClient.getHostUrl() );
            } catch ( Exception e ) {
                log.warn( "Failed to refresh EE2C from " + gemmaRestApiClient.getHostUrl(), e );
            }
        }
    }
}
