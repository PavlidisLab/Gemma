package ubic.gemma.core.apps;

import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionExperimentDataFileType;
import ubic.gemma.core.loader.expression.singleCell.*;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class SingleCellDataLoaderCli extends ExpressionExperimentManipulatingCLI {

    private static final String
            LOAD_CELL_TYPE_ASSIGNMENT_OPTION = "loadCta",
            LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION = "loadClc",
            DATA_TYPE_OPTION = "dataType",
            DATA_PATH_OPTION = "p",
            PLATFORM_OPTION = "a",
            QT_NAME_OPTION = "qtName",
            QT_NEW_NAME_OPTION = "qtNewName",
            QT_NEW_TYPE_OPTION = "qtNewType",
            QT_NEW_SCALE_TYPE_OPTION = "qtNewScaleType",
            PREFERRED_QT_OPTION = "preferredQt",
            REPLACE_OPTION = "replace",
            CELL_TYPE_ASSIGNMENT_FILE_OPTION = "ctaFile",
            CELL_TYPE_ASSIGNMENT_NAME_OPTION = "ctaName",
            CELL_TYPE_ASSIGNMENT_PROTOCOL_NAME_OPTION = "ctaProtocol",
            PREFERRED_CELL_TYPE_ASSIGNMENT = "preferredCta",
            OTHER_CELL_LEVEL_CHARACTERISTICS_FILE = "clcFile";

    private static final String
            INFER_SAMPLES_FROM_CELL_IDS_OVERLAP_OPTION = "inferSamplesFromCellIdsOverlap",
            IGNORE_UNMATCHED_CELL_IDS_OPTION = "ignoreUnmatchedCellIds";

    private static final String ANNDATA_OPTION_PREFIX = "annData";
    private static final String
            ANNDATA_SAMPLE_FACTOR_NAME_OPTION = ANNDATA_OPTION_PREFIX + "SampleFactorName",
            ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION = ANNDATA_OPTION_PREFIX + "CellTypeFactorName",
            ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION = ANNDATA_OPTION_PREFIX + "UnknownCellTypeIndicator",
            ANNDATA_TRANSPOSE_OPTION = ANNDATA_OPTION_PREFIX + "Transpose",
            ANNDATA_USE_X_OPTION = ANNDATA_OPTION_PREFIX + "UseX",
            ANNDATA_USE_RAW_X_OPTION = ANNDATA_OPTION_PREFIX + "UseRawX";

    private static final String MEX_OPTION_PREFIX = "mex";
    private static final String
            MEX_DISCARD_EMPTY_CELLS_OPTION = MEX_OPTION_PREFIX + "DiscardEmptyCells",
            MEX_ALLOW_MAPPING_DESIGN_ELEMENTS_TO_GENE_SYMBOLS_OPTION = MEX_OPTION_PREFIX + "AllowMappingDesignElementsToGeneSymbols";

    @Autowired
    private SingleCellDataLoaderService singleCellDataLoaderService;

    @Autowired
    private ExpressionDataFileService expressionDataFileService;

    /**
     * Operation mode when loading data.
     */
    private Mode mode;

    enum Mode {
        LOAD_CELL_TYPE_ASSIGNMENTS,
        LOAD_CELL_LEVEL_CHARACTERISTICS,
        LOAD_EVERYTHING
    }

    @Nullable
    private String platformName;
    @Nullable
    private Path dataPath;
    @Nullable
    private SingleCellDataType dataType;
    @Nullable
    private String qtName;
    private String newName;
    private StandardQuantitationType newType;
    private ScaleType newScaleType;
    private boolean preferredQt;
    private boolean replaceQt;
    @Nullable
    private Path cellTypeAssignmentFile;
    @Nullable
    private String cellTypeAssignmentName;
    @Nullable
    private String cellTypeAssignmentProtocolName;
    private boolean preferredCellTypeAssignment;
    @Nullable
    private Path otherCellLevelCharacteristicsFile;
    private boolean inferSamplesFromCellIdsOverlap;
    private boolean ignoreUnmatchedCellIds;

    // AnnData
    private String annDataSampleFactorName;
    private String annDataCellTypeFactorName;
    private String annDataUnknownCellTypeIndicator;
    private boolean annDataTranspose;
    @Nullable
    private Boolean annDataUseRawX;

    // MEX
    private boolean mexDiscardEmptyCells;
    private boolean mexAllowMappingDesignElementsToGeneSymbols;

    @Nullable
    @Override
    public String getCommandName() {
        return "loadSingleCellData";
    }

    @Nullable
    @Override
    public String getShortDesc() {
        return "Load single-cell data from either AnnData or 10x MEX format.";
    }

    @Override
    protected void buildExperimentOptions( Options options ) {
        options.addOption( LOAD_CELL_TYPE_ASSIGNMENT_OPTION, "load-cell-type-assignment", false, "Only load cell type assignment. Use -" + QT_NAME_OPTION + " to specify which set of vectors this is applicable to." );
        options.addOption( LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION, "load-cell-level-characteristics", false, "Only load cell-level characteristics. Use -" + QT_NAME_OPTION + " to specify which set of vectors this is applicable to." );
        options.addOption( DATA_TYPE_OPTION, "data-type", true, "Data type to import. Must be one of " + Arrays.stream( SingleCellDataType.values() ).map( Enum::name ).collect( Collectors.joining( ", " ) ) + "." );
        options.addOption( Option.builder( DATA_PATH_OPTION )
                .longOpt( "data-path" )
                .hasArg()
                .type( Path.class )
                .desc( "Load single-cell data from the given path instead of looking up the download directory. For AnnData and Seurat Disk, it is a file. For MEX it is a directory. Requires the -" + DATA_TYPE_OPTION + " option to be set." )
                .build() );
        options.addOption( PLATFORM_OPTION, "platform", true, "Target platform (must already exist in the system)" );
        options.addOption( QT_NAME_OPTION, "quantitation-type-name", true, "Quantitation type to import (optional, use if more than one is present in data)" );
        options.addOption( QT_NEW_NAME_OPTION, "quantitation-type-new-name", true, "New name to use for the imported quantitation type (optional, defaults to the data)" );
        options.addOption( QT_NEW_TYPE_OPTION, "quantitation-type-new-type", true, "New type to use for the imported quantitation type (optional, defaults to the data)" );
        options.addOption( QT_NEW_SCALE_TYPE_OPTION, "quantitation-type-new-scale-type", true, "New scale type to use for the imported quantitation type (optional, defaults to the data)" );
        options.addOption( PREFERRED_QT_OPTION, "preferred-quantitation-type", false, "Make the quantitation type the preferred one." );
        options.addOption( REPLACE_OPTION, "replace", false, "Replace an existing quantitation type. The -" + QT_NAME_OPTION + "/--quantitation-type-name option must be set." );
        // for the generic metadata loader

        // for the generic metadata loader
        options.addOption( Option.builder( CELL_TYPE_ASSIGNMENT_FILE_OPTION )
                .longOpt( "cell-type-assignment-file" )
                .hasArg().type( Path.class )
                .desc( "Path to a cell type assignment file. If missing, cell type importing will be delegated to a specific loader. For AnnData, you must supply the -" + ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION + " option." )
                .build() );
        options.addOption( CELL_TYPE_ASSIGNMENT_NAME_OPTION, "cell-type-assignment-name", true, "Name to use for the cell type assignment. This require the -" + CELL_TYPE_ASSIGNMENT_FILE_OPTION + " option to be set." );
        options.addOption( CELL_TYPE_ASSIGNMENT_PROTOCOL_NAME_OPTION, "cell-type-assignment-protocol", true, "An identifier for a protocol describing the cell type assignment. This require the -" + CELL_TYPE_ASSIGNMENT_FILE_OPTION + " option to be set." );
        options.addOption( PREFERRED_CELL_TYPE_ASSIGNMENT, "preferred-cell-type-assignment", false, "Make the cell type assignment the preferred one." );
        options.addOption( Option.builder( OTHER_CELL_LEVEL_CHARACTERISTICS_FILE )
                .longOpt( "cell-level-characteristics-file" )
                .hasArg().type( Path.class )
                .desc( "Path to a file containing additional cell-level characteristics to import." )
                .build() );
        options.addOption( INFER_SAMPLES_FROM_CELL_IDS_OVERLAP_OPTION, "infer-samples-from-cell-ids-overlap", false, "Infer sample names from cell IDs overlap." );
        options.addOption( IGNORE_UNMATCHED_CELL_IDS_OPTION, "ignore-unmatched-cell-ids", false, "Ignore unmatched cell IDs when loading cell type assignments and other cell-level characteristics." );

        // for AnnData
        options.addOption( ANNDATA_SAMPLE_FACTOR_NAME_OPTION, "anndata-sample-factor-name", true, "Name of the factor used for the sample name." );
        options.addOption( ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION, "anndata-cell-type-factor-name", true, "Name of the factor used for the cell type, incompatible with -" + CELL_TYPE_ASSIGNMENT_FILE_OPTION + "." );
        options.addOption( ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION, "anndata-unknown-cell-type-indicator", true, "Indicator used for missing cell type. Defaults to using the standard -1 categorical code." );
        options.addOption( ANNDATA_USE_X_OPTION, "anndata-use-x", true, "Use X. This option is incompatible with -" + ANNDATA_USE_RAW_X_OPTION + "." );
        options.addOption( ANNDATA_USE_RAW_X_OPTION, "anndata-use-raw-x", true, "Use raw.X. This option is incompatible with -" + ANNDATA_USE_X_OPTION + "." );
        options.addOption( ANNDATA_TRANSPOSE_OPTION, "anndata-transpose", false, "Transpose the data matrix." );

        // for MEX
        options.addOption( MEX_DISCARD_EMPTY_CELLS_OPTION, "mex-discard-empty-cells", false, "Discard empty cells when loading MEX data." );
        options.addOption( MEX_ALLOW_MAPPING_DESIGN_ELEMENTS_TO_GENE_SYMBOLS_OPTION, "mex-allow-mapping-design-elements-to-gene-symbols", false, "Allow mapping probe names to gene symbols when loading MEX data (i.e. the second column in features.tsv.gz)." );
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( commandLine.hasOption( LOAD_CELL_TYPE_ASSIGNMENT_OPTION ) && commandLine.hasOption( LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION ) ) {
            throw new IllegalArgumentException( "Can only choose one of -" + LOAD_CELL_TYPE_ASSIGNMENT_OPTION + " and -" + LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION + " at a time." );
        }
        if ( commandLine.hasOption( LOAD_CELL_TYPE_ASSIGNMENT_OPTION ) ) {
            mode = Mode.LOAD_CELL_TYPE_ASSIGNMENTS;
            if ( commandLine.hasOption( PLATFORM_OPTION ) ) {
                throw new ParseException( "The -" + PLATFORM_OPTION + " cannot be used with -" + LOAD_CELL_TYPE_ASSIGNMENT_OPTION + "." );
            }
        } else if ( commandLine.hasOption( LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION ) ) {
            mode = Mode.LOAD_CELL_LEVEL_CHARACTERISTICS;
            if ( commandLine.hasOption( PLATFORM_OPTION ) ) {
                throw new ParseException( "The -" + PLATFORM_OPTION + " cannot be used with -" + LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION + "." );
            }
        } else {
            mode = Mode.LOAD_EVERYTHING;
            platformName = commandLine.getOptionValue( PLATFORM_OPTION );
            if ( platformName == null ) {
                throw new MissingArgumentException( "The -" + PLATFORM_OPTION + " option is required when loading vectors." );
            }
        }
        if ( commandLine.hasOption( DATA_TYPE_OPTION ) ) {
            dataType = SingleCellDataType.valueOf( commandLine.getOptionValue( DATA_TYPE_OPTION ) );
        }
        if ( commandLine.hasOption( DATA_PATH_OPTION ) ) {
            if ( dataType == null ) {
                throw new ParseException( "The -" + DATA_TYPE_OPTION + " option must be set if a data path is provided." );
            }
            dataPath = commandLine.getParsedOptionValue( DATA_PATH_OPTION );
        }
        qtName = commandLine.getOptionValue( QT_NAME_OPTION );
        newName = commandLine.getOptionValue( QT_NEW_NAME_OPTION );
        if ( commandLine.hasOption( QT_NEW_TYPE_OPTION ) ) {
            newType = StandardQuantitationType.valueOf( commandLine.getOptionValue( QT_NEW_TYPE_OPTION ).toUpperCase() );
        } else {
            newType = null;
        }
        if ( commandLine.hasOption( QT_NEW_SCALE_TYPE_OPTION ) ) {
            newScaleType = ScaleType.valueOf( commandLine.getOptionValue( QT_NEW_SCALE_TYPE_OPTION ) );
        } else {
            newScaleType = null;
        }
        if ( commandLine.hasOption( REPLACE_OPTION ) ) {
            if ( qtName == null ) {
                throw new MissingOptionException( "The -" + QT_NAME_OPTION + " option must be set in order to replace an existing set of vectors. Use the listQuantitationTypes command to enumerate possible values." );
            }
            replaceQt = true;
        }
        preferredQt = commandLine.hasOption( PREFERRED_QT_OPTION );
        cellTypeAssignmentFile = commandLine.getParsedOptionValue( CELL_TYPE_ASSIGNMENT_FILE_OPTION );
        cellTypeAssignmentName = commandLine.getOptionValue( CELL_TYPE_ASSIGNMENT_NAME_OPTION );
        cellTypeAssignmentProtocolName = commandLine.getOptionValue( CELL_TYPE_ASSIGNMENT_PROTOCOL_NAME_OPTION );
        otherCellLevelCharacteristicsFile = commandLine.getParsedOptionValue( OTHER_CELL_LEVEL_CHARACTERISTICS_FILE );
        preferredCellTypeAssignment = commandLine.hasOption( PREFERRED_CELL_TYPE_ASSIGNMENT );
        inferSamplesFromCellIdsOverlap = commandLine.hasOption( INFER_SAMPLES_FROM_CELL_IDS_OVERLAP_OPTION );
        ignoreUnmatchedCellIds = commandLine.hasOption( IGNORE_UNMATCHED_CELL_IDS_OPTION );
        rejectInvalidOptionsForDataType( commandLine, dataType );
        if ( dataType == SingleCellDataType.ANNDATA ) {
            annDataSampleFactorName = commandLine.getOptionValue( ANNDATA_SAMPLE_FACTOR_NAME_OPTION );
            annDataCellTypeFactorName = commandLine.getOptionValue( ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION );
            annDataUnknownCellTypeIndicator = commandLine.getOptionValue( ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION );
            annDataTranspose = commandLine.hasOption( ANNDATA_TRANSPOSE_OPTION );
            if ( commandLine.hasOption( ANNDATA_USE_X_OPTION ) && commandLine.hasOption( ANNDATA_USE_X_OPTION ) ) {
                throw new ParseException( "Only one of -" + ANNDATA_USE_X_OPTION + " and -" + ANNDATA_USE_RAW_X_OPTION + " can be set." );
            }
            if ( commandLine.hasOption( ANNDATA_USE_RAW_X_OPTION ) ) {
                annDataUseRawX = true;
            } else if ( commandLine.hasOption( ANNDATA_USE_X_OPTION ) ) {
                annDataUseRawX = false;
            } else {
                annDataUseRawX = null;
            }
            if ( cellTypeAssignmentFile != null && annDataCellTypeFactorName != null ) {
                throw new ParseException( String.format( "The -%s option would override the value of -%s.",
                        CELL_TYPE_ASSIGNMENT_FILE_OPTION, ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION ) );
            }
        } else if ( dataType == SingleCellDataType.MEX ) {
            mexDiscardEmptyCells = commandLine.hasOption( MEX_DISCARD_EMPTY_CELLS_OPTION );
            mexAllowMappingDesignElementsToGeneSymbols = commandLine.hasOption( MEX_ALLOW_MAPPING_DESIGN_ELEMENTS_TO_GENE_SYMBOLS_OPTION );
        }
    }

    private void rejectInvalidOptionsForDataType( CommandLine commandLine, @Nullable SingleCellDataType dataType ) throws ParseException {
        SingleCellDataType[] dt = new SingleCellDataType[] { SingleCellDataType.ANNDATA, SingleCellDataType.MEX };
        String[] prefixes = new String[] { ANNDATA_OPTION_PREFIX, MEX_OPTION_PREFIX };
        for ( int i = 0; i < dt.length; i++ ) {
            if ( dt[i] != dataType ) {
                String prefix = prefixes[i];
                for ( Option o : commandLine.getOptions() ) {
                    if ( o.getOpt().startsWith( prefix ) ) {
                        throw new ParseException( String.format( "Options starting with -%s require -%s to be set to %s.",
                                prefixes[i], DATA_TYPE_OPTION, dt[i] ) );
                    }
                }
            }
        }
    }

    @Override
    protected void processBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        if ( dataPath != null || qtName != null || cellTypeAssignmentFile != null || otherCellLevelCharacteristicsFile != null ) {
            throw new IllegalArgumentException( "Cannot specify a data path, quantitation type name, cell type assignment file or cell-level characteristics file when processing more than one experiment." );
        }
        super.processBioAssaySets( expressionExperiments );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        SingleCellDataLoaderConfig config = getConfigForDataType( dataType );
        switch ( mode ) {
            case LOAD_CELL_TYPE_ASSIGNMENTS:
                Collection<CellTypeAssignment> cta;
                if ( dataType != null ) {
                    cta = singleCellDataLoaderService.loadCellTypeAssignments( ee, dataType, config );
                } else {
                    cta = singleCellDataLoaderService.loadCellTypeAssignments( ee, config );
                }
                addSuccessObject( ee, "Loaded cell type assignments " + cta );
                break;
            case LOAD_CELL_LEVEL_CHARACTERISTICS:
                Collection<CellLevelCharacteristics> clc;
                if ( dataType != null ) {
                    clc = singleCellDataLoaderService.loadOtherCellLevelCharacteristics( ee, dataType, config );
                } else {
                    clc = singleCellDataLoaderService.loadOtherCellLevelCharacteristics( ee, config );
                }
                addSuccessObject( ee, "Loaded cell-level characteristics " + clc );
                break;
            case LOAD_EVERYTHING:
                QuantitationType qt;
                if ( dataType != null ) {
                    qt = singleCellDataLoaderService.load( ee, getPlatform(), dataType, config );
                } else {
                    qt = singleCellDataLoaderService.load( ee, getPlatform(), config );
                }
                if ( qt.getIsSingleCellPreferred() ) {
                    log.info( "Generating MEX data files for preferred QT: " + qt + "..." );
                    try ( ExpressionDataFileService.LockedPath lockedPath = expressionDataFileService.writeOrLocateMexSingleCellExpressionData( ee, qt, true, 500, true ) ) {
                        log.info( "Generated MEX data file for " + qt + " at " + lockedPath.getPath() + "." );
                    } catch ( IOException e ) {
                        throw new RuntimeException( "Failed to generate MEX data files for " + qt + ".", e );
                    }
                } else if ( replaceQt ) {
                    // attempt to delete the MEX files if they exist since the data was replaced
                    try {
                        expressionDataFileService.deleteDataFile( ee, qt, ExpressionExperimentDataFileType.MEX );
                    } catch ( IOException e ) {
                        throw new RuntimeException( "Failed to delete MEX data files for " + qt + ".", e );
                    }
                } else {
                    log.info( "Adding a non-preferred QT, no need to generate MEX files." );
                }
                addSuccessObject( ee, "Loaded vectors for " + qt );
                break;
            default:
                throw new IllegalArgumentException( "Unknown operation mode " + mode );
        }
    }

    /**
     * Cached platform object.
     */
    @Nullable
    private ArrayDesign platform;

    private ArrayDesign getPlatform() {
        if ( platformName == null ) {
            throw new IllegalStateException( "A platform name must be set." );
        }
        if ( platform == null ) {
            platform = entityLocator.locateArrayDesign( platformName );
        }
        return platform;
    }

    private SingleCellDataLoaderConfig getConfigForDataType( @Nullable SingleCellDataType dataType ) {
        SingleCellDataLoaderConfig.SingleCellDataLoaderConfigBuilder<?, ?> configBuilder;
        if ( dataType == SingleCellDataType.ANNDATA ) {
            configBuilder = AnnDataSingleCellDataLoaderConfig.builder()
                    .sampleFactorName( annDataSampleFactorName )
                    .cellTypeFactorName( annDataCellTypeFactorName )
                    .unknownCellTypeIndicator( annDataUnknownCellTypeIndicator )
                    .transpose( annDataTranspose );
            if ( annDataUseRawX != null ) {
                ( ( AnnDataSingleCellDataLoaderConfig.AnnDataSingleCellDataLoaderConfigBuilder<?, ?> ) configBuilder )
                        .useRawX( annDataUseRawX );
            }
        } else if ( dataType == SingleCellDataType.MEX ) {
            configBuilder = MexSingleCellDataLoaderConfig.builder()
                    .discardEmptyCells( mexDiscardEmptyCells )
                    .allowMappingDesignElementsToGeneSymbols( mexAllowMappingDesignElementsToGeneSymbols );
        } else {
            configBuilder = SingleCellDataLoaderConfig.builder();
        }
        if ( dataPath != null ) {
            configBuilder.dataPath( dataPath );
        }
        if ( qtName != null ) {
            configBuilder
                    .quantitationTypeName( qtName )
                    .quantitationTypeNewName( newName )
                    .quantitationTypeNewType( newType )
                    .quantitationTypeNewScaleType( newScaleType )
                    .replaceExistingQuantitationType( replaceQt );
        }
        configBuilder.markQuantitationTypeAsPreferred( preferredQt );
        if ( cellTypeAssignmentFile != null ) {
            configBuilder
                    .cellTypeAssignmentFile( cellTypeAssignmentFile )
                    .markSingleCellTypeAssignmentAsPreferred( preferredCellTypeAssignment );
            if ( cellTypeAssignmentName != null ) {
                configBuilder
                        .cellTypeAssignmentName( cellTypeAssignmentName );
            }
            if ( cellTypeAssignmentProtocolName != null ) {
                configBuilder
                        .cellTypeAssignmentProtocol( entityLocator.locateProtocol( cellTypeAssignmentProtocolName ) );
            }
        }
        if ( otherCellLevelCharacteristicsFile != null ) {
            configBuilder.otherCellLevelCharacteristicsFile( otherCellLevelCharacteristicsFile );
        }
        // infer only on-demand
        configBuilder.inferSamplesFromCellIdsOverlap( inferSamplesFromCellIdsOverlap );
        // always allow for using barcodes to infer the sample names from the CLI
        configBuilder.useCellIdsIfSampleNameIsMissing( true );
        // ignore only on-demand
        configBuilder.ignoreUnmatchedCellIds( ignoreUnmatchedCellIds );
        return configBuilder.build();
    }
}
