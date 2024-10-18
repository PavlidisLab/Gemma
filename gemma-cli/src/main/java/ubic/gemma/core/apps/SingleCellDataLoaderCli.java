package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.loader.expression.singleCell.AnnDataSingleCellDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderConfig;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataLoaderService;
import ubic.gemma.core.loader.expression.singleCell.SingleCellDataType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class SingleCellDataLoaderCli extends ExpressionExperimentManipulatingCLI {

    private static final String
            LOAD_CELL_TYPE_ASSIGNMENT = "loadCta",
            LOAD_CELL_LEVEL_CHARACTERISTICS = "loadClc",
            DATA_TYPE_OPTION = "dataType",
            DATA_PATH_OPTION = "p",
            PLATFORM_OPTION = "a",
            QT_NAME_OPTION = "qtName",
            PREFERRED_QT_OPTION = "preferredQt",
            REPLACE_OPTION = "replace",
            CELL_TYPE_ASSIGNMENT_FILE_OPTION = "ctaFile",
            PREFERRED_CELL_TYPE_ASSIGNMENT = "preferredCta",
            OTHER_CELL_LEVEL_CHARACTERISTICS_FILE = "clcFile";

    private static final String ANNDATA_OPTION_PREFIX = "annData";
    private static final String
            ANNDATA_SAMPLE_FACTOR_NAME_OPTION = ANNDATA_OPTION_PREFIX + "SampleFactorName",
            ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION = ANNDATA_OPTION_PREFIX + "CellTypeFactorName",
            ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION = ANNDATA_OPTION_PREFIX + "UnknownCellTypeIndicator";

    @Autowired
    private SingleCellDataLoaderService singleCellDataLoaderService;

    /**
     * Operation mode when loading data.
     */
    private Mode mode;

    enum Mode {
        LOAD_CELL_TYPE_ASSIGNMENTS,
        LOAD_CELL_LEVEL_CHARACTERISTICS,
        LOAD_EVERYTHING
    }

    private String platformName;
    @Nullable
    private Path dataPath;
    @Nullable
    private SingleCellDataType dataType;
    @Nullable
    private String qtName;
    private boolean preferredQt;
    private boolean replaceQt;
    @Nullable
    private Path cellTypeAssignmentFile;
    private boolean preferredCellTypeAssignment;
    @Nullable
    private Path otherCellLevelCharacteristicsFile;

    // AnnData
    private String annDataSampleFactorName;
    private String annDataCellTypeFactorName;
    private String annDataUnknownCellTypeIndicator;

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
    protected void buildOptions( Options options ) {
        super.buildOptions( options );
        options.addOption( LOAD_CELL_TYPE_ASSIGNMENT, "load-cell-type-assignment", false, "Only load cell type assignment. Use -" + QT_NAME_OPTION + " to specify which set of vectors this is applicable to." );
        options.addOption( LOAD_CELL_LEVEL_CHARACTERISTICS, "load-cell-level-characteristics", false, "Only load cell-level characteristics. Use -" + QT_NAME_OPTION + " to specify which set of vectors this is applicable to." );
        options.addOption( DATA_TYPE_OPTION, "data-type", true, "Data type to import. Must be one of " + Arrays.stream( SingleCellDataType.values() ).map( Enum::name ).collect( Collectors.joining( ", " ) ) + "." );
        options.addOption( Option.builder( DATA_PATH_OPTION )
                .longOpt( "data-path" )
                .hasArg()
                .type( Path.class )
                .desc( "Load single-cell data from the given path instead of looking up the download directory. For AnnData and Seurat Disk, it is a file. For MEX it is a directory. Requires the -" + DATA_TYPE_OPTION + " option to be set." )
                .build() );
        options.addRequiredOption( PLATFORM_OPTION, "platform", true, "Target platform (must already exist in the system)" );
        options.addOption( QT_NAME_OPTION, "quantitation-type-name", true, "Quantitation type to import (optional, use if more than one is present in data)" );
        options.addOption( PREFERRED_QT_OPTION, "preferred-quantitation-type", false, "Make the quantitation type the preferred one." );
        options.addOption( REPLACE_OPTION, "replace", false, "Replace an existing quantitation type. The -" + QT_NAME_OPTION + "/--quantitation-type-name option must be set." );
        options.addOption( Option.builder( CELL_TYPE_ASSIGNMENT_FILE_OPTION )
                .longOpt( "cell-type-assignment-file" )
                .hasArg().type( Path.class )
                .desc( "Path to a cell type assignment file. If missing, cell type importing will be delegated to a specific loader. For AnnData, you must supply the -" + ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION + " option." )
                .build() );
        options.addOption( PREFERRED_CELL_TYPE_ASSIGNMENT, "preferred-cell-type-assignment", false, "Make the cell type assignment the preferred one." );
        options.addOption( Option.builder( OTHER_CELL_LEVEL_CHARACTERISTICS_FILE )
                .longOpt( "cell-level-characteristics-file" )
                .hasArg().type( Path.class )
                .desc( "Path to a file containing additional cell-level characteristics to import." )
                .build() );
        // for AnnData
        options.addOption( ANNDATA_SAMPLE_FACTOR_NAME_OPTION, "anndata-sample-factor-name", true, "Name of the factor used for the sample name." );
        options.addOption( ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION, "anndata-cell-type-factor-name", true, "Name of the factor used for the cell type, incompatible with -" + CELL_TYPE_ASSIGNMENT_FILE_OPTION + "." );
        options.addOption( ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION, "anndata-unknown-cell-type-indicator", true, "Indicator used for missing cell type. Defaults to using the standard -1 categorical code." );
    }

    @Override
    protected void processOptions( CommandLine commandLine ) throws ParseException {
        super.processOptions( commandLine );
        if ( commandLine.hasOption( LOAD_CELL_TYPE_ASSIGNMENT ) && commandLine.hasOption( LOAD_CELL_LEVEL_CHARACTERISTICS ) ) {
            throw new IllegalArgumentException( "Can only choose one of -" + LOAD_CELL_TYPE_ASSIGNMENT + " and -" + LOAD_CELL_LEVEL_CHARACTERISTICS + " at a time." );
        }
        if ( commandLine.hasOption( LOAD_CELL_TYPE_ASSIGNMENT ) ) {
            mode = Mode.LOAD_CELL_TYPE_ASSIGNMENTS;
        } else if ( commandLine.hasOption( LOAD_CELL_LEVEL_CHARACTERISTICS ) ) {
            mode = Mode.LOAD_CELL_LEVEL_CHARACTERISTICS;
        } else {
            mode = Mode.LOAD_EVERYTHING;
        }
        if ( commandLine.hasOption( DATA_TYPE_OPTION ) ) {
            dataType = SingleCellDataType.valueOf( commandLine.getOptionValue( DATA_TYPE_OPTION ) );
        }
        if ( commandLine.hasOption( DATA_PATH_OPTION ) ) {
            if ( dataType == null ) {
                throw new IllegalArgumentException( "The -" + DATA_TYPE_OPTION + " option must be set of a data path is provided." );
            }
            dataPath = commandLine.getParsedOptionValue( DATA_PATH_OPTION );
        }
        platformName = commandLine.getOptionValue( PLATFORM_OPTION );
        qtName = commandLine.getOptionValue( QT_NAME_OPTION );
        if ( commandLine.hasOption( REPLACE_OPTION ) ) {
            if ( qtName == null ) {
                throw new IllegalArgumentException( "The -" + QT_NAME_OPTION + " option must be set in order to replace an existing set of vectors." );
            }
            replaceQt = true;
        }
        preferredQt = commandLine.hasOption( PREFERRED_QT_OPTION );
        cellTypeAssignmentFile = commandLine.getParsedOptionValue( CELL_TYPE_ASSIGNMENT_FILE_OPTION );
        otherCellLevelCharacteristicsFile = commandLine.getParsedOptionValue( OTHER_CELL_LEVEL_CHARACTERISTICS_FILE );
        preferredCellTypeAssignment = commandLine.hasOption( PREFERRED_CELL_TYPE_ASSIGNMENT );
        if ( dataType == SingleCellDataType.ANNDATA ) {
            annDataSampleFactorName = commandLine.getOptionValue( ANNDATA_SAMPLE_FACTOR_NAME_OPTION );
            annDataCellTypeFactorName = commandLine.getOptionValue( ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION );
            annDataUnknownCellTypeIndicator = commandLine.getOptionValue( ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION );
            if ( cellTypeAssignmentFile != null && annDataCellTypeFactorName != null ) {
                throw new IllegalArgumentException( String.format( "The -%s option would override the value of -%s.",
                        CELL_TYPE_ASSIGNMENT_FILE_OPTION, ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION ) );
            }
        } else {
            for ( Option o : commandLine.getOptions() ) {
                if ( o.getOpt().startsWith( ANNDATA_OPTION_PREFIX ) ) {
                    throw new IllegalArgumentException( String.format( "Options starting with -%s require -%s to be set to %s.",
                            ANNDATA_OPTION_PREFIX, DATA_TYPE_OPTION, SingleCellDataType.ANNDATA ) );
                }
            }
        }
    }

    private ArrayDesign platform;

    @Override
    protected Collection<BioAssaySet> preprocessBioAssaySets( Collection<BioAssaySet> expressionExperiments ) {
        platform = entityLocator.locateArrayDesign( platformName );
        return super.preprocessBioAssaySets( expressionExperiments );
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
                    qt = singleCellDataLoaderService.load( ee, platform, dataType, config );
                } else {
                    qt = singleCellDataLoaderService.load( ee, platform, config );
                }
                addSuccessObject( ee, "Loaded vectors for " + qt.toString() );
                break;
            default:
                throw new IllegalArgumentException( "Unknown operation mode " + mode );
        }
    }

    private SingleCellDataLoaderConfig getConfigForDataType( @Nullable SingleCellDataType dataType ) {
        SingleCellDataLoaderConfig.SingleCellDataLoaderConfigBuilder<?, ?> configBuilder;
        if ( dataType == SingleCellDataType.ANNDATA ) {
            configBuilder = AnnDataSingleCellDataLoaderConfig.builder()
                    .sampleFactorName( annDataSampleFactorName )
                    .cellTypeFactorName( annDataCellTypeFactorName )
                    .unknownCellTypeIndicator( annDataUnknownCellTypeIndicator );
        } else {
            configBuilder = SingleCellDataLoaderConfig.builder();
        }
        if ( dataPath != null ) {
            configBuilder.dataPath( dataPath );
        }
        if ( qtName != null ) {
            configBuilder
                    .quantitationTypeName( qtName )
                    .replaceExistingQuantitationType( replaceQt );
        }
        configBuilder.markQuantitationTypeAsPreferred( preferredQt );
        if ( cellTypeAssignmentFile != null ) {
            configBuilder
                    .cellTypeAssignmentPath( cellTypeAssignmentFile )
                    .markSingleCellTypeAssignmentAsPreferred( preferredCellTypeAssignment );
        }
        if ( otherCellLevelCharacteristicsFile != null ) {
            configBuilder.otherCellLevelCharacteristicsFile( otherCellLevelCharacteristicsFile );
        }
        return configBuilder.build();
    }
}
