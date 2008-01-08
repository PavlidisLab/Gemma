/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * @author xiangwan
 */
public class ExpressionExperimentManager extends AbstractSpringAwareCLI {

    private char opt = 0x00;

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option option = OptionBuilder.hasArg().isRequired().withArgName( "Function" ).withDescription(
                "(A)nalysis:(L)oad:(D)elete?" ).withLongOpt( "function" ).create( 'x' );
        addOption( option );
        Option geneFileOption = OptionBuilder.hasArg().withArgName( "Gene Expression file" ).withDescription(
                "The Gene Expression File for analysis" ).withLongOpt( "genefile" ).create( 'g' );
        addOption( geneFileOption );

        Option geneFileListOption = OptionBuilder.hasArg().withArgName( "list of Gene Expression file" )
                .withDescription( "The list file of Gene Expression for analysis" ).withLongOpt( "listfile" ).create(
                        'f' );
        addOption( geneFileListOption );

    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'x' ) ) {
            this.opt = getOptionValue( 'x' ).charAt( 0 );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Expression Experiment Manager", args );
        if ( err != null ) {
            return err;
        }
        ExpressionExperiment expressionExperiment = null;
        ExpressionExperimentService eeService = ( ExpressionExperimentService ) this
                .getBean( "expressionExperimentService" );
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
                case 'S':
                    Collection<ExpressionExperiment> all = eeService.loadAll();
                    for ( ExpressionExperiment iter : all ) {
                        System.out.print( "Name:" + iter.getName() + " " );
                        System.out.println( "DESCRIPTION: " + iter.getDescription() );
                    }
                    System.err.println( "Get " + all.size() + " Experiments!" );
                    break;
                case 'D':
                    if ( this.hasOption( 'f' ) ) {
                        String fileList = getOptionValue( 'f' );
                        InputStream is = new FileInputStream( fileList );
                        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
                        String accession = null;
                        while ( ( accession = br.readLine() ) != null ) {
                            if ( StringUtils.isBlank( accession ) ) continue;
                            expressionExperiment = eeService.findByShortName( accession );
                            if ( expressionExperiment == null )
                                continue;
                            else
                                eeService.delete( expressionExperiment );
                        }

                    } else if ( this.hasOption( 'g' ) ) {
                        String geneFile = getOptionValue( 'g' );
                        expressionExperiment = eeService.findByShortName( geneFile );
                        if ( expressionExperiment != null ) {
                            log.info( "Deleteing expression experiment " + geneFile );
                            eeService.delete( expressionExperiment );
                        } else
                            log.info( "Can't find expression experiment " + geneFile );
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
            log.info( "Running Time" + watch.getTime() );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
