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
import java.util.Collection;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
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
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedLinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.TooSmallDatasetLinkAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.arrayDesign.TechnologyTypeEnum;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Commandline tool to conduct link analysis
 * 
 * @author xiangwan
 * @version $Id$
 */
public class LinkAnalysisCli extends ExpressionExperimentManipulatingCli {

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

    // private String geneExpressionFile = null;
    private ExpressionExperimentService eeService = null;

    private DesignElementDataVectorService vectorService = null;

    private LinkAnalysis linkAnalysis = new LinkAnalysis();

    private double tooSmallToKeep = DEFAULT_TOOSMALLTOKEEP;

    private boolean minPresentFractionIsSet = true;
    private boolean lowExpressionCutIsSet = true;
    private double minPresentFraction = DEFAULT_MINPRESENT_FRACTION;
    private double lowExpressionCut = DEFAULT_LOWEXPRESSIONCUT;
    private double highExpressionCut = DEFAULT_HIGHEXPRESSION_CUT;

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
    private void audit( ExpressionExperiment ee, String note, AuditEventType eventType ) {
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

    /**
     * @param ee
     * @param builder
     * @param eeDoubleMatrix
     * @return
     */
    private ExpressionDataDoubleMatrix filter( ExpressionExperiment ee, ExpressionDataMatrixBuilder builder,
            ExpressionDataDoubleMatrix eeDoubleMatrix ) {
        if ( eeDoubleMatrix.rows() == 0 ) throw new IllegalStateException( "No data found!" );

        if ( eeDoubleMatrix.rows() < MINIMUM_ROWS_TO_BOTHER )
            throw new IllegalArgumentException( "To few rows in " + ee.getShortName() + " (" + eeDoubleMatrix.rows()
                    + "), data sets are not analyzed unless they have at least " + MINIMUM_ROWS_TO_BOTHER + " rows" );

        if ( eeDoubleMatrix.columns() < LinkAnalysisCli.MINIMUM_SAMPLE )
            throw new InsufficientSamplesException( "Not enough samples " + ee.getShortName() + ", must have at least "
                    + LinkAnalysisCli.MINIMUM_SAMPLE + " to be eligble for link analysis." );

        eeDoubleMatrix = this.filter( eeDoubleMatrix, builder );

        if ( eeDoubleMatrix == null )
            throw new IllegalStateException( "Failed to get filtered data matrix, it was null " + ee.getShortName() );
        return eeDoubleMatrix;
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
     * @param matrix
     * @return
     */
    private ExpressionDataDoubleMatrix lowExpressionFilter( ExpressionDataDoubleMatrix matrix ) {
        RowLevelFilter rowLevelFilter = new RowLevelFilter();
        rowLevelFilter.setLowCut( this.lowExpressionCut );
        rowLevelFilter.setHighCut( this.highExpressionCut );
        rowLevelFilter.setRemoveAllNegative( true );
        rowLevelFilter.setUseAsFraction( true );
        return rowLevelFilter.filter( matrix );
    }

    /**
     * @param matrix
     * @return
     */
    private ExpressionDataDoubleMatrix minPresentFilter( ExpressionDataDoubleMatrix matrix,
            ExpressionDataBooleanMatrix absentPresent ) {
        log.info( "Filtering out genes that are missing too many values" );

        RowMissingValueFilter rowMissingFilter = new RowMissingValueFilter();
        if ( absentPresent != null ) {
            if ( absentPresent.rows() != matrix.rows() ) {
                log.warn( "Missing value matrix has " + absentPresent.rows() + " rows (!=" + matrix.rows() + ")" );
            }

            if ( absentPresent.columns() != matrix.columns() ) {
                throw new IllegalArgumentException( "Missing value matrix has " + absentPresent.columns()
                        + " columns (!=" + matrix.columns() + ")" );
            }

            rowMissingFilter.setAbsentPresentCalls( absentPresent );
        }
        rowMissingFilter.setMinPresentFraction( minPresentFraction );
        return ( ExpressionDataDoubleMatrix ) rowMissingFilter.filter( matrix );
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

        log.info( "Begin link processing: " + ee );
        Collection<DesignElementDataVector> dataVectors = getVectors( ee );

        if ( dataVectors == null ) throw new IllegalArgumentException( "No data vectors in " + ee );

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
        log.info( "Starting generating Raw Links for " + ee );
        this.linkAnalysis.analyze();
        log.info( "Done with processing of " + ee );

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

        super.addDateOption();

        Option geneFileOption = OptionBuilder.hasArg().withArgName( "dataSet" ).withDescription(
                "Short name of the expression experiment to analyze (default is to analyze all found in the database)" )
                .withLongOpt( "dataSet" ).create( 'e' );
        addOption( geneFileOption );

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
                .withLongOpt( "missingcut" ).create( 'm' );
        addOption( minPresentFraction );

        Option lowExpressionCut = OptionBuilder.hasArg().withArgName( "Expression Threshold" ).withDescription(
                "The tolerance for accepting the expression values, default=" + DEFAULT_LOWEXPRESSIONCUT ).withLongOpt(
                "lowcut" ).create( 'l' );
        addOption( lowExpressionCut );

        Option absoluteValue = OptionBuilder.withDescription( "If using absolute value in expression file" )
                .withLongOpt( "abs" ).create( 'a' );
        addOption( absoluteValue );

        Option useDB = OptionBuilder.withDescription( "Don't save the results in the database (i.e., testing)" )
                .withLongOpt( "nodb" ).create( 'd' );
        addOption( useDB );

    }

    @SuppressWarnings("unchecked")
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Link Analysis Data Loader", args );
        if ( err != null ) {
            return err;
        }

        this.eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        this.vectorService = ( DesignElementDataVectorService ) this.getBean( "designElementDataVectorService" );

        this.linkAnalysis.setDEService( vectorService );
        this.linkAnalysis.setCsService( ( CompositeSequenceService ) this.getBean( "compositeSequenceService" ) );
        this.linkAnalysis.setPPService( ( Probe2ProbeCoexpressionService ) this
                .getBean( "probe2ProbeCoexpressionService" ) );

        if ( this.getExperimentShortName() == null ) {
            if ( this.experimentListFile == null ) {
                Collection<ExpressionExperiment> all = eeService.loadAll();
                log.info( "Total ExpressionExperiment: " + all.size() );
                for ( ExpressionExperiment ee : all ) {

                    if ( !needToRun( ee, LinkAnalysisEvent.class ) ) {
                        continue;
                    }

                    try {
                        this.process( ee );
                        successObjects.add( ee.toString() );
                        audit( ee, "Part of run on all EEs", LinkAnalysisEvent.Factory.newInstance() );
                    } catch ( Exception e ) {
                        errorObjects.add( ee + ": " + e.getMessage() );
                        logFailure( ee, e );
                        log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
                    }
                }
            } else {
                try {
                    InputStream is = new FileInputStream( this.experimentListFile );
                    BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
                    String shortName = null;
                    while ( ( shortName = br.readLine() ) != null ) {
                        if ( StringUtils.isBlank( shortName ) ) continue;
                        ExpressionExperiment expressionExperiment = eeService.findByShortName( shortName );

                        if ( expressionExperiment == null ) {
                            errorObjects.add( shortName + " is not found in the database! " );
                            continue;
                        }

                        if ( !needToRun( expressionExperiment, LinkAnalysisEvent.class ) ) {
                            continue;
                        }

                        try {
                            this.process( expressionExperiment );
                            successObjects.add( expressionExperiment.toString() );

                            audit( expressionExperiment, "From list in file: " + experimentListFile,
                                    LinkAnalysisEvent.Factory.newInstance() );
                        } catch ( Exception e ) {
                            errorObjects.add( expressionExperiment + ": " + e.getMessage() );

                            logFailure( expressionExperiment, e );

                            e.printStackTrace();
                            log.error( "**** Exception while processing " + expressionExperiment + ": "
                                    + e.getMessage() + " ********" );
                        }
                    }
                } catch ( Exception e ) {
                    return e;
                }
            }
            summarizeProcessing();
        } else {
            String[] shortNames = this.getExperimentShortName().split( "," );

            for ( String shortName : shortNames ) {
                ExpressionExperiment expressionExperiment = locateExpressionExperiment( shortName );

                if ( expressionExperiment == null ) {
                    continue;
                }

                if ( !needToRun( expressionExperiment, LinkAnalysisEvent.class ) ) {
                    return null;
                }

                try {
                    this.process( expressionExperiment );
                    audit( expressionExperiment, "From list in file: " + experimentListFile, LinkAnalysisEvent.Factory
                            .newInstance() );
                } catch ( Exception e ) {
                    e.printStackTrace();
                    logFailure( expressionExperiment, e );
                    log.error( "**** Exception while processing " + expressionExperiment + ": " + e.getMessage()
                            + " ********" );
                }
            }

        }
        return null;
    }

    /**
     * @param expressionExperiment
     * @param e
     */
    private void logFailure( ExpressionExperiment expressionExperiment, Exception e ) {
        if ( e instanceof InsufficientSamplesException ) {
            audit( expressionExperiment, e.getMessage(), TooSmallDatasetLinkAnalysisEvent.Factory.newInstance() );
        } else {
            audit( expressionExperiment, ExceptionUtils.getFullStackTrace( e ), FailedLinkAnalysisEvent.Factory
                    .newInstance() );
        }
    }

    @Override
    protected void processOptions() {
        super.processOptions();

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
        if ( hasOption( 'l' ) ) {
            this.lowExpressionCutIsSet = true;
            this.lowExpressionCut = Double.parseDouble( getOptionValue( 'l' ) );
        }

        if ( hasOption( 'a' ) ) {
            this.linkAnalysis.setAbsoluteValue();
        }
        if ( hasOption( 'd' ) ) {
            this.linkAnalysis.setUseDB( false );
        }

        this.auditTrailService = ( AuditTrailService ) this.getBean( "auditTrailService" );
    }

}

class InsufficientSamplesException extends RuntimeException {

    public InsufficientSamplesException() {
        super();
    }

    public InsufficientSamplesException( String arg0, Throwable arg1 ) {
        super( arg0, arg1 );
    }

    public InsufficientSamplesException( String arg0 ) {
        super( arg0 );
    }

    public InsufficientSamplesException( Throwable arg0 ) {
        super( arg0 );
    }

}