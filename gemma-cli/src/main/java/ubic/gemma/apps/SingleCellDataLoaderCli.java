package ubic.gemma.apps;

import org.apache.commons.cli.*;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.cli.completion.CompletionType;
import ubic.gemma.cli.completion.CompletionUtils;
import ubic.gemma.cli.util.EnumeratedByCommandStringConverter;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.analysis.service.ExpressionDataFileService;
import ubic.gemma.core.analysis.service.ExpressionExperimentDataFileType;
import ubic.gemma.core.loader.expression.sequencing.SequencingMetadata;
import ubic.gemma.core.loader.expression.singleCell.*;
import ubic.gemma.core.util.concurrent.Executors;
import ubic.gemma.core.util.concurrent.SimpleThreadFactory;
import ubic.gemma.core.util.locking.LockedPath;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.CellLevelCharacteristics;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static ubic.gemma.cli.util.EntityOptionsUtils.addGenericPlatformOption;
import static ubic.gemma.cli.util.OptionsUtils.*;

public class SingleCellDataLoaderCli extends ExpressionExperimentManipulatingCLI {

    private static final String
            LOAD_CELL_TYPE_ASSIGNMENT_OPTION = "loadCta",
            LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION = "loadClc",
            LOAD_SEQUENCING_METADATA_OPTION = "loadSequencingMetadata";

    private static final String
            DATA_TYPE_OPTION = "dataType",
            DATA_PATH_OPTION = "p",
            PLATFORM_OPTION = "a",
            QT_NAME_OPTION = "qtName",
            QT_NEW_NAME_OPTION = "qtNewName",
            QT_NEW_TYPE_OPTION = "qtNewType",
            QT_NEW_SCALE_TYPE_OPTION = "qtNewScaleType",
            QT_RECOMPUTED_FROM_RAW_DATA_OPTION = "qtRecomputedFromRawData",
            PREFERRED_QT_OPTION = "preferredQt",
            PREFER_SINGLE_PRECISION = "preferSinglePrecision",
            REPLACE_OPTION = "replace",
            RENAMING_FILE_OPTION = "renamingFile",
            IGNORE_SAMPLES_LACKING_DATA_OPTION = "ignoreSamplesLackingData",
            TRANSFORM_THREADS_OPTION = "transformThreads";

    private static final String
            CELL_TYPE_ASSIGNMENT_FILE_OPTION = "ctaFile",
            CELL_TYPE_ASSIGNMENT_NAME_OPTION = "ctaName",
            CELL_TYPE_ASSIGNMENT_DESCRIPTION_OPTION = "ctaDescription",
            CELL_TYPE_ASSIGNMENT_PROTOCOL_NAME_OPTION = "ctaProtocol",
            REPLACE_CELL_TYPE_ASSIGNMENT_OPTION = "replaceCta",
            PREFERRED_CELL_TYPE_ASSIGNMENT_OPTION = "preferredCta",
            OTHER_CELL_LEVEL_CHARACTERISTICS_NAME = "clcName",
            OTHER_CELL_LEVEL_CHARACTERISTICS_FILE = "clcFile",
            REPLACE_OTHER_CELL_LEVEL_CHARACTERISTICS_OPTION = "replaceClc",
            INFER_SAMPLES_FROM_CELL_IDS_OVERLAP_OPTION = "inferSamplesFromCellIdsOverlap",
            IGNORE_UNMATCHED_CELL_IDS_OPTION = "ignoreUnmatchedCellIds";

    private static final String
            REPLACE_CELL_TYPE_FACTOR_OPTION = "replaceCtf",
            KEEP_CELL_TYPE_FACTOR_OPTION = "keepCtf";

    private static final String
            SEQUENCING_METADATA_FILE_OPTION = "sequencingMetadataFile",
            SEQUENCING_READ_LENGTH_OPTION = "sequencingReadLength",
            SEQUENCING_IS_PAIRED_OPTION = "sequencingIsPaired",
            SEQUENCING_IS_SINGLE_END_OPTION = "sequencingIsSingleEnd";

    private static final String ANNDATA_OPTION_PREFIX = "annData";
    private static final String
            ANNDATA_SAMPLE_FACTOR_NAME_OPTION = ANNDATA_OPTION_PREFIX + "SampleFactorName",
            ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION = ANNDATA_OPTION_PREFIX + "CellTypeFactorName",
            ANNDATA_IGNORE_CELL_TYPE_FACTOR_OPTION = ANNDATA_OPTION_PREFIX + "IgnoreCellTypeFactor",
            ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION = ANNDATA_OPTION_PREFIX + "UnknownCellTypeIndicator",
            ANNDATA_TRANSPOSE_OPTION = ANNDATA_OPTION_PREFIX + "Transpose",
            ANNDATA_NO_TRANSPOSE_OPTION = ANNDATA_OPTION_PREFIX + "NoTranspose",
            ANNDATA_USE_X_OPTION = ANNDATA_OPTION_PREFIX + "UseX",
            ANNDATA_USE_RAW_X_OPTION = ANNDATA_OPTION_PREFIX + "UseRawX";

    private static final String MEX_OPTION_PREFIX = "mex";
    private static final String
            MEX_ALLOW_MAPPING_DESIGN_ELEMENTS_TO_GENE_SYMBOLS_OPTION = MEX_OPTION_PREFIX + "AllowMappingDesignElementsToGeneSymbols",
            MEX_USE_DOUBLE_PRECISION_OPTION = MEX_OPTION_PREFIX + "UseDoublePrecision",
            MEX_10X_FILTER_OPTION = MEX_OPTION_PREFIX + "10xFilter",
            MEX_NO_10X_FILTER_OPTION = MEX_OPTION_PREFIX + "No10xFilter",
            MEX_10X_CHEMISTRY_OPTION = MEX_OPTION_PREFIX + "Chemistry";

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
        LOAD_SEQUENCING_METADATA,
        LOAD_EVERYTHING
    }

    // data vectors
    @Nullable
    private String platformName;
    @Nullable
    private Path dataPath;
    @Nullable
    private SingleCellDataType dataType;
    @Nullable
    private String qtName;
    @Nullable
    private String newName;
    @Nullable
    private StandardQuantitationType newType;
    @Nullable
    private ScaleType newScaleType;
    private boolean recomputedFromRawData;
    private boolean preferSinglePrecision;
    private boolean preferredQt;
    private boolean replaceQt;

    private boolean ignoreSamplesLackingData;

    // cell type assignment and cell-level characteristics
    @Nullable
    private Path cellTypeAssignmentFile;
    @Nullable
    private String cellTypeAssignmentName;
    @Nullable
    private String cellTypeAssignmentDescription;
    @Nullable
    private String cellTypeAssignmentProtocolName;
    private boolean preferredCellTypeAssignment;
    private boolean replaceExistingCellTypeAssignments;
    @Nullable
    private Path otherCellLevelCharacteristicsFile;
    @Nullable
    private List<String> otherCellLevelCharacteristicsNames;
    private boolean replaceExistingOtherCellLevelCharacteristics;
    private boolean inferSamplesFromCellIdsOverlap;
    private boolean ignoreUnmatchedCellIds;
    @Nullable
    private Path renamingFile;
    @Nullable
    private Integer transformThreads;

    @Nullable
    private Boolean replaceCellTypeFactor;

    // sequencing metadata
    @Nullable
    private Path sequencingMetadataFile;
    @Nullable
    private Integer sequencingReadLength;
    @Nullable
    private Boolean sequencingIsPaired;

    // AnnData
    @Nullable
    private String annDataSampleFactorName;
    @Nullable
    private String annDataCellTypeFactorName;
    @Nullable
    private String annDataUnknownCellTypeIndicator;
    private boolean annDataIgnoreCellTypeFactor;
    @Nullable
    private Boolean annDataTranspose;
    @Nullable
    private Boolean annDataUseRawX;

    // MEX
    private boolean mexAllowMappingDesignElementsToGeneSymbols;
    private boolean mexUseDoublePrecision;
    @Nullable
    private Boolean mex10xFilter;
    @Nullable
    private String mex10xChemistry;

    // options for streaming vectors when writing data to disk
    private boolean useStreaming;
    private int fetchSize;
    private boolean useCursorFetchIfSupported;

    @Nullable
    private ExecutorService transformExecutor;

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
        options.addOption( LOAD_SEQUENCING_METADATA_OPTION, "load-sequencing-metadata", false, "Load sequencing metadata." );

        addEnumOption( options, DATA_TYPE_OPTION, "data-type", "Data type to import.", SingleCellDataType.class );
        options.addOption( Option.builder( DATA_PATH_OPTION )
                .longOpt( "data-path" )
                .hasArg()
                .type( Path.class )
                .desc( "Load single-cell data from the given path instead of looking up the download directory. For AnnData and Seurat Disk, it is a file. For MEX it is a directory. Requires the " + formatOption( options, DATA_TYPE_OPTION ) + " option to be set." )
                .get() );
        addGenericPlatformOption( options, PLATFORM_OPTION, "platform", "Target platform (must already exist in the system)" );
        options.addOption( QT_NAME_OPTION, "quantitation-type-name", true, "Quantitation type to import (optional, use if more than one is present in data)" );
        options.addOption( QT_NEW_NAME_OPTION, "quantitation-type-new-name", true, "New name to use for the imported quantitation type (optional, defaults to the data)" );
        addEnumOption( options, QT_NEW_TYPE_OPTION, "quantitation-type-new-type", "New type to use for the imported quantitation type (optional, defaults to the data)", StandardQuantitationType.class );
        addEnumOption( options, QT_NEW_SCALE_TYPE_OPTION, "quantitation-type-new-scale-type", "New scale type to use for the imported quantitation type (optional, defaults to the data)", ScaleType.class );
        options.addOption( QT_RECOMPUTED_FROM_RAW_DATA_OPTION, "quantitation-type-recomputed-from-raw-data", false, "Mark the loaded QT as recomputed from raw data." );
        options.addOption( PREFERRED_QT_OPTION, "preferred-quantitation-type", false, "Make the quantitation type the preferred one." );
        options.addOption( PREFER_SINGLE_PRECISION, "prefer-single-precision", false, "Prefer single precision for storage, even if the data is available with double precision. This reduces the size of vectors and thus the storage requirement." );
        options.addOption( REPLACE_OPTION, "replace", false, "Replace an existing quantitation type." );
        options.addOption( IGNORE_SAMPLES_LACKING_DATA_OPTION, "ignore-samples-lacking-data", false, "Ignore samples that lack data. Those samples will not be included in the single-cell dimension." );
        options.addOption( Option.builder( TRANSFORM_THREADS_OPTION ).longOpt( "transform-threads" ).hasArg().type( Integer.class ).desc( "Number of threads to use for transforming single-cell data (e.g. filtering low quality cells)." ).get() );

        // for all loaders
        options.addOption( Option.builder( RENAMING_FILE_OPTION )
                .longOpt( "renaming-file" )
                .hasArg().type( Path.class )
                .desc( "File containing sample a renaming scheme. The format is a two-column TSV with the first column containing author-provided sample names and the second column suitable assay identifiers (i.e. name, GEO accessions)." )
                .get() );

        // for the generic metadata loader
        options.addOption( Option.builder( CELL_TYPE_ASSIGNMENT_FILE_OPTION )
                .longOpt( "cell-type-assignment-file" )
                .hasArg().type( Path.class )
                .desc( "Path to a cell type assignment file. If missing, cell type importing will be delegated to the loader implementation." )
                .get() );
        options.addOption( CELL_TYPE_ASSIGNMENT_NAME_OPTION, "cell-type-assignment-name", true, "Name to use for the cell type assignment. The " + formatOption( options, CELL_TYPE_ASSIGNMENT_FILE_OPTION ) + " option must be set." );
        options.addOption( CELL_TYPE_ASSIGNMENT_DESCRIPTION_OPTION, "cell-type-assignment-description", true, "Description to use for the cell type assignment. The " + formatOption( options, CELL_TYPE_ASSIGNMENT_FILE_OPTION ) + " option must be set." );
        options.addOption( Option.builder( CELL_TYPE_ASSIGNMENT_PROTOCOL_NAME_OPTION )
                .longOpt( "cell-type-assignment-protocol" ).hasArg()
                .converter( EnumeratedByCommandStringConverter.of( CompletionUtils.generateCompleteCommand( CompletionType.PROTOCOL ) ) )
                .desc( "An identifier for a protocol describing the cell type assignment. This require the " + formatOption( options, CELL_TYPE_ASSIGNMENT_FILE_OPTION ) + " option to be set." )
                .get() );
        options.addOption( REPLACE_CELL_TYPE_ASSIGNMENT_OPTION, "replace-cell-type-assignment", false, String.format( "Replace an existing cell type assignment with the same name. The %s and %s options must be set.", formatOption( options, CELL_TYPE_ASSIGNMENT_FILE_OPTION ), formatOption( options, CELL_TYPE_ASSIGNMENT_NAME_OPTION ) ) );
        options.addOption( PREFERRED_CELL_TYPE_ASSIGNMENT_OPTION, "preferred-cell-type-assignment", false, "Make the cell type assignment the preferred one. The " + formatOption( options, CELL_TYPE_ASSIGNMENT_FILE_OPTION ) + " option must be set." );
        options.addOption( Option.builder( OTHER_CELL_LEVEL_CHARACTERISTICS_FILE )
                .longOpt( "cell-level-characteristics-file" )
                .hasArg().type( Path.class )
                .desc( "Path to a file containing additional cell-level characteristics to import." )
                .get() );
        options.addOption( Option.builder( OTHER_CELL_LEVEL_CHARACTERISTICS_NAME ).longOpt( "cell-level-characteristics-name" )
                .hasArgs()
                .valueSeparator( ',' )
                .desc( "Name to use for the CLC. If the file contains more than one CLC, multiple names can be provided using ',' as a delimiter." )
                .get() );
        options.addOption( REPLACE_OTHER_CELL_LEVEL_CHARACTERISTICS_OPTION, "replace-cell-level-characteristics", false,
                String.format( "Replace existing cell-level characteristics with the same names. The %s and %s options must be set.", formatOption( options, OTHER_CELL_LEVEL_CHARACTERISTICS_FILE ), formatOption( options, OTHER_CELL_LEVEL_CHARACTERISTICS_NAME ) ) );
        options.addOption( INFER_SAMPLES_FROM_CELL_IDS_OVERLAP_OPTION, "infer-samples-from-cell-ids-overlap", false, "Infer sample names from cell IDs overlap." );
        options.addOption( IGNORE_UNMATCHED_CELL_IDS_OPTION, "ignore-unmatched-cell-ids", false, "Ignore unmatched cell IDs when loading cell type assignments and other cell-level characteristics." );

        // for the cell type factor
        OptionsUtils.addAutoOption( options,
                REPLACE_CELL_TYPE_FACTOR_OPTION, "replace-cell-type-factor", "Replace the existing cell type factor even if is compatible with the new preferred cell type assignment. If no cell type factor exists, it will be created.",
                KEEP_CELL_TYPE_FACTOR_OPTION, "keep-cell-type-factor", "Keep the existing cell type factor as-is even if it becomes misaligned with the preferred cell type assignment. If no cell type factor exists, it will be created.",
                "The default is to re-create the cell type factor if necessary." );

        options.addOption( Option.builder( SEQUENCING_READ_LENGTH_OPTION )
                .longOpt( "sequencing-read-length" )
                .hasArg().type( Integer.class )
                .desc( "Read length to use for the imported sequencing metadata." )
                .get() );
        OptionsUtils.addAutoOption( options,
                SEQUENCING_IS_PAIRED_OPTION, "sequencing-is-paired",
                "Indicate that the sequencing data is paired.",
                SEQUENCING_IS_SINGLE_END_OPTION, "sequencing-is-single-end",
                "Indicate that the sequencing data is single-end." );
        options.addOption( Option.builder( SEQUENCING_METADATA_FILE_OPTION )
                .longOpt( "sequencing-metadata-file" )
                .hasArg().type( Path.class )
                .desc( "Path to a file containing sequencing metadata to import. These values will override defaults set by " + formatOption( options, SEQUENCING_READ_LENGTH_OPTION ) + " and " + formatOption( options, SEQUENCING_IS_PAIRED_OPTION ) + "." )
                .get() );

        // for AnnData
        options.addOption( ANNDATA_SAMPLE_FACTOR_NAME_OPTION, "anndata-sample-factor-name", true, "Name of the factor used for the sample name." );
        options.addOption( ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION, "anndata-cell-type-factor-name", true, "Name of the factor used for the cell type, incompatible with " + formatOption( options, CELL_TYPE_ASSIGNMENT_FILE_OPTION ) + "." );
        options.addOption( ANNDATA_IGNORE_CELL_TYPE_FACTOR_OPTION, "anndata-ignore-cell-type-factor", false, "Do not attempt to load a cell type factor. Incompatible with " + formatOption( options, ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION ) + "." );
        options.addOption( ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION, "anndata-unknown-cell-type-indicator", true, "Indicator used for missing cell type. Defaults to using the standard -1 categorical code." );
        OptionsUtils.addAutoOption( options,
                ANNDATA_USE_RAW_X_OPTION, "anndata-use-raw-x", "Use raw.X",
                ANNDATA_USE_X_OPTION, "anndata-use-x", "Use X." );
        OptionsUtils.addAutoOption( options,
                ANNDATA_TRANSPOSE_OPTION, "anndata-transpose", "Transpose the data matrix.",
                ANNDATA_NO_TRANSPOSE_OPTION, "anndata-no-transpose", "Do not transpose the data matrix." );

        // for MEX
        options.addOption( MEX_ALLOW_MAPPING_DESIGN_ELEMENTS_TO_GENE_SYMBOLS_OPTION, "mex-allow-mapping-design-elements-to-gene-symbols", false, "Allow mapping probe names to gene symbols when loading MEX data (i.e. the second column in features.tsv.gz)." );
        options.addOption( MEX_USE_DOUBLE_PRECISION_OPTION, "mex-use-double-precision", false, "Use double precision (i.e. double and long) for storing vectors" );
        OptionsUtils.addAutoOption( options,
                MEX_10X_FILTER_OPTION, "mex-10x-filter", "Apply the 10x MEX filter.",
                MEX_NO_10X_FILTER_OPTION, "mex-no-10x-filter", "Do not apply the 10x MEX filter." );
        options.addOption( MEX_10X_CHEMISTRY_OPTION, "mex-10x-chemistry", true, "10x chemistry to use for filtering data." );

        options.addOption( "noStreaming", "no-streaming", false, "Use in-memory storage instead of streaming for retrieving and writing vectors." );
        options.addOption( Option.builder( "fetchSize" ).longOpt( "fetch-size" ).hasArg( true ).type( Integer.class ).desc( "Fetch size to use when retrieving vectors, incompatible with " + formatOption( options, "noStreaming" ) + "." ).get() );
        options.addOption( "noCursorFetch", "no-cursor-fetch", false, "Disable cursor fetching on the database server and produce results immediately. This is incompatible with " + formatOption( options, "noStreaming" ) + "." );
    }

    @Override
    protected void doAuthenticatedWork() throws Exception {
        if ( transformThreads != null ) {
            log.info( "Using " + transformThreads + " for transforming single-cell data." );
            transformExecutor = Executors.newFixedThreadPool( transformThreads, new SimpleThreadFactory( "gemma-single-cell-transform-thread-" ) );
        } else {
            transformExecutor = null;
        }
        try {
            super.doAuthenticatedWork();
        } finally {
            if ( transformExecutor != null ) {
                transformExecutor.shutdown();
                transformExecutor = null;
            }
        }
    }

    @Override
    protected void processExperimentOptions( CommandLine commandLine ) throws ParseException {
        if ( hasOption( commandLine, LOAD_CELL_TYPE_ASSIGNMENT_OPTION,
                noneOf( toBeSet( LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION ), toBeSet( LOAD_SEQUENCING_METADATA_OPTION ) ) ) ) {
            mode = Mode.LOAD_CELL_TYPE_ASSIGNMENTS;
            dataType = SingleCellDataType.NULL;
        } else if ( hasOption( commandLine, LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION,
                noneOf( toBeSet( LOAD_CELL_TYPE_ASSIGNMENT_OPTION ), toBeSet( LOAD_SEQUENCING_METADATA_OPTION ) ) ) ) {
            mode = Mode.LOAD_CELL_LEVEL_CHARACTERISTICS;
            dataType = SingleCellDataType.NULL;
        } else if ( hasOption( commandLine, LOAD_SEQUENCING_METADATA_OPTION,
                noneOf( toBeSet( LOAD_CELL_TYPE_ASSIGNMENT_OPTION ), toBeSet( LOAD_CELL_LEVEL_CHARACTERISTICS_OPTION ) ) ) ) {
            mode = Mode.LOAD_SEQUENCING_METADATA;
            dataType = SingleCellDataType.NULL;
        } else {
            mode = Mode.LOAD_EVERYTHING;
            dataType = null; // this will auto-datect the data type
            platformName = commandLine.getOptionValue( PLATFORM_OPTION );
            if ( platformName == null ) {
                throw new MissingOptionException( "The -" + PLATFORM_OPTION + " option is required when loading vectors." );
            }
        }
        if ( commandLine.hasOption( DATA_TYPE_OPTION ) ) {
            dataType = commandLine.getParsedOptionValue( DATA_TYPE_OPTION );
        }
        dataPath = getParsedOptionValue( commandLine, DATA_PATH_OPTION, requires( toBeSet( DATA_TYPE_OPTION ) ) );
        qtName = commandLine.getOptionValue( QT_NAME_OPTION );
        newName = commandLine.getOptionValue( QT_NEW_NAME_OPTION );
        if ( commandLine.hasOption( QT_NEW_TYPE_OPTION ) ) {
            newType = commandLine.getParsedOptionValue( QT_NEW_TYPE_OPTION );
        } else {
            newType = null;
        }
        if ( commandLine.hasOption( QT_NEW_SCALE_TYPE_OPTION ) ) {
            newScaleType = commandLine.getParsedOptionValue( QT_NEW_SCALE_TYPE_OPTION );
        } else {
            newScaleType = null;
        }
        recomputedFromRawData = commandLine.hasOption( QT_RECOMPUTED_FROM_RAW_DATA_OPTION );
        preferSinglePrecision = commandLine.hasOption( PREFER_SINGLE_PRECISION );
        preferredQt = commandLine.hasOption( PREFERRED_QT_OPTION );
        replaceQt = commandLine.hasOption( REPLACE_OPTION );
        renamingFile = commandLine.getParsedOptionValue( RENAMING_FILE_OPTION );
        ignoreSamplesLackingData = commandLine.hasOption( IGNORE_SAMPLES_LACKING_DATA_OPTION );
        transformThreads = commandLine.getParsedOptionValue( TRANSFORM_THREADS_OPTION );

        // CTAs
        cellTypeAssignmentFile = commandLine.getParsedOptionValue( CELL_TYPE_ASSIGNMENT_FILE_OPTION );
        cellTypeAssignmentName = getOptionValue( commandLine, CELL_TYPE_ASSIGNMENT_NAME_OPTION, requires( toBeSet( CELL_TYPE_ASSIGNMENT_FILE_OPTION ) ) );
        cellTypeAssignmentDescription = getOptionValue( commandLine, CELL_TYPE_ASSIGNMENT_DESCRIPTION_OPTION, requires( toBeSet( CELL_TYPE_ASSIGNMENT_FILE_OPTION ) ) );
        cellTypeAssignmentProtocolName = getOptionValue( commandLine, CELL_TYPE_ASSIGNMENT_PROTOCOL_NAME_OPTION, requires( toBeSet( CELL_TYPE_ASSIGNMENT_FILE_OPTION ) ) );
        preferredCellTypeAssignment = hasOption( commandLine, PREFERRED_CELL_TYPE_ASSIGNMENT_OPTION, requires( toBeSet( CELL_TYPE_ASSIGNMENT_FILE_OPTION ) ) );
        replaceExistingCellTypeAssignments = hasOption( commandLine, REPLACE_CELL_TYPE_ASSIGNMENT_OPTION, requires( allOf( toBeSet( CELL_TYPE_ASSIGNMENT_FILE_OPTION ), toBeSet( CELL_TYPE_ASSIGNMENT_NAME_OPTION ) ) ) );

        // CLCs
        if ( commandLine.hasOption( OTHER_CELL_LEVEL_CHARACTERISTICS_NAME ) ) {
            otherCellLevelCharacteristicsNames = Arrays.asList( commandLine.getOptionValues( OTHER_CELL_LEVEL_CHARACTERISTICS_NAME ) );
        } else {
            otherCellLevelCharacteristicsNames = null;
        }
        otherCellLevelCharacteristicsFile = commandLine.getParsedOptionValue( OTHER_CELL_LEVEL_CHARACTERISTICS_FILE );
        replaceExistingOtherCellLevelCharacteristics = hasOption( commandLine, REPLACE_OTHER_CELL_LEVEL_CHARACTERISTICS_OPTION,
                requires( allOf( toBeSet( OTHER_CELL_LEVEL_CHARACTERISTICS_FILE ), toBeSet( OTHER_CELL_LEVEL_CHARACTERISTICS_NAME ) ) ) );

        // applies to both cell type assignments and other cell-level characteristics
        inferSamplesFromCellIdsOverlap = hasOption( commandLine, INFER_SAMPLES_FROM_CELL_IDS_OVERLAP_OPTION,
                requires( anyOf( toBeSet( CELL_TYPE_ASSIGNMENT_FILE_OPTION ), toBeSet( OTHER_CELL_LEVEL_CHARACTERISTICS_FILE ) ) ) );
        ignoreUnmatchedCellIds = hasOption( commandLine, IGNORE_UNMATCHED_CELL_IDS_OPTION,
                requires( anyOf( toBeSet( CELL_TYPE_ASSIGNMENT_FILE_OPTION ), toBeSet( OTHER_CELL_LEVEL_CHARACTERISTICS_FILE ) ) ) );

        // cell type factor
        replaceCellTypeFactor = getAutoOptionValue( commandLine, REPLACE_CELL_TYPE_FACTOR_OPTION, KEEP_CELL_TYPE_FACTOR_OPTION );

        // sequencing metadata
        sequencingMetadataFile = commandLine.getParsedOptionValue( SEQUENCING_METADATA_FILE_OPTION );
        sequencingReadLength = commandLine.getParsedOptionValue( SEQUENCING_READ_LENGTH_OPTION );
        sequencingIsPaired = getAutoOptionValue( commandLine, SEQUENCING_IS_PAIRED_OPTION, SEQUENCING_IS_SINGLE_END_OPTION );

        // data-type specific options
        rejectInvalidOptionsForDataType( commandLine, dataType );
        if ( dataType == SingleCellDataType.ANNDATA ) {
            annDataSampleFactorName = commandLine.getOptionValue( ANNDATA_SAMPLE_FACTOR_NAME_OPTION );
            annDataCellTypeFactorName = commandLine.getOptionValue( ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION );
            annDataUnknownCellTypeIndicator = commandLine.getOptionValue( ANNDATA_UNKNOWN_CELL_TYPE_INDICATOR_OPTION );
            annDataIgnoreCellTypeFactor = commandLine.hasOption( ANNDATA_IGNORE_CELL_TYPE_FACTOR_OPTION );
            annDataTranspose = getAutoOptionValue( commandLine, ANNDATA_TRANSPOSE_OPTION, ANNDATA_NO_TRANSPOSE_OPTION );
            annDataUseRawX = getAutoOptionValue( commandLine, ANNDATA_USE_RAW_X_OPTION, ANNDATA_USE_X_OPTION );
            if ( cellTypeAssignmentFile != null && annDataCellTypeFactorName != null ) {
                throw new ParseException( String.format( "The -%s option would override the value of -%s.",
                        CELL_TYPE_ASSIGNMENT_FILE_OPTION, ANNDATA_CELL_TYPE_FACTOR_NAME_OPTION ) );
            }
        } else if ( dataType == SingleCellDataType.MEX ) {
            mexAllowMappingDesignElementsToGeneSymbols = commandLine.hasOption( MEX_ALLOW_MAPPING_DESIGN_ELEMENTS_TO_GENE_SYMBOLS_OPTION );
            mexUseDoublePrecision = commandLine.hasOption( MEX_USE_DOUBLE_PRECISION_OPTION );
            mex10xFilter = getAutoOptionValue( commandLine, MEX_10X_FILTER_OPTION, MEX_NO_10X_FILTER_OPTION );
            mex10xChemistry = commandLine.getOptionValue( MEX_10X_CHEMISTRY_OPTION );
        }

        if ( commandLine.hasOption( "noStreaming" ) && commandLine.hasOption( "fetchSize" ) ) {
            throw new ParseException( "Cannot use -noStreaming/--no-streaming and -fetchSize/--fetch-size at the same time." );
        }
        if ( commandLine.hasOption( "noStreaming" ) && commandLine.hasOption( "noCursorFetch" ) ) {
            throw new ParseException( "Cannot use -noStreaming/--no-streaming and -noCursorFetch/--no-cursor-fetch at the same time." );
        }
        this.useStreaming = !commandLine.hasOption( "noStreaming" );
        this.fetchSize = commandLine.getParsedOptionValue( "fetchSize", 30 );
        this.useCursorFetchIfSupported = !commandLine.hasOption( "noCursorFetch" );
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
    protected void processExpressionExperiments( Collection<ExpressionExperiment> expressionExperiments ) {
        if ( dataPath != null || qtName != null || cellTypeAssignmentFile != null || otherCellLevelCharacteristicsFile != null || sequencingMetadataFile != null ) {
            throw new IllegalArgumentException( "Cannot specify a data path, quantitation type name, cell type assignment file, cell-level characteristics file or sequencing metadata file when processing more than one experiment." );
        }
        super.processExpressionExperiments( expressionExperiments );
    }

    @Override
    protected void processExpressionExperiment( ExpressionExperiment ee ) {
        ee = eeService.thawLite( ee );
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
            case LOAD_SEQUENCING_METADATA:
                Map<BioAssay, SequencingMetadata> sm = singleCellDataLoaderService.loadSequencingMetadata( ee, config );
                if ( !sm.isEmpty() ) {
                    addSuccessObject( ee, "Loaded sequencing metadata for " + sm.size() + " assays." );
                } else {
                    addErrorObject( ee, "No sequencing metadata were loaded." );
                }
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
                    try ( LockedPath lockedPath = expressionDataFileService.writeOrLocateMexSingleCellExpressionData( ee, qt, useStreaming ? fetchSize : -1, useCursorFetchIfSupported, true, getCliContext().getConsole() ) ) {
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
                try {
                    refreshExpressionExperimentFromGemmaWeb( ee, true, false );
                } catch ( Exception e ) {
                    addWarningObject( ee, "Failed to refresh dataset from Gemma Web", e );
                }
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
                    .ignoreCellTypeFactor( annDataIgnoreCellTypeFactor )
                    .unknownCellTypeIndicator( annDataUnknownCellTypeIndicator )
                    .transpose( annDataTranspose )
                    .useRawX( annDataUseRawX );
        } else if ( dataType == SingleCellDataType.MEX ) {
            configBuilder = MexSingleCellDataLoaderConfig.builder()
                    .allowMappingDesignElementsToGeneSymbols( mexAllowMappingDesignElementsToGeneSymbols )
                    .apply10xFilter( mex10xFilter )
                    .use10xChemistry( mex10xChemistry )
                    .useDoublePrecision( mexUseDoublePrecision );
        } else {
            configBuilder = SingleCellDataLoaderConfig.builder();
        }
        if ( dataPath != null ) {
            configBuilder.dataPath( dataPath );
        }
        if ( qtName != null ) {
            configBuilder
                    .quantitationTypeName( qtName );
        }
        configBuilder
                .ignoreSamplesLackingData( ignoreSamplesLackingData )
                .replaceExistingQuantitationType( replaceQt )
                .quantitationTypeNewName( newName )
                .quantitationTypeNewType( newType )
                .quantitationTypeNewScaleType( newScaleType )
                .markQuantitationTypeAsRecomputedFromRawData( recomputedFromRawData )
                .preferSinglePrecision( preferSinglePrecision )
                .markQuantitationTypeAsPreferred( preferredQt )
                .transformExecutor( transformExecutor )
                .console( getCliContext().getConsole() );
        if ( renamingFile != null ) {
            configBuilder.renamingFile( renamingFile );
        }
        if ( cellTypeAssignmentFile != null ) {
            configBuilder
                    .cellTypeAssignmentFile( cellTypeAssignmentFile )
                    .cellTypeAssignmentName( cellTypeAssignmentName )
                    .cellTypeAssignmentDescription( cellTypeAssignmentDescription )
                    .cellTypeAssignmentProtocol( cellTypeAssignmentProtocolName != null ? entityLocator.locateProtocol( cellTypeAssignmentProtocolName ) : null )
                    .replaceExistingCellTypeAssignment( replaceExistingCellTypeAssignments )
                    .markSingleCellTypeAssignmentAsPreferred( preferredCellTypeAssignment );
        }

        if ( otherCellLevelCharacteristicsFile != null ) {
            configBuilder
                    .otherCellLevelCharacteristicsFile( otherCellLevelCharacteristicsFile )
                    .otherCellLevelCharacteristicsNames( otherCellLevelCharacteristicsNames )
                    .replaceExistingOtherCellLevelCharacteristics( replaceExistingOtherCellLevelCharacteristics );
        }
        // infer only on-demand
        configBuilder.inferSamplesFromCellIdsOverlap( inferSamplesFromCellIdsOverlap );
        // always allow for using barcodes to infer the sample names from the CLI
        configBuilder.useCellIdsIfSampleNameIsMissing( true );
        // ignore only on-demand
        configBuilder.ignoreUnmatchedCellIds( ignoreUnmatchedCellIds );
        // cell type factor options
        configBuilder
                // the default is to recreate if necessary
                .recreateCellTypeFactorIfNecessary( replaceCellTypeFactor == null || replaceCellTypeFactor )
                .ignoreCompatibleCellTypeFactor( replaceCellTypeFactor != null && replaceCellTypeFactor );
        if ( sequencingMetadataFile != null ) {
            configBuilder.sequencingMetadataFile( sequencingMetadataFile );
        }
        if ( sequencingReadLength != null || sequencingIsPaired != null ) {
            configBuilder.defaultSequencingMetadata( SequencingMetadata.builder()
                    .readLength( sequencingReadLength )
                    .isPaired( sequencingIsPaired )
                    .build() );
        }
        return configBuilder.build();
    }
}
