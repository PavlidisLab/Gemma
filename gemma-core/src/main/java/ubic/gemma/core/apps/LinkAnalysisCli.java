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
package ubic.gemma.core.apps;

import org.apache.commons.cli.Option;
import org.apache.commons.lang3.time.StopWatch;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.util.FileTools;
import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisConfig;
import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisConfig.NormalizationMethod;
import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisConfig.SingularThreshold;
import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisPersister;
import ubic.gemma.core.analysis.expression.coexpression.links.LinkAnalysisService;
import ubic.gemma.core.analysis.preprocess.filter.FilterConfig;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.auditAndSecurity.eventType.LinkAnalysisEvent;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.association.coexpression.CoexpressionService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Commandline tool to conduct link (coexpression) analysis.
 *
 * @author xiangwan
 * @author paul (refactoring)
 * @author vaneet
 */
public class LinkAnalysisCli extends ExpressionExperimentManipulatingCLI {

    private final FilterConfig filterConfig = new FilterConfig();
    private final LinkAnalysisConfig linkAnalysisConfig = new LinkAnalysisConfig();
    private String analysisTaxon = null;
    private String dataFileName = null;
    private LinkAnalysisService linkAnalysisService;
    private boolean initializeFromOldData = false;
    private boolean updateNodeDegree = false;
    private boolean deleteAnalyses = false;

    public static void main( String[] args ) {
        LinkAnalysisCli p = new LinkAnalysisCli();
        executeCommand( p, args );
    }

    @Override
    public String getCommandName() {
        return "coexpAnalyze";
    }

    @Override
    protected Exception doWork( String[] args ) {
        Exception err = this.processCommandLine( args );
        if ( err != null ) {
            return err;
        }

        if ( initializeFromOldData ) {
            AbstractCLI.log.info( "Initializing links from old data for " + this.getTaxon() );
            LinkAnalysisPersister s = this.getBean( LinkAnalysisPersister.class );
            s.initializeLinksFromOldData( this.getTaxon() );
            return null;
        } else if ( updateNodeDegree ) {

            // we waste some time here getting the experiments.
            this.loadTaxon();

            this.getBean( CoexpressionService.class ).updateNodeDegrees( this.getTaxon() );

            return null;
        }

        this.linkAnalysisService = this.getBean( LinkAnalysisService.class );

        if ( this.dataFileName != null ) {
            /*
             * Read vectors from file. Could provide as a matrix, but it's easier to provide vectors (less mess in later
             * code)
             */
            ArrayDesignService arrayDesignService = this.getBean( ArrayDesignService.class );

            ArrayDesign arrayDesign = arrayDesignService.findByShortName( this.linkAnalysisConfig.getArrayName() );

            if ( arrayDesign == null ) {
                return new IllegalArgumentException( "No such array design " + this.linkAnalysisConfig.getArrayName() );
            }

            this.loadTaxon();
            arrayDesign = arrayDesignService.thawLite( arrayDesign );

            Collection<ProcessedExpressionDataVector> dataVectors = new HashSet<>();

            Map<String, CompositeSequence> csMap = new HashMap<>();
            for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                csMap.put( cs.getName(), cs );
            }

            QuantitationType qtype = this.makeQuantitationType();

            SimpleExpressionDataLoaderService simpleExpressionDataLoaderService = this
                    .getBean( SimpleExpressionDataLoaderService.class );
            ByteArrayConverter bArrayConverter = new ByteArrayConverter();
            try (InputStream data = new FileInputStream( new File( this.dataFileName ) )) {

                DoubleMatrix<String, String> matrix = simpleExpressionDataLoaderService.parse( data );

                BioAssayDimension bad = this.makeBioAssayDimension( arrayDesign, matrix );

                for ( int i = 0; i < matrix.rows(); i++ ) {
                    byte[] bData = bArrayConverter.doubleArrayToBytes( matrix.getRow( i ) );

                    ProcessedExpressionDataVector vector = ProcessedExpressionDataVector.Factory.newInstance();
                    vector.setData( bData );

                    CompositeSequence cs = csMap.get( matrix.getRowName( i ) );
                    if ( cs == null ) {
                        continue;
                    }
                    vector.setDesignElement( cs );

                    vector.setBioAssayDimension( bad );
                    vector.setQuantitationType( qtype );

                    dataVectors.add( vector );

                }
                AbstractCLI.log.info( "Read " + dataVectors.size() + " data vectors" );

            } catch ( Exception e ) {
                return e;
            }

            this.linkAnalysisService.processVectors( this.getTaxon(), dataVectors, filterConfig, linkAnalysisConfig );
        } else {

            /*
             * Do in decreasing order of size, to help capture more links earlier - reduces fragmentation.
             */
            List<BioAssaySet> sees = new ArrayList<>( expressionExperiments );

            if ( expressionExperiments.size() > 1 ) {
                AbstractCLI.log.info( "Sorting data sets by number of samples, doing large data sets first." );

                Collection<ExpressionExperimentValueObject> vos = eeService
                        .loadValueObjects( EntityUtils.getIds( expressionExperiments ), true );
                final Map<Long, ExpressionExperimentValueObject> idMap = EntityUtils.getIdMap( vos );

                Collections.sort( sees, new Comparator<BioAssaySet>() {
                    @Override
                    public int compare( BioAssaySet o1, BioAssaySet o2 ) {

                        ExpressionExperimentValueObject e1 = idMap.get( o1.getId() );
                        ExpressionExperimentValueObject e2 = idMap.get( o2.getId() );

                        assert e1 != null : "No valueobject: " + e2;
                        assert e2 != null : "No valueobject: " + e1;

                        return -e1.getBioMaterialCount().compareTo( e2.getBioMaterialCount() );

                    }
                } );
            }

            for ( BioAssaySet ee : sees ) {
                if ( ee instanceof ExpressionExperiment ) {
                    this.processExperiment( ( ExpressionExperiment ) ee );
                } else {
                    throw new UnsupportedOperationException( "Can't handle non-EE BioAssaySets yet" );
                }
            }
            this.summarizeProcessing();
        }

        return null;
    }

    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for coexpressed genes";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        super.buildOptions();

        super.addDateOption();

        Option nodeDegreeUpdate = Option.builder( "n" ).desc( "Update the node degree for taxon given by -t option. All other options ignored." )
                .build();
        this.addOption( nodeDegreeUpdate );

        super.addOption( "init", "Initialize links for taxon given by -t option, based on old data. All other options ignored." );

        Option cdfCut = Option.builder( "c" ).hasArg().argName( "Tolerance Threshold" )
                .desc( "The tolerance threshold for coefficient value" ).longOpt( "cdfcut" )
                .build();
        this.addOption( cdfCut );

        Option tooSmallToKeep = Option.builder( "k" ).hasArg().argName( "Cache Threshold" )
                .desc( "The threshold for coefficient cache" ).longOpt( "cachecut" ).build();
        this.addOption( tooSmallToKeep );

        Option fwe = Option.builder( "w" ).hasArg().argName( "Family Wise Error Rate" )
                .desc( "The setting for family wise error control" ).longOpt( "fwe" ).build();
        this.addOption( fwe );

        this.buildFilterConfigOptions();

        Option absoluteValue = Option.builder( "a" )
                .desc( "Use the absolute value of the correlation (rarely used)" ).longOpt( "abs" )
                .build();
        this.addOption( absoluteValue );

        Option noNegCorr = Option.builder( "nonegcorr" ).desc( "Omit negative correlated probes in link selection" )
                .build();
        this.addOption( noNegCorr );

        Option useDB = Option.builder( "d" ).desc( "Don't save the results in the database (i.e., testing)" )
                .longOpt( "nodb" ).build();
        this.addOption( useDB );

        Option fileOpt = Option.builder( "dataFile" ).hasArg().argName( "Expression data file" ).desc(
                "Provide expression data from a tab-delimited text file, rather than from the database. Implies 'nodb' and must also provide 'array' and 't' option" )
                .build();
        this.addOption( fileOpt );

        // supply taxon on command line
        Option taxonNameOption = Option.builder( "t" ).hasArg().desc( "Taxon species name e.g. 'mouse'" )
                .build();
        this.addOption( taxonNameOption );

        Option arrayOpt = Option.builder( "array" ).hasArg().argName( "Array Design" ).desc(
                "Provide the short name of the array design used. Only needed if you are using the 'dataFile' option" )
                .build();
        this.addOption( arrayOpt );

        Option textOutOpt = Option.builder( "text" ).desc(
                "Output links as text. If multiple experiments are analyzed (e.g. using -f option) "
                        + "results for each are put in a separate file in the current directory with the format {shortname}-links.txt. Otherwise output is to STDOUT" )
                .build();
        this.addOption( textOutOpt );

        Option metricOption = Option.builder( "metric" ).hasArg().argName( "metric" )
                .desc( "Similarity metric {pearson|spearman}, default is pearson" ).build();
        this.addOption( metricOption );

        Option imagesOption = Option.builder( "noimages" ).desc( "Suppress the generation of correlation matrix images" )
                .build();
        this.addOption( imagesOption );

        Option normalizationOption = Option.builder( "normalizemethod" ).hasArg().argName( "method" ).desc(
                "Normalization method to apply to the data matrix first: SVD, BALANCE, SPELL or omit this option for none (default=none)" )
                .build();
        this.addOption( normalizationOption );

        Option logTransformOption = Option.builder( "logtransform" )
                .desc( "Log-transform the data prior to analysis, if it is not already transformed." )
                .build();
        this.addOption( logTransformOption );

        Option subsetOption = Option.builder( "subset" ).hasArg().argName( "Number of coexpression links to print out" )
                .desc(
                        "Only a random subset of total coexpression links will be written to output with approximate "
                                + "size given as the argument; recommended if thresholds are loose to avoid memory problems or gigantic files." )
                .build();
        this.addOption( subsetOption );

        Option chooseCutOption = Option.builder( "choosecut" ).hasArg().argName( "Singular correlation threshold" ).desc(
                "Choose correlation threshold {fwe|cdfCut} to be used independently to select best links, default is none" )
                .build();
        this.addOption( chooseCutOption );

        // finer-grained control is possible, of course.
        Option skipQC = Option.builder( "noqc" )
                .desc( "Skip strict QC for outliers, batch effects and correlation distribution" )
                .build();
        this.addOption( skipQC );

        Option deleteOption = Option.builder( "delete" ).desc(
                "Delete analyses for selected experiments, instead of doing analysis; supersedes all other options" )
                .build();
        this.addOption( deleteOption );

        this.addForceOption();
        this.addAutoOption();
    }

    @Override
    protected void processOptions() {
        this.autoSeekEventType = LinkAnalysisEvent.class;
        super.processOptions();

        if ( this.hasOption( "delete" ) ) {
            this.deleteAnalyses = true;
            this.force = true;
            return;
        } else if ( this.hasOption( "init" ) ) {
            initializeFromOldData = true;
            if ( this.hasOption( 't' ) ) {
                this.analysisTaxon = this.getOptionValue( 't' );
            } else {
                AbstractCLI.log.error( "Must provide 'taxon' option when initializing from old data" );
                exitwithError();
            }
            // all other options ignored.
            return;
        } else if ( this.hasOption( 'n' ) ) {
            this.updateNodeDegree = true;
            if ( this.hasOption( 't' ) ) {
                this.analysisTaxon = this.getOptionValue( 't' );
            } else {
                AbstractCLI.log.error( "Must provide 'taxon' option when updating node degree" );
                exitwithError();
            }
            // all other options ignored.
            return;
        }

        if ( this.hasOption( "dataFile" ) ) {
            if ( this.expressionExperiments.size() > 0 ) {
                AbstractCLI.log.error( "The 'dataFile' option is incompatible with other data set selection options" );
                exitwithError();
            }

            if ( this.hasOption( "array" ) ) {
                this.linkAnalysisConfig.setArrayName( this.getOptionValue( "array" ) );
            } else {
                AbstractCLI.log.error( "Must provide 'array' option if you  use 'dataFile" );
                exitwithError();
            }

            if ( this.hasOption( 't' ) ) {
                this.analysisTaxon = this.getOptionValue( 't' );
            } else {
                AbstractCLI.log
                        .error( "Must provide 'taxon' option if you  use 'dataFile' as RNA taxon may be different to array taxon" );
                exitwithError();
            }

            this.dataFileName = this.getOptionValue( "dataFile" );

            this.linkAnalysisConfig.setUseDb( false );
        }

        if ( this.hasOption( "logTransform" ) ) {
            this.filterConfig.setLogTransform( true );
        }

        if ( this.hasOption( 'c' ) ) {
            this.linkAnalysisConfig.setCdfCut( Double.parseDouble( this.getOptionValue( 'c' ) ) );
        }
        if ( this.hasOption( 'k' ) ) {
            this.linkAnalysisConfig.setCorrelationCacheThreshold( Double.parseDouble( this.getOptionValue( 'k' ) ) );
        }
        if ( this.hasOption( 'w' ) ) {
            this.linkAnalysisConfig.setFwe( Double.parseDouble( this.getOptionValue( 'w' ) ) );
        }

        if ( this.hasOption( "noqc" ) ) {
            this.linkAnalysisConfig.setCheckCorrelationDistribution( false );
            this.linkAnalysisConfig.setCheckForBatchEffect( false );
            this.linkAnalysisConfig.setCheckForOutliers( false );
        }

        this.getFilterConfigOptions();

        if ( this.hasOption( 'a' ) ) {
            this.linkAnalysisConfig.setAbsoluteValue( true );
        }
        if ( this.hasOption( 'd' ) ) {
            this.linkAnalysisConfig.setUseDb( false );
        }
        if ( this.hasOption( "metric" ) ) {
            this.linkAnalysisConfig.setMetric( this.getOptionValue( "metric" ) );
        }
        if ( this.hasOption( "text" ) ) {
            this.linkAnalysisConfig.setTextOut( true );
        }

        if ( this.hasOption( "noimages" ) ) {
            linkAnalysisConfig.setMakeSampleCorrMatImages( false );
        }
        if ( this.hasOption( "nonegcorr" ) ) {
            this.linkAnalysisConfig.setOmitNegLinks( true );
        }

        if ( this.hasOption( "normalizemethod" ) ) {
            String optionValue = this.getOptionValue( "normalizemethod" );

            NormalizationMethod value = NormalizationMethod.valueOf( optionValue );
            this.linkAnalysisConfig.setNormalizationMethod( value );
        }

        if ( this.hasOption( "subset" ) ) {
            String subsetSize = this.getOptionValue( "subset" );
            AbstractCLI.log.info( "Representative subset of links requested for output" );
            this.linkAnalysisConfig.setSubsetSize( Double.parseDouble( subsetSize ) );
            this.linkAnalysisConfig.setSubset( true );
        }

        if ( this.hasOption( "choosecut" ) ) {
            String singularThreshold = this.getOptionValue( "choosecut" );
            if ( singularThreshold.equals( "fwe" ) || singularThreshold.equals( "cdfCut" ) || singularThreshold
                    .equals( "none" ) ) {
                AbstractCLI.log.info( "Singular correlation threshold chosen" );
                this.linkAnalysisConfig.setSingularThreshold( SingularThreshold.valueOf( singularThreshold ) );
            } else {
                AbstractCLI.log
                        .error( "Must choose 'fwe', 'cdfCut', or 'none' as the singular correlation threshold, defaulting to 'none'" );
            }
        }

        if ( this.hasOption( "probeDegreeLim" ) ) {
            this.linkAnalysisConfig.setProbeDegreeThreshold( this.getIntegerOptionValue( "probeDegreeLim" ) );
        }

    }

    @SuppressWarnings("static-access")
    private void buildFilterConfigOptions() {
        Option minPresentFraction = Option.builder( "m" ).hasArg().argName( "Missing Value Threshold" ).desc(
                "Fraction of data points that must be present in a profile to be retained , default="
                        + FilterConfig.DEFAULT_MINPRESENT_FRACTION )
                .longOpt( "missingcut" ).build();
        this.addOption( minPresentFraction );

        Option lowExpressionCut = Option.builder( "l" ).hasArg().argName( "Expression Threshold" ).desc(
                "Fraction of expression vectors to reject based on low values, default="
                        + FilterConfig.DEFAULT_LOWEXPRESSIONCUT )
                .longOpt( "lowcut" ).build();
        this.addOption( lowExpressionCut );

        Option lowVarianceCut = Option.builder( "lv" ).hasArg().argName( "Variance Threshold" ).desc(
                "Fraction of expression vectors to reject based on low variance (or coefficient of variation), default="
                        + FilterConfig.DEFAULT_LOWVARIANCECUT )
                .longOpt( "lowvarcut" ).build();
        this.addOption( lowVarianceCut );

        Option distinctValueCut = Option.builder( "dv" ).hasArg().argName( "Fraction distinct values threshold" )
                .desc( "Fraction of values which must be distinct (NaN counts as one value), default="
                        + FilterConfig.DEFAULT_DISTINCTVALUE_FRACTION )
                .longOpt( "distinctValCut" ).build();
        this.addOption( distinctValueCut );

    }

    private void getFilterConfigOptions() {
        if ( this.hasOption( 'm' ) ) {
            filterConfig.setMinPresentFraction( Double.parseDouble( this.getOptionValue( 'm' ) ) );
        }
        if ( this.hasOption( 'l' ) ) {
            filterConfig.setLowExpressionCut( Double.parseDouble( this.getOptionValue( 'l' ) ) );
        }
        if ( this.hasOption( "lv" ) ) {
            filterConfig.setLowVarianceCut( Double.parseDouble( this.getOptionValue( "lv" ) ) );
        }
        if ( this.hasOption( "dv" ) ) {
            filterConfig.setLowDistinctValueCut( Double.parseDouble( this.getOptionValue( "dv" ) ) );
        }
    }

    private void loadTaxon() {
        super.setTaxon( this.getTaxonService().findByCommonName( analysisTaxon ) );
        if ( this.getTaxon() == null || !this.getTaxon().getIsGenesUsable() ) {
            throw new IllegalArgumentException( "No such taxon or, does not have usable gene information: " + getTaxon() );
        }
        AbstractCLI.log.debug( getTaxon() + "is used" );
    }

    private BioAssayDimension makeBioAssayDimension( ArrayDesign arrayDesign, DoubleMatrix<String, String> matrix ) {
        BioAssayDimension bad = BioAssayDimension.Factory.newInstance();
        bad.setName( "For " + this.dataFileName );
        bad.setDescription( "Generated from flat file" );
        for ( int i = 0; i < matrix.columns(); i++ ) {
            Object columnName = matrix.getColName( i );

            BioMaterial bioMaterial = BioMaterial.Factory.newInstance();
            bioMaterial.setName( columnName.toString() );
            bioMaterial.setSourceTaxon( getTaxon() );

            BioAssay assay = BioAssay.Factory.newInstance();
            assay.setName( columnName.toString() );
            assay.setArrayDesignUsed( arrayDesign );
            assay.setSampleUsed( bioMaterial );
            assay.setIsOutlier( false );
            assay.setSequencePairedReads( false );
            bad.getBioAssays().add( assay );
        }
        return bad;
    }

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

    private void processExperiment( ExpressionExperiment ee ) {
        ee = eeService.thaw( ee );

        if ( this.deleteAnalyses ) {
            AbstractCLI.log.info( "======= Deleting coexpression analysis (if any) for: " + ee );
            if ( this.getBean( LinkAnalysisPersister.class ).deleteAnalyses( ee ) ) {
                successObjects.add( ee.toString() );
            } else {
                errorObjects.add( ee + ": Seems to not have any eligible link analysis to remove" );
            }
            return;
        }

        /*
         * If we're not using the database, always run it.
         */
        if ( linkAnalysisConfig.isUseDb() && !force && this.noNeedToRun( ee, LinkAnalysisEvent.class ) ) {
            AbstractCLI.log.info( "Can't or Don't need to run " + ee );
            return;
        }

        /*
         * Note that auditing is handled by the service.
         */
        StopWatch sw = new StopWatch();
        sw.start();
        try {

            if ( this.expressionExperiments.size() > 1 && linkAnalysisConfig.isTextOut() ) {
                linkAnalysisConfig
                        .setOutputFile( new File( FileTools.cleanForFileName( ee.getShortName() ) + "-links.txt" ) );
            }

            AbstractCLI.log.info( "==== Starting: [" + ee.getShortName() + "] ======" );

            linkAnalysisService.process( ee, filterConfig, linkAnalysisConfig );

            successObjects.add( ee.toString() );
        } catch ( Exception e ) {
            errorObjects.add( ee + ": " + e.getMessage() );
            AbstractCLI.log.error( "**** Exception while processing " + ee + ": " + e.getMessage() + " ********" );
            AbstractCLI.log.error( e, e );
        }
        AbstractCLI.log.info( "==== Done: [" + ee.getShortName() + "] ======" );
        AbstractCLI.log.info( "Time elapsed: " + String.format( "%.2f", sw.getTime() / 1000.0 / 60.0 ) + " minutes" );

    }

}
