package ubic.gemma.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.FactorValueDeletion;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Delete factor values from a dataset's experimental design, either by ID or by factor and displayed value.
 * <p>
 * Every differential expression analysis that uses the factor is deleted with them, as when factor values are deleted
 * from the web interface. Nothing is changed unless {@code -commit} is given: without it the command only reports what
 * would be deleted.
 */
public class FactorValueDeleterCli extends ExpressionExperimentManipulatingCLI {

    private static final String
            FACTOR_VALUES_OPTION = "fv",
            FACTOR_OPTION = "factor",
            VALUES_OPTION = "values",
            COMMIT_OPTION = "commit";

    @Autowired
    private FactorValueDeletion factorValueDeletion;

    @Nullable
    private List<Long> factorValueIds;
    @Nullable
    private String factor;
    @Nullable
    private List<String> values;
    private boolean commit;

    public FactorValueDeleterCli() {
        setSingleExperimentMode();
    }

    @Override
    public String getCommandName() {
        return "deleteFactorValues";
    }

    @Override
    public String getShortDesc() {
        return "Delete factor values from a dataset's design, with the differential expression analyses using their factor; dry-run unless -commit";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( Option.builder( FACTOR_VALUES_OPTION ).longOpt( "factor-values" ).hasArgs().valueSeparator( ',' )
                .desc( "IDs of the factor values to delete, comma-separated." ).get() );
        options.addOption( Option.builder( FACTOR_OPTION ).longOpt( "factor" ).hasArg()
                .desc( "Name or ID of the factor whose values -" + VALUES_OPTION + " names." ).get() );
        options.addOption( Option.builder( VALUES_OPTION ).longOpt( "values" ).hasArgs().valueSeparator( ',' )
                .desc( "Displayed values of the factor values to delete, comma-separated, ignoring case. A value that matches more than one factor value is refused; use -" + FACTOR_VALUES_OPTION + " for those." ).get() );
        options.addOption( COMMIT_OPTION, "commit", false, "Delete. Without it, only report what would be deleted." );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        boolean byId = commandLine.hasOption( FACTOR_VALUES_OPTION );
        boolean byValue = commandLine.hasOption( FACTOR_OPTION ) || commandLine.hasOption( VALUES_OPTION );
        if ( byId == byValue ) {
            throw new ParseException( "Give either -" + FACTOR_VALUES_OPTION + ", or -" + FACTOR_OPTION + " with -" + VALUES_OPTION + "." );
        }
        if ( byId ) {
            try {
                factorValueIds = Arrays.stream( commandLine.getOptionValues( FACTOR_VALUES_OPTION ) )
                        .map( String::trim ).map( Long::parseLong ).collect( Collectors.toList() );
            } catch ( NumberFormatException e ) {
                throw new ParseException( "-" + FACTOR_VALUES_OPTION + " takes numeric IDs: " + e.getMessage() );
            }
        } else {
            if ( !commandLine.hasOption( FACTOR_OPTION ) || !commandLine.hasOption( VALUES_OPTION ) ) {
                throw new ParseException( "-" + FACTOR_OPTION + " and -" + VALUES_OPTION + " go together." );
            }
            factor = commandLine.getOptionValue( FACTOR_OPTION );
            values = Arrays.stream( commandLine.getOptionValues( VALUES_OPTION ) ).map( String::trim ).collect( Collectors.toList() );
        }
        commit = commandLine.hasOption( COMMIT_OPTION );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        List<Long> ids = factorValueIds != null ? factorValueIds
                : factorValueDeletion.resolveFactorValueIds( ee, factor, values );
        String plan = factorValueDeletion.describeDeletion( ee, ids );
        if ( !commit ) {
            addSuccessObject( ee, "Dry run, nothing deleted; -commit would delete:\n" + plan );
            return;
        }
        factorValueDeletion.deleteFactorValues( ee, ids );
        addSuccessObject( ee, "Deleted:\n" + plan );
    }
}
