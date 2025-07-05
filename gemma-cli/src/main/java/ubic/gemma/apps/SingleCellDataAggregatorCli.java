package ubic.gemma.apps;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.preprocess.PreprocessorService;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.singleCell.aggregate.AggregateConfig;
import ubic.gemma.core.analysis.singleCell.aggregate.SingleCellExpressionExperimentSplitAndAggregateService;
import ubic.gemma.core.analysis.singleCell.aggregate.SplitConfig;
import ubic.gemma.core.analysis.singleCell.aggregate.UnsupportedScaleTypeForAggregationException;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.experiment.SingleCellExpressionExperimentService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static ubic.gemma.cli.util.OptionsUtils.*;
import static ubic.gemma.core.analysis.singleCell.aggregate.CellLevelCharacteristicsMappingUtils.*;

@CommonsLog
public class SingleCellDataAggregatorCli extends ExpressionExperimentVectorsManipulatingCli<SingleCellExpressionDataVector> {

    private static final String
            CTA_OPTION = "cta",
            CLC_OPTION = "clc",
            MASK_OPTION = "mask",
            NO_MASK_OPTION = "noMask",
            FACTOR_OPTION = "factor",
            MAKE_PREFERRED_OPTION = "p",
            SKIP_POST_PROCESSING_OPTION = "nopost",
            ADJUST_LIBRARY_SIZES_OPTION = "adjustLibrarySizes",
            ALLOW_UNMAPPED_CHARACTERISTICS_OPTION = "allowUnmappedCharacteristics",
            ALLOW_UNMAPPED_FACTOR_VALUES_OPTION = "allowUnmappedFactorValues",
            MAPPING_FILE_OPTION = "mappingFile",
            REDO_OPTION = "redo",
            REDO_QT_OPTION = "redoQt",
            REDO_DIMENSION_OPTION = "redoDimension",
            PRINT_MAPPING_OPTION = "writeCellTypeMapping";

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    @Autowired
    private SingleCellExpressionExperimentSplitAndAggregateService splitAndAggregateService;

    @Autowired
    private PreprocessorService preprocessorService;

    @Nullable
    private String ctaIdentifier;
    @Nullable
    private String clcIdentifier;
    @Nullable
    private String factorName;
    @Nullable
    private Path mappingFile;
    @Nullable
    private String maskIdentifier;
    private boolean noMask;
    private boolean allowUnmappedCharacteristics;
    private boolean allowUnmappedFactorValues;
    private boolean makePreferred;
    private boolean skipPostProcessing;
    private boolean adjustLibrarySizes;
    private boolean redo;
    @Nullable
    private String redoQt;
    @Nullable
    private String redoDimension;
    private boolean printMapping;

    public SingleCellDataAggregatorCli() {
        super( SingleCellExpressionDataVector.class );
        setUsePreferredQuantitationType();
    }

    @Nullable
    @Override
    public String getCommandName() {
        return "aggregateSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Aggregate single cell data into pseudo-bulks";
    }

    @Override
    protected void buildExperimentVectorsOptions( Options options ) {
        options.addOption( CTA_OPTION, "cell-type-assignment", true, "Name of the cell type assignment to use (defaults to the preferred one). Incompatible with -" + CLC_OPTION + "." );
        addSingleExperimentOption( options, CLC_OPTION, "cell-level-characteristics", true, "Identifier of the cell-level characteristics to use. Incompatible with -" + CTA_OPTION + "." );
        addSingleExperimentOption( options, MASK_OPTION, "mask", true, "Identifier of the cell-level characteristics to use to mask. Defaults to auto-detecting the mask." );
        addSingleExperimentOption( options, NO_MASK_OPTION, "--no-mask", true, "Do not use a mask if one is auto-detected for aggregating single-cell data. Incompatible with -" + MASK_OPTION + "." );
        options.addOption( FACTOR_OPTION, "factor", true, "Identifier of the factor to use (defaults to the cell type factor)" );
        addSingleExperimentOption( options, Option.builder( MAPPING_FILE_OPTION ).longOpt( "mapping-file" ).hasArg().type( Path.class ).desc( "File containing explicit mapping between cell-level characteristics and factor values" ).build() );
        options.addOption( ALLOW_UNMAPPED_CHARACTERISTICS_OPTION, "allow-unmapped-characteristics", false, "Allow unmapped characteristics from the cell-level characteristics." );
        options.addOption( ALLOW_UNMAPPED_FACTOR_VALUES_OPTION, "allow-unmapped-factor-values", false, "Allow unmapped factor values from the experimental factor." );
        options.addOption( MAKE_PREFERRED_OPTION, "make-preferred", false, "Make the resulting aggregated data the preferred raw data for the experiment." );
        options.addOption( ADJUST_LIBRARY_SIZES_OPTION, false, "Adjust library sizes for the resulting aggregated assays." );
        options.addOption( REDO_OPTION, "redo", false, "Redo the aggregation." );
        // a string is fine to use when bulk-processing
        options.addOption( REDO_QT_OPTION, "redo-quantitation-type", true, "Quantitation to re-aggregate, defaults to the preferred one. Requires the -" + REDO_OPTION + " flag. Incompatible with -" + REDO_DIMENSION_OPTION + "." );
        addSingleExperimentOption( options, REDO_DIMENSION_OPTION, "redo-dimension", true, "Dimension to re-aggregate, defaults to the one corresponding to -" + REDO_QT_OPTION + ". Requires the -" + REDO_OPTION + " flag. Incompatible with -" + REDO_QT_OPTION + "." );
        options.addOption( PRINT_MAPPING_OPTION, "print-mapping", false, "Print the cell type mapping to the standard output. No aggregation is performed or redone." );
        options.addOption( SKIP_POST_PROCESSING_OPTION, "no-post-processing", false, "Skip post-processing steps after aggregation." );
    }

    @Override
    protected void processExperimentVectorsOptions( CommandLine commandLine ) throws ParseException {
        ctaIdentifier = commandLine.getOptionValue( CTA_OPTION );
        clcIdentifier = commandLine.getOptionValue( CLC_OPTION );
        if ( ctaIdentifier != null && clcIdentifier != null ) {
            throw new ParseException( "Only one of -cta or -clc can be set at a time." );
        }
        maskIdentifier = getOptionValue( commandLine, MASK_OPTION, requires( toBeUnset( NO_MASK_OPTION ) ) );
        noMask = commandLine.hasOption( NO_MASK_OPTION );
        factorName = commandLine.getOptionValue( FACTOR_OPTION );
        allowUnmappedCharacteristics = commandLine.hasOption( ALLOW_UNMAPPED_CHARACTERISTICS_OPTION );
        allowUnmappedFactorValues = commandLine.hasOption( ALLOW_UNMAPPED_FACTOR_VALUES_OPTION );
        mappingFile = commandLine.getParsedOptionValue( MAPPING_FILE_OPTION );
        printMapping = commandLine.hasOption( PRINT_MAPPING_OPTION );
        makePreferred = commandLine.hasOption( MAKE_PREFERRED_OPTION );
        skipPostProcessing = commandLine.hasOption( SKIP_POST_PROCESSING_OPTION );
        adjustLibrarySizes = commandLine.hasOption( ADJUST_LIBRARY_SIZES_OPTION );
        redo = commandLine.hasOption( REDO_OPTION );
        redoQt = getOptionValue( commandLine, REDO_QT_OPTION,
                requires( allOf( toBeSet( REDO_OPTION ), toBeUnset( REDO_DIMENSION_OPTION ) ) ) );
        redoDimension = getOptionValue( commandLine, REDO_DIMENSION_OPTION,
                requires( allOf( toBeSet( REDO_OPTION ), toBeUnset( REDO_QT_OPTION ) ) ) );
    }

    @Override
    protected void processExpressionExperimentVectors( ExpressionExperiment expressionExperiment, QuantitationType qt ) {
        log.info( "Splitting single cell data into pseudo-bulks for: " + expressionExperiment + " and " + qt );

        expressionExperiment = eeService.thawLite( expressionExperiment );

        CellLevelCharacteristics clc;
        if ( ctaIdentifier != null ) {
            clc = entityLocator.locateCellTypeAssignment( expressionExperiment, qt, ctaIdentifier );
        } else if ( clcIdentifier != null ) {
            clc = entityLocator.locateCellLevelCharacteristics( expressionExperiment, qt, clcIdentifier );
        } else {
            ExpressionExperiment finalExpressionExperiment = expressionExperiment;
            clc = singleCellExpressionExperimentService.getPreferredCellTypeAssignment( expressionExperiment, qt )
                    .orElseThrow( () -> new IllegalStateException( finalExpressionExperiment + " does not have a preferred cell-type assignment for " + qt + "." ) );
        }

        CellLevelCharacteristics mask;
        if ( noMask ) {
            mask = null;
        } else if ( maskIdentifier != null ) {
            mask = entityLocator.locateCellLevelCharacteristics( expressionExperiment, qt, maskIdentifier );
        } else {
            log.info( "Auto-detecting the mask for " + expressionExperiment + " and " + qt + "..." );
            mask = singleCellExpressionExperimentService.getCellLevelMask( expressionExperiment, qt )
                    .orElse( null );
        }

        ExpressionExperiment finalExpressionExperiment1 = expressionExperiment;

        ExperimentalFactor cellTypeFactor;
        if ( factorName != null ) {
            cellTypeFactor = entityLocator.locateExperimentalFactor( expressionExperiment, factorName );
        } else {
            cellTypeFactor = singleCellExpressionExperimentService.getCellTypeFactor( expressionExperiment )
                    .orElseThrow( () -> new IllegalStateException( finalExpressionExperiment1 + " does not have a cell type factor." ) );
        }

        SplitConfig splitConfig = SplitConfig.builder()
                .ignoreUnmatchedCharacteristics( allowUnmappedCharacteristics )
                .ignoreUnmatchedFactorValues( allowUnmappedFactorValues )
                .build();

        AggregateConfig config = AggregateConfig.builder()
                .mask( mask )
                .makePreferred( makePreferred )
                .adjustLibrarySizes( adjustLibrarySizes )
                .build();

        QuantitationType newQt;
        if ( redo ) {
            QuantitationType previousQt;
            BioAssayDimension dimension;
            if ( redoDimension != null ) {
                dimension = eeService.getBioAssayDimensionById( expressionExperiment, Long.parseLong( redoDimension ) );
                if ( dimension == null ) {
                    throw new IllegalStateException( "No bioassay dimension with ID " + redoDimension + "." );
                }
                Collection<QuantitationType> previousQts = eeService.getQuantitationTypes( expressionExperiment, dimension, RawExpressionDataVector.class );
                if ( previousQts.isEmpty() ) {
                    previousQt = null;
                } else if ( previousQts.size() > 1 ) {
                    throw new IllegalStateException( String.format( "There is more than one quantitation type for %s. Use -redoQt to re-aggregate a specific one. Possible values:\n\t%s",
                            redoDimension, previousQts.stream().map( QuantitationType::toString ).collect( Collectors.joining( "\n\t" ) ) ) );
                } else {
                    previousQt = previousQts.iterator().next();
                }
            } else {
                if ( redoQt != null ) {
                    previousQt = entityLocator.locateQuantitationType( expressionExperiment, redoQt, RawExpressionDataVector.class );
                } else {
                    ExpressionExperiment finalExpressionExperiment2 = expressionExperiment;
                    previousQt = eeService.getPreferredQuantitationType( expressionExperiment )
                            .orElseThrow( () -> new IllegalStateException( "No preferred quantitation type found for " + finalExpressionExperiment2 + "." ) );
                }
                dimension = eeService.getBioAssayDimension( expressionExperiment, previousQt, RawExpressionDataVector.class );
                if ( dimension == null ) {
                    throw new IllegalStateException( "No dimension found for " + expressionExperiment + " and " + previousQt + "." );
                }
            }

            if ( previousQt != null && previousQt.getIsPreferred() && !makePreferred ) {
                log.warn( "The previous quantitation type is the preferred one, consider using -" + MAKE_PREFERRED_OPTION + " to make the new one preferred as well." );
            }

            Map<FactorValue, ExpressionExperimentSubSet> subsetsByFv = eeService.getSubSetsByFactorValueWithCharacteristicsAndBioAssays( expressionExperiment, cellTypeFactor, dimension );

            if ( subsetsByFv == null ) {
                throw new IllegalStateException( "There are no subsets by " + cellTypeFactor + " for " + dimension );
            }

            Set<FactorValue> mappedFactorValues = subsetsByFv.keySet();
            log.info( "Re-aggregating the following subsets:\n\t" + subsetsByFv.entrySet().stream()
                    .map( e -> FactorValueUtils.getSummaryString( e.getKey() ) + " → " + e.getValue() )
                    .collect( Collectors.joining( "\n\t" ) ) );

            Map<Characteristic, FactorValue> c2f;
            if ( mappingFile != null ) {
                try {
                    c2f = readMappingFromFile( clc, cellTypeFactor, mappingFile );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } else {
                c2f = createMappingByFactorValueCharacteristics( clc, cellTypeFactor );
                log.info( "Mapping by factor value characteristics:\n" + printMapping( c2f ) );
                if ( !c2f.values().containsAll( mappedFactorValues ) ) {
                    log.warn( "Cannot fully reconstruct the original cell type mapping from the factor values, will attempt to use the subset characteristics..." );
                    c2f = createMappingBySubSetCharacteristics( clc, cellTypeFactor, subsetsByFv );
                    log.info( "Mapping by subset characteristics:\n" + printMapping( c2f ) );
                }
            }

            if ( c2f.values().containsAll( mappedFactorValues ) ) {
                log.info( "Mapping is complete!" );
            } else if ( allowUnmappedFactorValues ) {
                log.warn( String.format( "Not all factor values from %s are mapped, ignoring since -%s is set.",
                        cellTypeFactor, ALLOW_UNMAPPED_FACTOR_VALUES_OPTION ) );
            } else {
                throw new IllegalStateException( String.format( "Not all factor values from %s are mapped. You can either provide a complete mapping with -%s or pass the -%s option.",
                        cellTypeFactor, MAPPING_FILE_OPTION, ALLOW_UNMAPPED_FACTOR_VALUES_OPTION ) );
            }

            if ( printMapping ) {
                try {
                    writeMapping( clc, cellTypeFactor, c2f, new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
                return;
            }

            try {
                newQt = splitAndAggregateService.redoAggregate( expressionExperiment, qt, clc, cellTypeFactor, c2f, dimension, previousQt, config );
                addSuccessObject( expressionExperiment, "Aggregated single-cell data into " + newQt + "." );
            } catch ( UnsupportedScaleTypeForAggregationException e ) {
                addErrorObject( expressionExperiment, String.format( "Aggregation is not support for data of scale type %s, change it first in the GUI %s.",
                        qt.getScale(), entityUrlBuilder.fromHostUrl().entity( expressionExperiment ).web().edit().toUriString() ), e );
                return;
            }
        } else {
            Map<Characteristic, FactorValue> c2f;
            if ( mappingFile != null ) {
                try {
                    c2f = readMappingFromFile( clc, cellTypeFactor, mappingFile );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
            } else {
                c2f = createMappingByFactorValueCharacteristics( clc, cellTypeFactor );
            }

            if ( printMapping ) {
                try {
                    writeMapping( clc, cellTypeFactor, c2f, new OutputStreamWriter( getCliContext().getOutputStream(), StandardCharsets.UTF_8 ) );
                } catch ( IOException e ) {
                    throw new RuntimeException( e );
                }
                return;
            }

            try {
                newQt = splitAndAggregateService.splitAndAggregate( expressionExperiment, qt, clc, cellTypeFactor, c2f, splitConfig, config );
                addSuccessObject( expressionExperiment, "Aggregated single-cell data into " + newQt + "." );
            } catch ( UnsupportedScaleTypeForAggregationException e ) {
                addErrorObject( expressionExperiment, String.format( "Aggregation is not support for data of scale type %s, change it first in the GUI %s.",
                        qt.getScale(), entityUrlBuilder.fromHostUrl().entity( expressionExperiment ).web().edit().toUriString() ), e );
                return;
            }
        }

        // create/recreate processed vectors
        boolean refreshProcessedVectors = false;
        if ( newQt.getIsPreferred() ) {
            log.info( "Creating a data file for " + newQt + "..." );
            try ( LockedPath lockedFile = expressionDataFileService.writeOrLocateRawExpressionDataFile( expressionExperiment, newQt, true ) ) {
                addSuccessObject( expressionExperiment, "Created a data file for " + newQt + ": " + lockedFile.getPath() );
            } catch ( IOException e ) {
                addErrorObject( expressionExperiment, "Failed to generate a data file for " + newQt + ".", e );
            }

            if ( !skipPostProcessing ) {
                log.info( "Reprocessing experiment since a new set of raw data vectors was added or replaced..." );
                try {
                    expressionExperiment = eeService.thaw( expressionExperiment );
                    preprocessorService.process( expressionExperiment );
                    addSuccessObject( expressionExperiment, "Post-processed data from " + newQt + "." );
                } catch ( Exception e ) {
                    addErrorObject( expressionExperiment, "Failed to post-process the data from " + newQt + ".", e );
                } finally {
                    // if process() fails, vector might or might not have been created, so we should evict the cache of
                    // Gemma Web regardless
                    refreshProcessedVectors = true;
                }
            }
        }

        // refresh
        try {
            refreshExpressionExperimentFromGemmaWeb( expressionExperiment, refreshProcessedVectors, false );
        } catch ( Exception e ) {
            addWarningObject( expressionExperiment, "Failed to refresh the experiment from Gemma Web.", e );
        }
    }
}
