package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.openjena.atlas.logging.Log;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;

import static ubic.gemma.persistence.service.expression.experiment.GeeqService.*;

public class GeeqCli extends AbstractCLIContextCLI {
    private ExpressionExperimentService eeService;
    private GeeqService geeqService;
    private String startArg;
    private String stopArg;
    private String mode = GeeqService.OPT_MODE_ALL;

    public static void main( String[] args ) {
        GeeqCli p = new GeeqCli();
        AbstractCLIContextCLI.tryDoWork( p, args );
    }

    @Override
    public GemmaCLI.CommandGroup getCommandGroup() {
        return GemmaCLI.CommandGroup.EXPERIMENT;
    }

    @Override
    public String getShortDesc() {
        return "Generate GEEQ for given EE ID range.";
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        eeService = this.getBean( ExpressionExperimentService.class );
        geeqService = this.getBean( GeeqService.class );

        if ( this.hasOption( "start" ) ) {
            this.startArg = this.getOptionValue( "start" );
        }
        if ( this.hasOption( "stop" ) ) {
            this.stopArg = this.getOptionValue( "stop" );
        }
        if ( this.hasOption( 'm' ) ) {
            this.mode = this.getOptionValue( 'm' );
        }
    }

    @Override
    public String getCommandName() {
        return "runGeeq";
    }

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    protected void buildOptions() {
        Option startOption = Option.builder( "start" )
                .desc( "The first experiment ID denoting the beginning of the ID range to run the GEEQ for. " ).hasArg()
                .required().build();
        this.addOption( startOption );

        Option stopOption = Option.builder( "stop" )
                .desc( "The ID of the last experiment to generate GEEQ for, i.e the end of the ID range." ).hasArg()
                .required().build();
        this.addOption( stopOption );

        Option modeOption = Option.builder( "m" ).longOpt( "mode" )
                .desc( "If specified, switches the scoring mode. By default the mode is set to 'all'" //
                        + "\n Possible values are:" //
                        + "\n " + OPT_MODE_ALL + " - runs all scoring" //
                        + "\n " + OPT_MODE_BATCH + "- recalcualtes batch related scores - info, confound and batch effect" //
                        + "\n " + OPT_MODE_REPS + " - recalculates score for replicates" //
                        + "\n " + OPT_MODE_PUB + " - recalculates score for publication" ).hasArg().build();
        this.addOption( modeOption );
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = super.processCommandLine( args );
        if ( err != null )
            return err;

        Exception lastE = null;
        // Loads the EE with largest ID.
        long max = eeService.loadValueObjectsPreFilter( 0, 1, "id", false, null ).iterator().next().getId();
        long start = Long.parseLong( startArg );
        long stop = Long.parseLong( stopArg ) + 1;
        String msg = "Success, no problems";
        int ran = 0;
        int errorred = 0;

        for ( long i = start; i < stop; i++ ) {
            if ( i > max ) {
                msg = "Success, max ID reached before getting to stop arg: " + max;
                break;
            }
            try {
                ExpressionExperiment ee = eeService.load( i );
                if ( ee != null ) {
                    geeqService.calculateScore( i, mode );
                    ran++;
                }
            } catch ( Exception e ) {
                System.out.println( i + " failed: " + e.getMessage() );
                errorred++;
                lastE = e;
            }
        }

        if ( lastE != null ) {
            Log.info( this.getClass(),
                    "Geeq for some EEs failed, only ran " + ran + " EEs. Error on " + errorred + " EEs." );
            return lastE;
        }

        Log.info( this.getClass(), msg + ", ran " + ran + " EEs." );
        return null;
    }
}
