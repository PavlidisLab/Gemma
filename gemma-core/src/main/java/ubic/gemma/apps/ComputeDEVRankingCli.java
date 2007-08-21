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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.preprocess.DedvRankService;
import ubic.gemma.analysis.preprocess.DedvRankService.Method;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.RankComputationEvent;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * For each DesignElementDataVector in the experiment, compute the 'rank' of the expression level. For experiments using
 * multiple array designs, ranks are computed on a per-array basis.
 * 
 * @author xwan
 * @version $Id$
 * @see ubic.gemma.analysis.preprocess.DedvRankService
 */
public class ComputeDEVRankingCli extends AbstractGeneExpressionExperimentManipulatingCLI {

    private static Log log = LogFactory.getLog( ComputeDEVRankingCli.class.getName() );

    private String geneExpressionList = null;
    private Method method = Method.MAX;

    /**
     * 
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        super.buildOptions();

        Option geneFileListOption = OptionBuilder.hasArg().withArgName( "list of Gene Expression file" )
                .withDescription(
                        "File with list of short names of expression experiments (one per line; use instead of '-g')" )
                .withLongOpt( "listfile" ).create( 'f' );

        addOption( geneFileListOption );

        Option methodOption = OptionBuilder.hasArg().withArgName( "Method for determining row statistics" )
                .withDescription( "MEAN, MEDIAN, MAX or MIN; default = MAX" ).withLongOpt( "method" ).create( 'm' );

        addOption( methodOption );

        addDateOption();
    }

    DedvRankService dedvRankservice;

    private AuditTrailService auditTrailService;

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note ) {
        AuditEventType eventType = RankComputationEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    /**
     * 
     */
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 'f' ) ) {
            this.geneExpressionList = getOptionValue( 'f' );
        }
        if ( hasOption( 'm' ) ) {
            this.method = Method.valueOf( getOptionValue( 'm' ) );
        }
        dedvRankservice = ( DedvRankService ) this.getBean( "dedvRankService" );
        this.auditTrailService = ( AuditTrailService ) this.getBean( "auditTrailService" );
        eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
    }

    ExpressionExperimentService eeService;

    /**
     * 
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "DEV Ranking Calculator ", args );
        if ( err != null ) {
            return err;
        }

        ExpressionExperiment expressionExperiment = null;

        if ( this.getExperimentShortName() == null ) {

            if ( this.geneExpressionList == null ) {
                Collection<ExpressionExperiment> all = eeService.loadAll();
                log.info( "Total ExpressionExperiment: " + all.size() );
                for ( ExpressionExperiment ee : all ) {
                    processExperiment( ee );
                }
            } else {
                try {
                    InputStream is = new FileInputStream( this.geneExpressionList );
                    BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
                    String shortName = null;
                    while ( ( shortName = br.readLine() ) != null ) {
                        if ( StringUtils.isBlank( shortName ) ) continue;
                        expressionExperiment = eeService.findByShortName( shortName );

                        if ( expressionExperiment == null ) {
                            errorObjects.add( shortName + " is not found in the database! " );
                            continue;
                        }
                        processExperiment( expressionExperiment );
                    }
                } catch ( Exception e ) {
                    return e;
                }
            }
            summarizeProcessing();
        } else {
            String[] shortNames = this.getExperimentShortName().split( "," );

            for ( String shortName : shortNames ) {
                ExpressionExperiment ee = locateExpressionExperiment( shortName );

                if ( ee == null ) {
                    continue;
                }

                processExperiment( ee );
            }
            summarizeProcessing();

        }

        return null;
    }

    /**
     * @param errorObjects
     * @param persistedObjects
     * @param ee
     */
    private void processExperiment( ExpressionExperiment ee ) {
        try {
            eeService.thawLite( ee );
            boolean needToRun = needToRun( ee, RankComputationEvent.class );

            if ( !needToRun ) return;
            this.dedvRankservice.computeDevRankForExpressionExperiment( ee, method );
            successObjects.add( ee.toString() );
            audit( ee, "" );
        } catch ( Exception e ) {
            errorObjects.add( ee + ": " + e.getMessage() );
            log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********", e );
        }
    }

    /**
     * @param args
     */
    public static void main( String[] args ) {
        ComputeDEVRankingCli computing = new ComputeDEVRankingCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = computing.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

}
