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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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

    @Override
    public String getCommandName() {
        return "coexpAnalyze";
    }

    @Override
    protected void doWork() throws Exception {
        if ( initializeFromOldData ) {
            AbstractCLI.log.info( "Initializing links from old data for " + this.getTaxon() );
            LinkAnalysisPersister s = this.getBean( LinkAnalysisPersister.class );
            s.initializeLinksFromOldData( this.getTaxon() );
            return;
        } else if ( updateNodeDegree ) {

            // we waste some time here getting the experiments.
            this.loadTaxon();

            this.getBean( CoexpressionService.class ).updateNodeDegrees( this.getTaxon() );
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
                throw new IllegalArgumentException( "No such array design " + this.linkAnalysisConfig.getArrayName() );
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
            try ( InputStream data = new FileInputStream( new File( this.dataFileName ) ) ) {

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
                throw e;
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
                        .loadValueObjectsByIds( EntityUtils.getIds( expressionExperiments ), true );
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
        }
    }

    @Override
    public String getShortDesc() {
        return "Analyze expression data sets for coexpressed genes";
    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions( Options options ) {
        super.buildOptions( options );

        super.addDateOption( options );

        Option nodeDegreeUpdate = Option.builder( "n" ).desc( "Update the node degree for taxon given by -t option. All other options ignored." )
                .build();
        options.addOption( nodeDegreeUpdate );

        options.addOption( "init", "Initialize links for taxon given by -t option, based on old data. All other options ignored." );

        Option cdfCut = Option.builder( "c" ).hasArg().argName( "Tolerance Threshold" )
                .desc( "The tolerance threshold for coefficient value" ).longOpt( "cdfcut" )
                .build();
        options.addOption( cdfCut );

        Option tooSmallToKeep = Option.builder( "k" ).hasArg().argName( "Cache Threshold" )
                .desc( "The threshold for coefficient cache" ).longOpt( "cachecut" ).build();
        options.addOption( tooSmallToKeep );

        Option fwe = Option.builder( "w" ).hasArg().argName( "Family Wise Error Rate" )
                .desc( "The setting for family wise error control" ).longOpt( "fwe" ).build();
        options.addOption( fwe );

        this.buildFilterConfigOptions( options );

        Option absoluteValue = Option.builder( "a" )
                .desc( "Use the absolute value of the correlation (rarely used)" ).longOpt( "abs" )
                .build();
        options.addOption( absoluteValue );

        Option noNegCorr = Option.builder( "nonegcorr" ).desc( "Omit negative correlated probes in link selection" )
                .build();
        options.addOption( noNegCorr );

        Option useDB = Option.builder( "d" ).desc( "Don't save the results in the database (i.e., testing)" )
                .longOpt( "nodb" ).build();
        options.addOption( useDB );

        Option fileOpt = Option.builder( "dataFile" ).hasArg().argName( "Expression data file" ).desc(
                        "Provide expression data from a tab-delimited text file, rather than from the database. Implies 'nodb' and must also provide 'array' and 't' option" )
                .build();
        options.addOption( fileOpt );

        // supply taxon on command line
        Option taxonNameOption = Option.builder( "t" ).hasArg().desc( "Taxon species name e.g. 'mouse'" )
                .build();
        options.addOption( taxonNameOption );

        Option arrayOpt = Option.builder( "array" ).hasArg().argName( "Array Design" ).desc(
                        "Provide the short name of the array design used. Only needed if you are using the 'dataFile' option" )
                .build();
        options.addOption( arrayOpt );

        Option textOutOpt = Option.builder( "text" ).desc(
                        "Output links as text. If multiple experiments are analyzed (e.g. using -f option) "
                                + "results for each are put in a separate file in the current directory with the format {shortname}-links.txt. Otherwise output is to STDOUT" )
                .build();
        options.addOption( textOutOpt );

        Option metricOption = Option.builder( "metric" ).hasArg().argName( "metric" )
                .desc( "Similarity metric {pearson|spearman}, default is pearson" ).build();
        options.addOption( metricOption );

        Option imagesOption = Option.builder( "noimages" ).desc( "Suppress the generation of correlation matrix images" )
                .build();
        options.addOption( imagesOption );

        Option normalizationOption = Option.builder( "normalizemethod" ).hasArg().argName( "method" ).desc(
                        "Normalization method to apply to the data matrix first: SVD, BALANCE, SPELL or omit this option for none (default=none)" )
                .build();
        options.addOption( normalizationOption );

        Option logTransformOption = Option.builder( "logtransform" )
                .desc( "Log-transform the data prior to analysis, if it is not already transformed." )
                .build();
        options.addOption( logTransformOption );

        Option subsetOption = Option.builder( "subset" ).hasArg().argName( "Number of coexpression links to print out" )
                .desc(
                        "Only a random subset of total coexpression links will be written to output with approximate "
                                + "size given as the argument; recommended if thresholds are loose to avoid memory problems or gigantic files." )
                .build();
        options.addOption( subsetOption );

        Option chooseCutOption = Option.builder( "choosecut" ).hasArg().argName( "Singular correlation threshold" ).desc(
                        "Choose correlation threshold {fwe|cdfCut} to be used independently to select best links, default is none" )
                .build();
        options.addOption( chooseCutOption );

        // finer-grained control is possible, of course.
        Option skipQC = Option.builder( "noqc" )
                .desc( "Skip strict QC for outliers, batch effects and correlation distribution" )
                .build();
        options.addOption( skipQC );

        Option deleteOption = Option.builder( "delete" ).desc(
                        "Delete analyses for selected experiments, instead of doing analysis; supersedes all other options" )
                .build();
        options.addOption( deleteOption );

        this.addForceOption( options );
        this.addAutoOption( options, LinkAnalysisEvent.class );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        super.processOptions( commandLine );

        if ( commandLine.hasOption( "delete" ) ) {
            this.deleteAnalyses = true;
            this.force = true;
            return;
        } else if ( commandLine.hasOption( "init" ) ) {
            initializeFromOldData = true;
            if ( commandLine.hasOption( 't' ) ) {
                this.analysisTaxon = commandLine.getOptionValue( 't' );
            } else {
                throw new RuntimeException( "Must provide 'taxon' option when initializing from old data" );
            }
            // all other options ignored.
            return;
        } else if ( commandLine.hasOption( 'n' ) ) {
            this.updateNodeDegree = true;
            if ( commandLine.hasOption( 't' ) ) {
                this.analysisTaxon = commandLine.getOptionValue( 't' );
            } else {
                throw new RuntimeException( "Must provide 'taxon' option when updating node degree" );
            }
            // all other options ignored.
            return;
        }

        if ( commandLine.hasOption( "dataFile" ) ) {
            if ( this.expressionExperiments.size() > 0 ) {
                throw new RuntimeException( "The 'dataFile' option is incompatible with other data set selection options" );
            }

            if ( commandLine.hasOption( "array" ) ) {
                this.linkAnalysisConfig.setArrayName( commandLine.getOptionValue( "array" ) );
            } else {
                throw new RuntimeException( "Must provide 'array' option if you  use 'dataFile" );
            }

            if ( commandLine.hasOption( 't' ) ) {
                this.analysisTaxon = commandLine.getOptionValue( 't' );
            } else {
                throw new RuntimeException( "Must provide 'taxon' option if you  use 'dataFile' as RNA taxon may be different to array taxon" );
            }

            this.dataFileName = commandLine.getOptionValue( "dataFile" );

            this.linkAnalysisConfig.setUseDb( false );
        }

        if ( commandLine.hasOption( "logTransform" ) ) {
            this.filterConfig.setLogTransform( true );
        }

        if ( commandLine.hasOption( 'c' ) ) {
            this.linkAnalysisConfig.setCdfCut( Double.parseDouble( commandLine.getOptionValue( 'c' ) ) );
        }
        if ( commandLine.hasOption( 'k' ) ) {
            this.linkAnalysisConfig.setCorrelationCacheThreshold( Double.parseDouble( commandLine.getOptionValue( 'k' ) ) );
        }
        if ( commandLine.hasOption( 'w' ) ) {
            this.linkAnalysisConfig.setFwe( Double.parseDouble( commandLine.getOptionValue( 'w' ) ) );
        }

        if ( commandLine.hasOption( "noqc" ) ) {
            this.linkAnalysisConfig.setCheckCorrelationDistribution( false );
            this.linkAnalysisConfig.setCheckForBatchEffect( false );
            this.linkAnalysisConfig.setCheckForOutliers( false );
        }

        this.getFilterConfigOptions( commandLine );

        if ( commandLine.hasOption( 'a' ) ) {
            this.linkAnalysisConfig.setAbsoluteValue( true );
        }
        if ( commandLine.hasOption( 'd' ) ) {
            this.linkAnalysisConfig.setUseDb( false );
        }
        if ( commandLine.hasOption( "metric" ) ) {
            this.linkAnalysisConfig.setMetric( commandLine.getOptionValue( "metric" ) );
        }
        if ( commandLine.hasOption( "text" ) ) {
            this.linkAnalysisConfig.setTextOut( true );
        }

        if ( commandLine.hasOption( "noimages" ) ) {
            linkAnalysisConfig.setMakeSampleCorrMatImages( false );
        }
        if ( commandLine.hasOption( "nonegcorr" ) ) {
            this.linkAnalysisConfig.setOmitNegLinks( true );
        }

        if ( commandLine.hasOption( "normalizemethod" ) ) {
            String optionValue = commandLine.getOptionValue( "normalizemethod" );

            NormalizationMethod value = NormalizationMethod.valueOf( optionValue );
            this.linkAnalysisConfig.setNormalizationMethod( value );
        }

        if ( commandLine.hasOption( "subset" ) ) {
            String subsetSize = commandLine.getOptionValue( "subset" );
            AbstractCLI.log.info( "Representative subset of links requested for output" );
            this.linkAnalysisConfig.setSubsetSize( Double.parseDouble( subsetSize ) );
            this.linkAnalysisConfig.setSubset( true );
        }

        if ( commandLine.hasOption( "choosecut" ) ) {
            String singularThreshold = commandLine.getOptionValue( "choosecut" );
            if ( singularThreshold.equals( "fwe" ) || singularThreshold.equals( "cdfCut" ) || singularThreshold
                    .equals( "none" ) ) {
                AbstractCLI.log.info( "Singular correlation threshold chosen" );
                this.linkAnalysisConfig.setSingularThreshold( SingularThreshold.valueOf( singularThreshold ) );
            } else {
                AbstractCLI.log
                        .error( "Must choose 'fwe', 'cdfCut', or 'none' as the singular correlation threshold, defaulting to 'none'" );
            }
        }

        if ( commandLine.hasOption( "probeDegreeLim" ) ) {
            int result;
            try {
                result = Integer.parseInt( commandLine.getOptionValue( "probeDegreeLim" ) );
            } catch ( NumberFormatException e ) {
                throw new RuntimeException( "Invalid value '" + commandLine.getOptionValue( "probeDegreeLim" ) + " for option " + "probeDegreeLim" + ", not a valid integer", e );
            }
            this.linkAnalysisConfig.setProbeDegreeThreshold( result );
        }

    }

    @SuppressWarnings("static-access")
    private void buildFilterConfigOptions( Options options ) {
        Option minPresentFraction = Option.builder( "m" ).hasArg().argName( "Missing Value Threshold" ).desc(
                        "Fraction of data points that must be present in a profile to be retained , default="
                                + FilterConfig.DEFAULT_MINPRESENT_FRACTION )
                .longOpt( "missingcut" ).build();
        options.addOption( minPresentFraction );

        Option lowExpressionCut = Option.builder( "l" ).hasArg().argName( "Expression Threshold" ).desc(
                        "Fraction of expression vectors to reject based on low values, default="
                                + FilterConfig.DEFAULT_LOWEXPRESSIONCUT )
                .longOpt( "lowcut" ).build();
        options.addOption( lowExpressionCut );

        Option lowVarianceCut = Option.builder( "lv" ).hasArg().argName( "Variance Threshold" ).desc(
                        "Fraction of expression vectors to reject based on low variance (or coefficient of variation), default="
                                + FilterConfig.DEFAULT_LOWVARIANCECUT )
                .longOpt( "lowvarcut" ).build();
        options.addOption( lowVarianceCut );

        Option distinctValueCut = Option.builder( "dv" ).hasArg().argName( "Fraction distinct values threshold" )
                .desc( "Fraction of values which must be distinct (NaN counts as one value), default="
                        + FilterConfig.DEFAULT_DISTINCTVALUE_FRACTION )
                .longOpt( "distinctValCut" ).build();
        options.addOption( distinctValueCut );

    }

    private void getFilterConfigOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'm' ) ) {
            filterConfig.setMinPresentFraction( Double.parseDouble( commandLine.getOptionValue( 'm' ) ) );
        }
        if ( commandLine.hasOption( 'l' ) ) {
            filterConfig.setLowExpressionCut( Double.parseDouble( commandLine.getOptionValue( 'l' ) ) );
        }
        if ( commandLine.hasOption( "lv" ) ) {
            filterConfig.setLowVarianceCut( Double.parseDouble( commandLine.getOptionValue( "lv" ) ) );
        }
        if ( commandLine.hasOption( "dv" ) ) {
            filterConfig.setLowDistinctValueCut( Double.parseDouble( commandLine.getOptionValue( "dv" ) ) );
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
                addSuccessObject( ee );
            } else {
                addErrorObject( ee, "Seems to not have any eligible link analysis to remove" );
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

            addSuccessObject( ee );
        } catch ( Exception e ) {
            addErrorObject( ee, e );
        }
        AbstractCLI.log.info( "==== Done: [" + ee.getShortName() + "] ======" );
        AbstractCLI.log.info( "Time elapsed: " + String.format( "%.2f", sw.getTime() / 1000.0 / 60.0 ) + " minutes" );

    }

}
