/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.analysis.linkAnalysis.LinkAnalysis;
import ubic.gemma.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.analysis.preprocess.filter.AffyProbeNameFilter;
import ubic.gemma.analysis.preprocess.filter.RowLevelFilter;
import ubic.gemma.analysis.preprocess.filter.RowMissingValueFilter;
import ubic.gemma.analysis.preprocess.filter.AffyProbeNameFilter.Pattern;
import ubic.gemma.datastructure.matrix.ExpressionDataBooleanMatrix;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.arrayDesign.TechnologyTypeEnum;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.util.AbstractSpringAwareCLI;
import ubic.gemma.util.DateUtil;

/**
 * Commandline tool to conduct the link analysis
 * 
 * @author xiangwan
 * @version $Id$
 */
public class LinkAnalysisCli extends AbstractSpringAwareCLI {

    /**
     * Fewer rows than this, and we bail.
     */
    private static final int MINIMUM_ROWS_TO_BOTHER = 100;

    private static final double DEFAULT_HIGHEXPRESSION_CUT = 0.0;

    private static final double DEFAULT_LOWEXPRESSIONCUT = 0.3;

    private static final double DEFAULT_TOOSMALLTOKEEP = 0.5;

    private static final double DEFAULT_MINPRESENT_FRACTION = 0.3;

    /**
     * How many samples a dataset has to have before we consider analyzing it.
     */
    final static int MINIMUM_SAMPLE = 5;

    /**
     * @param args
     */
    public static void main( String[] args ) {
        LinkAnalysisCli analysis = new LinkAnalysisCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = analysis.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( "Elapsed time: " + watch.getTime() / 1000 + " seconds" );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Use for batch processing These two files could contain the lists of experiment;
     */
    private String geneExpressionList = null;

    private String geneExpressionFile = null;

    private ExpressionExperimentService eeService = null;

    private DesignElementDataVectorService vectorService = null;

    private LinkAnalysis linkAnalysis = new LinkAnalysis();

    private double tooSmallToKeep = DEFAULT_TOOSMALLTOKEEP;

    private boolean minPresentFractionIsSet = true;
    private boolean lowExpressionCutIsSet = true;
    private double minPresentFraction = DEFAULT_MINPRESENT_FRACTION;
    private double lowExpressionCut = DEFAULT_LOWEXPRESSIONCUT;
    private double highExpressionCut = DEFAULT_HIGHEXPRESSION_CUT;
    private String mDate;

    AuditTrailService auditTrailService;

    /**
     * @param ee
     * @param filteredMatrix
     * @param arrayDesign
     * @return
     */
    private ExpressionDataDoubleMatrix affyControlProbeFilter( ExpressionDataDoubleMatrix matrix ) {

        AffyProbeNameFilter affyProbeNameFilter = new AffyProbeNameFilter( new Pattern[] { Pattern.AFFX } );
        return affyProbeNameFilter.filter( matrix );
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note ) {
        AuditEventType eventType = LinkAnalysisEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    /**
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<ArrayDesign> checkForMixedTechnologies( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );
        if ( arrayDesignsUsed.size() > 1 ) {
            boolean containsTwoColor = false;
            boolean containsOneColor = false;
            for ( ArrayDesign arrayDesign : arrayDesignsUsed ) {
                if ( arrayDesign.getTechnologyType().equals( TechnologyType.ONECOLOR ) ) {
                    containsOneColor = true;
                }
                if ( !arrayDesign.getTechnologyType().equals( TechnologyType.ONECOLOR ) ) {
                    containsTwoColor = true;
                }
            }

            if ( containsTwoColor && containsOneColor ) {
                throw new UnsupportedOperationException(
                        "Can't correctly handle expression experiments that combine different array technologies." );
            }
        }
        return arrayDesignsUsed;
    }

    /**
     * Apply filters as configured by the command line parameters and technology type.
     * 
     * @param dataMatrix
     * @param eeDoubleMatrix
     * @param ee
     * @return
     */
    private ExpressionDataDoubleMatrix filter( ExpressionDataDoubleMatrix eeDoubleMatrix,
            ExpressionDataMatrixBuilder builder ) {

        ExpressionDataDoubleMatrix filteredMatrix = eeDoubleMatrix;

        boolean twoColor = isTwoColor( builder.getExpressionExperiment() );
        if ( minPresentFractionIsSet && twoColor ) {
            /* Apply two color missing value filter */
            ExpressionDataBooleanMatrix missingValues = builder.getMissingValueData( null );
            filteredMatrix = minPresentFilter( filteredMatrix, missingValues );
        }

        if ( !twoColor ) {

            if ( minPresentFractionIsSet ) filteredMatrix = minPresentFilter( filteredMatrix, null );

            if ( lowExpressionCutIsSet ) filteredMatrix = lowExpressionFilter( eeDoubleMatrix );

            if ( usesAffymetrix( builder.getExpressionExperiment() ) )
                filteredMatrix = affyControlProbeFilter( filteredMatrix );
        }
        return filteredMatrix;
    }

    private ExpressionDataDoubleMatrix filter( ExpressionExperiment ee, ExpressionDataMatrixBuilder builder,
            ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        if ( eeDoubleMatrix.rows() == 0 ) throw new IllegalStateException( "No data found!" );

        if ( eeDoubleMatrix.rows() < MINIMUM_ROWS_TO_BOTHER )
            throw new IllegalArgumentException( "Most Probes are filtered out " + ee.getShortName() );

        if ( eeDoubleMatrix.columns() < LinkAnalysisCli.MINIMUM_SAMPLE )
            throw new IllegalArgumentException( "No enough samples " + ee.getShortName() );

        eeDoubleMatrix = this.filter( eeDoubleMatrix, builder );

        if ( eeDoubleMatrix == null )
            throw new IllegalStateException( "Failed to get filtered data matrix " + ee.getShortName() );
        return eeDoubleMatrix;
    }

    /**
     * FIXME this code was copied from ArrayDesignSequenceManipulatingCli
     * 
     * @param expressionExperiment
     * @param eventClass
     * @return
     */
    private List<AuditEvent> getEvents( ExpressionExperiment expressionExperiment,
            Class<? extends ExpressionExperimentAnalysisEvent> eventClass ) {
        List<AuditEvent> events = new ArrayList<AuditEvent>();

        for ( AuditEvent event : expressionExperiment.getAuditTrail().getEvents() ) {
            if ( event == null ) continue;
            if ( event.getEventType() != null && eventClass.isAssignableFrom( event.getEventType().getClass() ) ) {
                events.add( event );
            }
        }
        return events;
    }

    /**
     * @param ee
     * @param dataVectors
     * @return
     */
    private ExpressionDataDoubleMatrix getFilteredMatrix( ExpressionExperiment ee,
            Collection<DesignElementDataVector> dataVectors ) {
        log.info( "Getting expression data..." );
        ExpressionDataMatrixBuilder builder = new ExpressionDataMatrixBuilder( dataVectors );

        ExpressionDataDoubleMatrix eeDoubleMatrix = builder.getPreferredData();

        eeDoubleMatrix = filter( ee, builder, eeDoubleMatrix );
        return eeDoubleMatrix;
    }

    /**
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    private Collection<DesignElementDataVector> getVectors( ExpressionExperiment ee ) {
        checkForMixedTechnologies( ee );
        Collection<QuantitationType> qts = ExpressionDataMatrixBuilder.getUsefulQuantitationTypes( ee );
        if ( qts.size() == 0 ) throw new IllegalArgumentException( "No usable quantitation type in " + ee );

        log.info( "Loading vectors..." );
        Collection<DesignElementDataVector> dataVectors = eeService.getDesignElementDataVectors( ee, qts );
        vectorService.thaw( dataVectors );
        return dataVectors;
    }

    /**
     * Determine if the expression experiment uses two-color arrays. This is not guaranteed to give the right answer if
     * the experiment uses both types of technologies.F
     * 
     * @param ee
     * @return
     */
    @SuppressWarnings("unchecked")
    private boolean isTwoColor( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );

        ArrayDesign arrayDesign = ( ArrayDesign ) arrayDesignsUsed.iterator().next();
        TechnologyType techType = arrayDesign.getTechnologyType();
        return techType.equals( TechnologyTypeEnum.TWOCOLOR ) || techType.equals( TechnologyType.DUALMODE );
    }

    /**
     * @param ee
     * @param filteredMatrix
     * @return
     */
    private ExpressionDataDoubleMatrix lowExpressionFilter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setLowCut( this.lowExpressionCut );
        rowLevelFilter.setHighCut( this.highExpressionCut );
        rowLevelFilter.setRemoveAllNegative( true ); // todo: fix
        rowLevelFilter.setUseAsFraction( true );
        return rowLevelFilter.filter( matrix );
    }

    /**
     * @param filteredMatrix
     * @return
     */
    private ExpressionDataDoubleMatrix minPresentFilter( ExpressionDataDoubleMatrix filteredMatrix,
            ExpressionDataBooleanMatrix absentPresent ) {
        log.info( "Filtering out genes that are missing too many values" );
        RowMissingValueFilter rowMissingFilter = new RowMissingValueFilter();
        if ( absentPresent != null ) rowMissingFilter.setAbsentPresentCalls( absentPresent );
        rowMissingFilter.setMinPresentFraction( minPresentFraction );
        return ( ExpressionDataDoubleMatrix ) rowMissingFilter.filter( filteredMatrix );
    }

    /**
     * @param ee
     * @return
     */
    private void process( ExpressionExperiment ee ) throws Exception {
        linkAnalysis.clear();

        if ( this.hasOption( 'd' ) ) {
            log.warn( "TEST MODE, Database will not be modified" );
        }

        Collection<DesignElementDataVector> dataVectors = getVectors( ee );

        if ( dataVectors == null ) throw new IllegalArgumentException( "No data vectors " + ee.getShortName() );

        ExpressionDataDoubleMatrix eeDoubleMatrix = getFilteredMatrix( ee, dataVectors );

        this.linkAnalysis.setDataMatrix( eeDoubleMatrix );
        this.linkAnalysis.setDataVectors( dataVectors ); // shouldn't have to do this.
        this.linkAnalysis.setTaxon( eeService.getTaxon( ee.getId() ) );

        /*
         * this value will be optimized depending on the size of experiment in the analysis. So it need to be set as the
         * given value before the analysis. Otherwise, the value in the previous experiment will be in effect for the
         * current experiment.
         */
        this.linkAnalysis.setTooSmallToKeep( this.tooSmallToKeep );

        /*
         * Start the analysis.
         */
        log.info( "Starting generating Raw Links for " + ee.getShortName() );
        this.linkAnalysis.analyze();
        log.info( "Generated Raw Links for " + ee.getShortName() );

    }

    @SuppressWarnings("unchecked")
    private boolean usesAffymetrix( ExpressionExperiment ee ) {
        Collection<ArrayDesign> arrayDesignsUsed = this.eeService.getArrayDesignsUsed( ee );
        ArrayDesign arrayDesign = arrayDesignsUsed.iterator().next();
        return arrayDesign.getName().toUpperCase().contains( "AFFYMETRIX" );
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {

        Option geneFileOption = OptionBuilder.hasArg().withArgName( "dataSet" ).withDescription(
                "Short name of the expression experiment to analyze (default is to analyze all found in the database)" )
                .withLongOpt( "dataSet" ).create( 'g' );
        addOption( geneFileOption );

        Option geneFileListOption = OptionBuilder.hasArg().withArgName( "list of Gene Expression file" )
                .withDescription(
                        "File with list of short names of expression experiments (one per line; use instead of '-g')" )
                .withLongOpt( "listfile" ).create( 'f' );
        addOption( geneFileListOption );

        Option cdfCut = OptionBuilder.hasArg().withArgName( "Tolerance Thresold" ).withDescription(
                "The tolerance threshold for coefficient value" ).withLongOpt( "cdfcut" ).create( 'c' );
        addOption( cdfCut );

        Option tooSmallToKeep = OptionBuilder.hasArg().withArgName( "Cache Threshold" ).withDescription(
                "The threshold for coefficient cache" ).withLongOpt( "cachecut" ).create( 'k' );
        addOption( tooSmallToKeep );

        Option fwe = OptionBuilder.hasArg().withArgName( "Family Wise Error Rate" ).withDescription(
                "The setting for family wise error control" ).withLongOpt( "fwe" ).create( 'w' );
        addOption( fwe );

        Option minPresentFraction = OptionBuilder.hasArg().withArgName( "Missing Value Threshold" ).withDescription(
                "The tolerance for accepting the gene with missing values, default=" + DEFAULT_MINPRESENT_FRACTION )
                .withLongOpt( "missing" ).create( 'm' );
        addOption( minPresentFraction );

        Option lowExpressionCut = OptionBuilder.hasArg().withArgName( "Expression Threshold" ).withDescription(
                "The tolerance for accepting the expression values, default=" + DEFAULT_LOWEXPRESSIONCUT ).withLongOpt(
                "expression" ).create( 'e' );
        addOption( lowExpressionCut );

        Option absoluteValue = OptionBuilder.withDescription( "If using absolute value in expression file" )
                .withLongOpt( "abs" ).create( 'a' );
        addOption( absoluteValue );

        Option useDB = OptionBuilder.withDescription( "Don't save the results in the database (i.e., testing)" )
                .withLongOpt( "nodb" ).create( 'd' );
        addOption( useDB );

        Option dateOption = OptionBuilder
                .hasArg()
                .withArgName( "mdate" )
                .withDescription(
                        "Constrain to run only on experiments with analyses older than the given date. "
                                + "For example, to run only on entities that have not been analyzed in the last 10 days, use '-10d'. "
                                + "If there is no record of when the analysis was last run, it will be run." ).create(
                        "mdate" );

        addOption( dateOption );

    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Link Analysis Data Loader", args );
        if ( err != null ) {
            return err;
        }

        Date skipIfLastRunLaterThan = getLimitingDate();

        this.eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        this.vectorService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );

        this.linkAnalysis.setDEService( vectorService );
        this.linkAnalysis.setCsService( ( CompositeSequenceService ) this.getBean( "compositeSequenceService" ) );
        this.linkAnalysis.setPPService( ( Probe2ProbeCoexpressionService ) this
                .getBean( "probe2ProbeCoexpressionService" ) );

        if ( this.geneExpressionFile == null ) {
            Collection<String> errorObjects = new HashSet<String>();
            Collection<String> persistedObjects = new HashSet<String>();
            if ( this.geneExpressionList == null ) {
                Collection<ExpressionExperiment> all = eeService.loadAll();
                log.info( "Total ExpressionExperiment: " + all.size() );
                for ( ExpressionExperiment ee : all ) {

                    if ( !needToRun( skipIfLastRunLaterThan, ee, LinkAnalysisEvent.class ) ) {
                        log.warn( ee + " was last run more recently than " + skipIfLastRunLaterThan );
                        continue;
                    }

                    try {
                        this.process( ee );
                        persistedObjects.add( ee.toString() );
                        audit( ee, "Part of run on all EEs" );
                    } catch ( Exception e ) {
                        errorObjects.add( ee + ": " + e.getMessage() );
                        e.printStackTrace();
                        log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
                    }
                }
            } else {
                try {
                    InputStream is = new FileInputStream( this.geneExpressionList );
                    BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
                    String shortName = null;
                    while ( ( shortName = br.readLine() ) != null ) {
                        if ( StringUtils.isBlank( shortName ) ) continue;
                        ExpressionExperiment expressionExperiment = eeService.findByShortName( shortName );

                        if ( expressionExperiment == null ) {
                            errorObjects.add( shortName + " is not found in the database! " );
                            continue;
                        }

                        if ( !needToRun( skipIfLastRunLaterThan, expressionExperiment, LinkAnalysisEvent.class ) ) {
                            log.warn( expressionExperiment + " was last run more recently than "
                                    + skipIfLastRunLaterThan );
                            continue;
                        }

                        try {
                            this.process( expressionExperiment );
                            persistedObjects.add( expressionExperiment.toString() );
                            audit( expressionExperiment, "From list in file: " + geneExpressionList );
                        } catch ( Exception e ) {
                            errorObjects.add( expressionExperiment + ": " + e.getMessage() );
                            e.printStackTrace();
                            log.error( "**** Exception while processing " + expressionExperiment + ": "
                                    + e.getMessage() + " ********" );
                        }
                    }
                } catch ( Exception e ) {
                    return e;
                }
            }
            summarizeProcessing( errorObjects, persistedObjects );
        } else {
            ExpressionExperiment expressionExperiment = eeService.findByShortName( this.geneExpressionFile );

            if ( expressionExperiment == null ) {
                log.info( this.geneExpressionFile + " is not loaded yet!" );
                return null;
            }

            if ( !needToRun( skipIfLastRunLaterThan, expressionExperiment, LinkAnalysisEvent.class ) ) {
                log.warn( expressionExperiment + " was last run more recently than " + skipIfLastRunLaterThan );
                return null;
            }

            try {
                this.process( expressionExperiment );
                audit( expressionExperiment, "From list in file: " + geneExpressionList );
            } catch ( Exception e ) {
                e.printStackTrace();
                log.error( "**** Exception while processing " + expressionExperiment + ": " + e.getMessage()
                        + " ********" );
            }

        }
        return null;
    }

    /**
     * FIXME this code was copied from ArrayDesignSequenceManipulatingCli
     * 
     * @return
     */
    protected Date getLimitingDate() {
        Date skipIfLastRunLaterThan = null;
        if ( StringUtils.isNotBlank( mDate ) ) {
            skipIfLastRunLaterThan = DateUtil.getRelativeDate( new Date(), mDate );
            log.info( "Analyses will be run only if last was older than " + skipIfLastRunLaterThan );
        }
        return skipIfLastRunLaterThan;
    }

    /**
     * FIXME this code was copied from ArrayDesignSequenceManipulatingCli
     * 
     * @param skipIfLastRunLaterThan
     * @param expressionExperiment
     * @param eventClass e.g., ArrayDesignSequenceAnalysisEvent.class
     * @return true if skipIfLastRunLaterThan is null, or there is no record of a previous analysis, or if the last
     *         analysis was run before skipIfLastRunLaterThan. false otherwise.
     */
    protected boolean needToRun( Date skipIfLastRunLaterThan, ExpressionExperiment expressionExperiment,
            Class<? extends ExpressionExperimentAnalysisEvent> eventClass ) {
        if ( skipIfLastRunLaterThan == null ) return true;
        auditTrailService.thaw( expressionExperiment );

        List<AuditEvent> sequenceAnalysisEvents = getEvents( expressionExperiment, eventClass );

        if ( sequenceAnalysisEvents.size() == 0 ) {
            return true; // always do it
        } else {
            // return true if the last time was older than the limit time.
            AuditEvent lastEvent = sequenceAnalysisEvents.get( sequenceAnalysisEvents.size() - 1 );
            return lastEvent.getDate().before( skipIfLastRunLaterThan );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( 'g' ) ) {
            this.geneExpressionFile = getOptionValue( 'g' );
        }
        if ( hasOption( 'f' ) ) {
            this.geneExpressionList = getOptionValue( 'f' );
        }
        if ( hasOption( 'c' ) ) {
            this.linkAnalysis.setCdfCut( Double.parseDouble( getOptionValue( 'c' ) ) );
        }
        if ( hasOption( 'k' ) ) {
            this.tooSmallToKeep = Double.parseDouble( getOptionValue( 'k' ) );
            this.linkAnalysis.setTooSmallToKeep( this.tooSmallToKeep );
        }
        if ( hasOption( 'w' ) ) {
            this.linkAnalysis.setFwe( Double.parseDouble( getOptionValue( 'w' ) ) );
        }

        if ( hasOption( 'm' ) ) {
            this.minPresentFractionIsSet = true;
            this.minPresentFraction = Double.parseDouble( getOptionValue( 'm' ) );
        }
        if ( hasOption( 'e' ) ) {
            this.lowExpressionCutIsSet = true;
            this.lowExpressionCut = Double.parseDouble( getOptionValue( 'e' ) );
        }

        if ( hasOption( 'a' ) ) {
            this.linkAnalysis.setAbsoluteValue();
        }
        if ( hasOption( 'd' ) ) {
            this.linkAnalysis.setUseDB( false );
        }

        if ( hasOption( "mdate" ) ) {
            this.mDate = this.getOptionValue( "mdate" );
        }

        this.auditTrailService = ( AuditTrailService ) this.getBean( "auditTrailService" );
    }

}
