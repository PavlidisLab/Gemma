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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.time.StopWatch;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig.NormalizationMethod;
import ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisConfig.SingularThreshold;
import ubic.gemma.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Commandline tool to conduct link analysis.
 * 
 * @author xiangwan
 * @author paul (refactoring)
 * @author vaneet
 * @version $Id$
 */
public class LinkAnalysisCli extends ExpressionExperimentManipulatingCLI {

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

    private LinkAnalysisService linkAnalysisService;

    private FilterConfig filterConfig = new FilterConfig();

    private LinkAnalysisConfig linkAnalysisConfig = new LinkAnalysisConfig();

    private String dataFileName = null;

    private String analysisTaxon = null;

    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for coexpressed genes";
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.apps.ExpressionExperimentManipulatingCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        super.addDateOption();

        Option cdfCut = OptionBuilder.hasArg().withArgName( "Tolerance Thresold" ).withDescription(
                "The tolerance threshold for coefficient value" ).withLongOpt( "cdfcut" ).create( 'c' );
        addOption( cdfCut );

        Option tooSmallToKeep = OptionBuilder.hasArg().withArgName( "Cache Threshold" ).withDescription(
                "The threshold for coefficient cache" ).withLongOpt( "cachecut" ).create( 'k' );
        addOption( tooSmallToKeep );

        Option fwe = OptionBuilder.hasArg().withArgName( "Family Wise Error Rate" ).withDescription(
                "The setting for family wise error control" ).withLongOpt( "fwe" ).create( 'w' );
        addOption( fwe );

        buildFilterConfigOptions();

        Option absoluteValue = OptionBuilder
                .withDescription( "Use the absolute value of the correlation (rarely used)" ).withLongOpt( "abs" )
                .create( 'a' );
        addOption( absoluteValue );

        Option noNegCorr = OptionBuilder.withDescription( "Omit negative correlated probes in link selection" ).create(
                "nonegcorr" );
        addOption( noNegCorr );

        Option useDB = OptionBuilder.withDescription( "Don't save the results in the database (i.e., testing)" )
                .withLongOpt( "nodb" ).create( 'd' );
        addOption( useDB );

        Option fileOpt = OptionBuilder
                .hasArg()
                .withArgName( "Expression data file" )
                .withDescription(
                        "Provide expression data from a tab-delimited text file, rather than from the database. Implies 'nodb' and must also provide 'array' and 't' option" )
                .create( "dataFile" );
        addOption( fileOpt );

        // supply taxon on command line
        Option taxonNameOption = OptionBuilder.hasArg().withDescription(
                "Taxon species name e.g. 'chinook' has to be a species " ).create( "t" );
        addOption( taxonNameOption );

        Option arrayOpt = OptionBuilder.hasArg().withArgName( "Array Design" ).withDescription(
                "Provide the short name of the array design used. Only needed if you are using the 'dataFile' option" )
                .create( "array" );
        addOption( arrayOpt );

        Option textOutOpt = OptionBuilder
                .withDescription(
                        "Output links as text. If multiple experiments are analyzed (e.g. using -f option) "
                                + "results for each are put in a separate file in the current directory with the format {shortname}-links.txt. Otherwise output is to STDOUT" )
                .create( "text" );
        addOption( textOutOpt );

        Option metricOption = OptionBuilder.hasArg().withArgName( "metric" ).withDescription(
                "Similarity metric {pearson|spearman}, default is pearson" ).create( "metric" );
        addOption( metricOption );

        Option imagesOption = OptionBuilder.withDescription( "Suppress the generation of correlation matrix images" )
                .create( "noimages" );
        addOption( imagesOption );

        Option normalizationOption = OptionBuilder
                .hasArg()
                .withArgName( "method" )
                .withDescription(
                        "Normalization method to apply to the data matrix first: SVD, BALANCE, SPELL or omit this option for none (default=none)" )
                .create( "normalizemethod" );
        addOption( normalizationOption );

        Option logTransformOption = OptionBuilder.withDescription(
                "Log-transform the data prior to analysis, if it is not already transformed." ).create( "logtransform" );
        addOption( logTransformOption );

        Option subsetOption = OptionBuilder
                .hasArg()
                .withArgName( "Number of coexpression links to print out" )
                .withDescription(
                        "Only a random subset of total coexpression links will be written to output with approximate "
                                + "size given as the argument; recommended if thresholds are loose to avoid memory problems or gigantic files." )
                .create( "subset" );
        addOption( subsetOption );

        Option chooseCutOption = OptionBuilder
                .hasArg()
                .withArgName( "Singular correlation threshold" )
                .withDescription(
                        "Choose correlation threshold {fwe|cdfCut} to be used independently to select best links, default is none" )
                .create( "choosecut" );
        addOption( chooseCutOption );

        Option probeDegreeThresholdOption = OptionBuilder.hasArg().withArgName( "threshold" ).withDescription(
                "Probes with greater than this number of links will be ignored; default is "
                        + LinkAnalysisConfig.DEFAULT_PROBE_DEGREE_THRESHOLD ).create( "probeDegreeLim" );
        addOption( probeDegreeThresholdOption );

        addForceOption();
        addAutoOption();
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Link Analysis Data Loader", args );
        if ( err != null ) {
            return err;
        }

        this.linkAnalysisService = ( LinkAnalysisService ) this.getBean( "linkAnalysisService" );

        if ( this.dataFileName != null ) {
            /*
             * Read vectors from file. Could provide as a matrix, but it's easier to provide vectors (less mess in later
             * code)
             */

            ArrayDesignService arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );

            ArrayDesign arrayDesign = arrayDesignService.findByShortName( this.linkAnalysisConfig.getArrayName() );

            if ( arrayDesign == null ) {
                return new IllegalArgumentException( "No such array design " + this.linkAnalysisConfig.getArrayName() );
            }

            // this.taxon = arrayDesignService.getTaxon( arrayDesign.getId() );
            this.taxon = taxonService.findByCommonName( analysisTaxon );
            if ( this.taxon == null || !this.taxon.getIsSpecies() ) {
                return new IllegalArgumentException( "No such taxon held in system please check that it is a species: "
                        + taxon );
            }
            log.debug( taxon + "is used" );
            arrayDesign = arrayDesignService.thawLite( arrayDesign );

            Collection<ProcessedExpressionDataVector> dataVectors = new HashSet<ProcessedExpressionDataVector>();

            Map<String, CompositeSequence> csMap = new HashMap<String, CompositeSequence>();
            for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                csMap.put( cs.getName(), cs );
            }

            QuantitationType qtype = makeQuantitationType();

            SimpleExpressionDataLoaderService simpleExpressionDataLoaderService = ( SimpleExpressionDataLoaderService ) this
                    .getBean( "simpleExpressionDataLoaderService" );
            ByteArrayConverter bArrayConverter = new ByteArrayConverter();
            try {
                InputStream data = new FileInputStream( new File( this.dataFileName ) );
                DoubleMatrix<String, String> matrix = simpleExpressionDataLoaderService.parse( data );

                BioAssayDimension bad = makeBioAssayDimension( arrayDesign, matrix );

                for ( int i = 0; i < matrix.rows(); i++ ) {
                    byte[] bdata = bArrayConverter.doubleArrayToBytes( matrix.getRow( i ) );

                    ProcessedExpressionDataVector vector = ProcessedExpressionDataVector.Factory.newInstance();
                    vector.setData( bdata );

                    CompositeSequence cs = csMap.get( matrix.getRowName( i ) );
                    if ( cs == null ) {
                        continue;
                    }
                    vector.setDesignElement( cs );

                    vector.setBioAssayDimension( bad );
                    vector.setQuantitationType( qtype );

                    dataVectors.add( vector );

                }
                log.info( "Read " + dataVectors.size() + " data vectors" );

            } catch ( Exception e ) {
                return e;
            }

            this.linkAnalysisService.process( this.taxon, dataVectors, filterConfig, linkAnalysisConfig );
        } else {

            for ( BioAssaySet ee : expressionExperiments ) {
                if ( ee instanceof ExpressionExperiment ) {
                    processExperiment( ( ExpressionExperiment ) ee );
                } else {
                    throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
                }
            }
            summarizeProcessing();
        }

        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();

        if ( hasOption( "dataFile" ) ) {
            if ( this.expressionExperiments.size() > 0 ) {
                log.error( "The 'dataFile' option is incompatible with other data set selection options" );
                this.bail( ErrorCode.INVALID_OPTION );
            }

            if ( hasOption( "array" ) ) {
                this.linkAnalysisConfig.setArrayName( getOptionValue( "array" ) );
            } else {
                log.error( "Must provide 'array' option if you  use 'dataFile" );
                this.bail( ErrorCode.INVALID_OPTION );
            }

            if ( hasOption( 't' ) ) {
                this.analysisTaxon = this.getOptionValue( 't' );
            } else {
                log
                        .error( "Must provide 'taxon' option if you  use 'dataFile' as RNA taxon may be different to array taxon" );
                this.bail( ErrorCode.INVALID_OPTION );
            }

            this.dataFileName = getOptionValue( "dataFile" );

            this.linkAnalysisConfig.setUseDb( false );
        }

        if ( hasOption( "logTransform" ) ) {
            this.filterConfig.setLogTransform( true );
        }

        if ( hasOption( 'c' ) ) {
            this.linkAnalysisConfig.setCdfCut( Double.parseDouble( getOptionValue( 'c' ) ) );
        }
        if ( hasOption( 'k' ) ) {
            this.linkAnalysisConfig.setCorrelationCacheThreshold( Double.parseDouble( getOptionValue( 'k' ) ) );
        }
        if ( hasOption( 'w' ) ) {
            this.linkAnalysisConfig.setFwe( Double.parseDouble( getOptionValue( 'w' ) ) );
        }

        getFilterConfigOptions();

        if ( hasOption( 'a' ) ) {
            this.linkAnalysisConfig.setAbsoluteValue( true );
        }
        if ( hasOption( 'd' ) ) {
            this.linkAnalysisConfig.setUseDb( false );
        }
        if ( hasOption( "metric" ) ) {
            this.linkAnalysisConfig.setMetric( getOptionValue( "metric" ) );
        }
        if ( hasOption( "text" ) ) {
            this.linkAnalysisConfig.setTextOut( true );
        }

        if ( hasOption( "knownGenesOnly" ) ) {
            linkAnalysisConfig.setKnownGenesOnly( true );
        }
        if ( hasOption( "force" ) ) {
            this.force = true;
        }
        if ( hasOption( "noimages" ) ) {
            linkAnalysisConfig.setMakeSampleCorrMatImages( false );
        }
        if ( hasOption( "nonegcorr" ) ) {
            this.linkAnalysisConfig.setOmitNegLinks( true );
        }

        if ( hasOption( "normalizemethod" ) ) {
            String optionValue = getOptionValue( "normalizemethod" );

            NormalizationMethod value = NormalizationMethod.valueOf( optionValue );
            if ( value == null ) {
                log.error( "No such normalization method: " + value );
                this.bail( ErrorCode.INVALID_OPTION );
            }
            this.linkAnalysisConfig.setNormalizationMethod( value );
        }

        if ( hasOption( "subset" ) ) {
            String subsetSize = getOptionValue( "subset" );
            log.info( "Representative subset of links requested for output" );
            this.linkAnalysisConfig.setSubsetSize( Double.parseDouble( subsetSize ) );
            this.linkAnalysisConfig.setSubset( true );
        }

        if ( hasOption( "choosecut" ) ) {
            String singularThreshold = getOptionValue( "choosecut" );
            if ( singularThreshold.equals( "fwe" ) || singularThreshold.equals( "cdfCut" )
                    || singularThreshold.equals( "none" ) ) {
                log.info( "Singular correlation threshold chosen" );
                this.linkAnalysisConfig.setSingularThreshold( SingularThreshold.valueOf( singularThreshold ) );
            } else {
                log
                        .error( "Must choose 'fwe', 'cdfCut', or 'none' as the singular correlation threshold, defaulting to 'none'" );
            }
        }

        if ( hasOption( "probeDegreeLim" ) ) {
            this.linkAnalysisConfig.setProbeDegreeThreshold( getIntegerOptionValue( "probeDegreeLim" ) );
        }

    }

    @SuppressWarnings("static-access")
    private void buildFilterConfigOptions() {
        Option minPresentFraction = OptionBuilder.hasArg().withArgName( "Missing Value Threshold" ).withDescription(
                "Fraction of data points that must be present in a profile to be retained , default="
                        + FilterConfig.DEFAULT_MINPRESENT_FRACTION ).withLongOpt( "missingcut" ).create( 'm' );
        addOption( minPresentFraction );

        Option lowExpressionCut = OptionBuilder.hasArg().withArgName( "Expression Threshold" ).withDescription(
                "Fraction of expression vectors to reject based on low values, default="
                        + FilterConfig.DEFAULT_LOWEXPRESSIONCUT ).withLongOpt( "lowcut" ).create( 'l' );
        addOption( lowExpressionCut );

        Option lowVarianceCut = OptionBuilder.hasArg().withArgName( "Variance Threshold" ).withDescription(
                "Fraction of expression vectors to reject based on low variance (or coefficient of variation), default="
                        + FilterConfig.DEFAULT_LOWVARIANCECUT ).withLongOpt( "lowvarcut" ).create( "lv" );
        addOption( lowVarianceCut );

        Option knownGenesOnlyOption = OptionBuilder.withDescription(
                "Only save (or print) results for links between 'known genes'" ).create( "knownGenesOnly" );
        addOption( knownGenesOnlyOption );

    }

    private void getFilterConfigOptions() {
        if ( hasOption( 'm' ) ) {
            filterConfig.setMinPresentFraction( Double.parseDouble( getOptionValue( 'm' ) ) );
        }
        if ( hasOption( 'l' ) ) {
            filterConfig.setLowExpressionCut( Double.parseDouble( getOptionValue( 'l' ) ) );
        }
        if ( hasOption( "lv" ) ) {
            filterConfig.setLowVarianceCut( Double.parseDouble( getOptionValue( "lv" ) ) );
        }
    }

    /**
     * @param arrayDesign
     * @param matrix
     * @return
     */
    private BioAssayDimension makeBioAssayDimension( ArrayDesign arrayDesign, DoubleMatrix<String, String> matrix ) {
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setName( "For " + this.dataFileName );
        bad.setDescription( "Generated from flat file" );
        for ( int i = 0; i < matrix.columns(); i++ ) {
            Object columnName = matrix.getColName( i );

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
            bioMaterial.setName( columnName.toString() );
            bioMaterial.setSourceTaxon( taxon );
            Collection<BioMaterial> bioMaterials = new HashSet<BioMaterial>();
            bioMaterials.add( bioMaterial );

            BioAssay assay = BioAssay.Factory.newInstance();
            assay.setName( columnName.toString() );
            assay.setArrayDesignUsed( arrayDesign );
            assay.setSamplesUsed( bioMaterials );
            bad.getBioAssays().add( assay );
        }
        return bad;
    }

    /**
     * @return
     */
    private QuantitationType makeQuantitationType() {
        QuantitationType qtype = QuantitationType.Factory.newInstance();
        qtype.setName( "Dummy" );
        qtype.setGeneralType( GeneralType.QUANTITATIVE );
        qtype.setRepresentation( PrimitiveType.DOUBLE ); // no choice here
        qtype.setIsPreferred( Boolean.TRUE );
        qtype.setIsNormalized( Boolean.TRUE );
        qtype.setIsBackgroundSubtracted( Boolean.TRUE );
        qtype.setIsBackground( false );
        qtype.setType( StandardQuantitationType.AMOUNT );// this shouldn't get used, just filled in to keep everybody
        // happy.
        qtype.setIsMaskedPreferred( true );

        qtype.setScale( ScaleType.OTHER );// this shouldn't get used, just filled in to keep everybody happy.
        qtype.setIsRatio( false ); // this shouldn't get used, just filled in to keep everybody happy.
        return qtype;
    }

    /**
     * @param ee
     */
    private void processExperiment( ExpressionExperiment ee ) {
        ee = eeService.thawLite( ee );

        /*
         * If we're not using the database, always run it.
         */
        if ( linkAnalysisConfig.isUseDb() && !force && !needToRun( ee, LinkAnalysisEvent.class ) ) {
            log.info( "Can't or Don't need to run " + ee );
            return;
        }

        /*
         * Note that auditing is handled by the service.
         */
        try {

            if ( this.expressionExperiments.size() > 1 && linkAnalysisConfig.isTextOut() ) {
                linkAnalysisConfig
                        .setOutputFile( new File( ee.getShortName().replaceAll( "\\s", "_" ) + "-links.txt" ) );
            }

            linkAnalysisService.process( ee, filterConfig, linkAnalysisConfig );
            successObjects.add( ee.toString() );
        } catch ( Exception e ) {
            errorObjects.add( ee + ": " + e.getMessage() );
            log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
            log.error( e, e );
        }
    }

}
