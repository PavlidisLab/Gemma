package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.openjena.atlas.logging.Log;
import ubic.gemma.core.util.AbstractCLIContextCLI;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.GeeqService;
import ubic.gemma.persistence.service.expression.experiment.GeeqServiceImpl;

public class GeeqCli extends AbstractCLIContextCLI {

    private static final String OPT_MODE_ALL = "all";
    private static final String OPT_MODE_B_EFFECT = "beff";
    private static final String OPT_MODE_B_CONFOUND = "bcnf";
    private static final String OPT_MODE_REPS = "reps";
    private ExpressionExperimentService eeService;
    private GeeqService geeqService;
    private String startArg;
    private String stopArg;
    private GeeqServiceImpl.ScoringMode mode = GeeqServiceImpl.ScoringMode.all;

    public static void main( String[] args ) {
        GeeqCli p = new GeeqCli();
        tryDoWork( p, args );
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
    public String getCommandName() {
        return "generateGeeq";
    }

    @SuppressWarnings("AccessStaticViaInstance")
    @Override
    protected void buildOptions() {
        Option startOption = OptionBuilder.hasArg().withDescription(
                "The first experiment ID denoting the beginning of the ID range to run the GEEQ for. " ).isRequired()
                .create( "start" );
        addOption( startOption );

        Option stopOption = OptionBuilder.hasArg()
                .withDescription( "The ID of the last experiment to generate GEEQ for, i.e the end of the ID range." )
                .isRequired().create( "stop" );
        addOption( stopOption );

        Option modeOption = OptionBuilder.hasArg()
                .withDescription( "If specified, switches the scoring mode. By default the mode is set to 'all'" //
                        + "\n Possible values are:" //
                        + "\n " + OPT_MODE_ALL + " - runs all scoring" //
                        + "\n " + OPT_MODE_B_EFFECT + "- recalcualtes batch effect score" //
                        + "\n " + OPT_MODE_B_CONFOUND + " - recalculates batch confound score" //
                        + "\n " + OPT_MODE_REPS + " - recalculates replicates score" ).isRequired().create( 'm' );
        addOption( modeOption );
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
            String m = this.getOptionValue( 'm' );
            switch ( m ) {
                default:
                case OPT_MODE_ALL:
                    this.mode = GeeqServiceImpl.ScoringMode.all;
                    break;
                case OPT_MODE_B_EFFECT:
                    this.mode = GeeqServiceImpl.ScoringMode.batchEffect;
                    break;
                case OPT_MODE_B_CONFOUND:
                    this.mode = GeeqServiceImpl.ScoringMode.batchConfound;
                    break;
                case OPT_MODE_REPS:
                    this.mode = GeeqServiceImpl.ScoringMode.replicates;
                    break;
            }
        }
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( args );
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
