package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author xiangwan
 *
 */

public class ExpressionExperimentManager extends AbstractSpringAwareCLI {

    private char opt = 0x00;

    private String cmdPara = null;

    @Override
    protected void buildOptions() {
        // TODO Auto-generated method stub
        Option option = OptionBuilder.hasArg().isRequired().withArgName( "Function" ).withDescription(
                "(A)nalysis:(L)oad:(D)elete?" ).withLongOpt( "function" ).create( 'x' );
        addOption( option );
    }

    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'x' ) ) {
            this.opt = getOptionValue( 'x' ).charAt( 0 );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {
        // TODO Auto-generated method stub
        Exception err = processCommandLine( "Expression Experiment Manager", args );
        if ( err != null ) {
            return err;
        }
        try {
            switch ( Character.toUpperCase( this.opt ) ) {
                case 'A':
                    LinkAnalysisCli analysis = new LinkAnalysisCli();
                    analysis.doWork( args );
                    break;
                case 'L':
                    LoadExpressionDataCli load = new LoadExpressionDataCli();
                    load.doWork( args );
                    break;
                case 'D':
                    if ( this.hasOption( 'f' ) ) {
                        String fileList = getOptionValue( 'f' );
                        ExpressionExperiment expressionExperiment = null;
                        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                                .getBean( "expressionExperimentService" );
                        expressionExperiment = eeService.findByShortName( fileList );
                        if ( expressionExperiment == null ) {
                            InputStream is = new FileInputStream( fileList );
                            if ( is == null ) break;
                            BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
                            String accession = null;
                            while ( ( accession = br.readLine() ) != null ) {
                                if ( StringUtils.isBlank( accession ) ) {
                                    continue;
                                }
                                expressionExperiment = eeService.findByShortName( accession );
                                if ( expressionExperiment == null ) continue;
                            }

                        } else
                            eeService.delete( expressionExperiment );
                    }
            }
        } catch ( Exception e ) {
            log.error( e );
            return e;
        }

        return null;
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        // TODO Auto-generated method stub
        ExpressionExperimentManager eeManager = new ExpressionExperimentManager();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = eeManager.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            System.err.println( watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
