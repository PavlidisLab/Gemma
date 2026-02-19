package ubic.gemma.cli.options;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.cli.*;
import org.springframework.util.Assert;
import ubic.gemma.cli.util.OptionsUtils;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.common.quantitationtype.ScaleType;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.BiConsumer;

/**
 * Utilities to add and process options for data files (raw data, processed data, designs, etc.).
 *
 * @author poirigui
 */
@CommonsLog
public class DataFileOptionsUtils {

    public static final String OUTPUT_FILE_OPTION = "o";
    public static final String OUTPUT_DIR_OPTION = "d";
    public static final String STANDARD_LOCATION_OPTION = "standardLocation";
    public static final String STANDARD_OUTPUT_OPTION = "stdout";
    public static final String EXCLUDE_SAMPLE_IDENTIFIERS_OPTION = "excludeSampleIdentifiers";
    public static final String USE_BIO_ASSAY_IDS_OPTION = "useBioAssayIds";
    public static final String USE_RAW_COLUMN_NAMES_OPTION = "useRawColumnNames";
    public static final String SCALE_TYPE_OPTION = "scaleType";
    public static final String USE_MULTIPLE_ROWS_FOR_ASSAYS_OPTION = "useMultipleRowsForAssays";
    public static final String SEPARATE_SAMPLE_FROM_ASSAYS_IDENTIFIERS_OPTION = "separateSampleFromAssayIdentifiers";
    public static final String USE_ENSEMBL_IDS_OPTION = "useEnsemblIds";

    public static void addDataFileOptions( Options options, String what, boolean allowStandardLocation, BiConsumer<OptionGroup, Option> addSingleOption ) {
        addDataFileOptions( options, what, allowStandardLocation, true, true, false, true, addSingleOption );
    }

    /**
     * Add options for writing data files, such as raw or processed data files.
     *
     * @param allowStandardLocation if true, the standard location option will be added
     * @param allowFile             if true, the output file option will be added, otherwise only the output directory
     *                              will be added
     * @param allowDirectory        if true, the output directory option will be added
     * @param allowCurrentDirectory if true, writing to the current directory will be allowed
     * @param allowStdout           if true, the standard output option will be added
     * @param addSingleOption       a function that adds a single option to the given option group; this is needed
     *                              because a CLI might want to track which options apply to individual entities
     *                              separately.
     * @see ubic.gemma.core.analysis.service.ExpressionDataFileService
     * @see ubic.gemma.core.analysis.service.ExpressionDataFileUtils
     */
    public static void addDataFileOptions( Options options, String what, boolean allowStandardLocation, boolean allowFile,
            boolean allowDirectory, boolean allowCurrentDirectory, boolean allowStdout,
            BiConsumer<OptionGroup, Option> addSingleOption ) {
        Assert.isTrue( !allowCurrentDirectory || allowDirectory, "Cannot allow current directory without allowing output directory." );
        OptionGroup og = new OptionGroup();
        if ( allowFile ) {
            addSingleOption.accept( og, Option.builder( OUTPUT_FILE_OPTION )
                    .longOpt( "output-file" ).hasArg().type( Path.class )
                    .desc( "Write " + what + " to the given output file." ).get() );
        }
        if ( allowDirectory ) {
            og.addOption( Option.builder( OUTPUT_DIR_OPTION )
                    .longOpt( "output-dir" ).hasArg().type( Path.class )
                    .desc( "Write " + what + " inside the given directory." + ( !allowStandardLocation && allowCurrentDirectory && !allowStdout ? " Defaults to the current directory of no other destination is selected." : "" ) )
                    .get() );
        }
        if ( allowStandardLocation ) {
            og.addOption( Option.builder( STANDARD_LOCATION_OPTION )
                    .longOpt( "standard-location" )
                    .desc( "Write " + what + " to the standard location under ${gemma.appdata.home}/dataFiles. This is the default if no other destination is selected." )
                    .get() );
        }
        if ( allowStdout ) {
            addSingleOption.accept( og, Option.builder( STANDARD_OUTPUT_OPTION )
                    .longOpt( "stdout" )
                    .desc( "Write " + what + " to standard output." + ( !allowStandardLocation && !allowCurrentDirectory ? " This is the default if no other destination is selected." : "" ) )
                    .get() );
        }
        options.addOptionGroup( og );
    }

    public static void addExpressionDataFileOptions( Options options, String what, boolean allowStandardLocation,
            BiConsumer<OptionGroup, Option> addSingleOption ) {
        addDataFileOptions( options, what, allowStandardLocation, addSingleOption );
        options.addOption( EXCLUDE_SAMPLE_IDENTIFIERS_OPTION, "exclude-sample-identifiers", false, "Only include bioassays identifier in the output instead of mangling it with the sample identifier." );
        options.addOption( USE_BIO_ASSAY_IDS_OPTION, "use-bioassay-ids", false, "Use IDs instead of names or short names for bioassays and samples." );
        options.addOption( USE_RAW_COLUMN_NAMES_OPTION, "use-raw-column-names", false, "Use raw column names instead of R-friendly ones." );
        OptionsUtils.addEnumOption( options, SCALE_TYPE_OPTION, "scale-type", "Scale type to use for the data. This is incompatible with -standardLocation/--standard-location.", ScaleType.class );
    }

    public static void addSingleCellExpressionDataFileOptions( Options options, String what, boolean allowStandardLocation,
            BiConsumer<OptionGroup, Option> addSingleOption ) {
        addExpressionDataFileOptions( options, what, allowStandardLocation, addSingleOption );
        options.addOption( USE_ENSEMBL_IDS_OPTION, "use-ensembl-ids", false, "Use Ensembl IDs instead of official gene symbols (only for MEX output). This is incompatible with -standardLocation/--standard-location." );
    }

    public static void addDesignFileOptions( Options options, String what, boolean allowStandardLocation, BiConsumer<OptionGroup, Option> addSingleOption ) {
        addDataFileOptions( options, what, allowStandardLocation, addSingleOption );
        options.addOption( USE_BIO_ASSAY_IDS_OPTION, "use-bioassay-ids", false, "Use IDs instead of names or short names for bioassays and samples." );
        options.addOption( USE_RAW_COLUMN_NAMES_OPTION, "use-raw-column-names", false, "Use raw column names instead of R-friendly ones." );
        options.addOption( USE_MULTIPLE_ROWS_FOR_ASSAYS_OPTION, "use-multiple-rows-for-assays", false, "Use multiple rows for assays." );
        options.addOption( SEPARATE_SAMPLE_FROM_ASSAYS_IDENTIFIERS_OPTION, "separate-sample-from-assays-identifiers", false,
                "Separate sample and assay(s) identifiers in distinct columns named 'Sample' and 'Assays' (instead of a single 'Bioassay' column). The assays will be delimited by a '" + TsvUtils.SUB_DELIMITER + "' character." );
    }

    public static void addCellMetadataFileOptions( Options options, String what, boolean allowStandardLocation, BiConsumer<OptionGroup, Option> addSingleOption ) {
        addDataFileOptions( options, what, allowStandardLocation, addSingleOption );
        options.addOption( USE_BIO_ASSAY_IDS_OPTION, "use-bioassay-ids", false, "Use IDs instead of names or short names for bioassays and samples." );
        options.addOption( USE_RAW_COLUMN_NAMES_OPTION, "use-raw-column-names", false, "Use raw column names instead of R-friendly ones." );
        options.addOption( SEPARATE_SAMPLE_FROM_ASSAYS_IDENTIFIERS_OPTION, "separate-sample-from-assays-identifiers", false,
                "Separate sample and assay(s) identifiers in distinct columns named 'Sample' and 'Assays' (instead of a single 'Bioassay' column). The assays will be delimited by a '" + TsvUtils.SUB_DELIMITER + "' character." );
    }

    public static DataFileOptionValue getDataFileOptionValue( CommandLine commandLine, boolean allowStandardLocation, String forceOption, boolean force ) throws ParseException {
        return getDataFileOptionValue( commandLine, allowStandardLocation, true, true, forceOption, force );
    }

    /**
     * Obtain the result of the data file options added by {@link #addDataFileOptions(Options, String, boolean, boolean, boolean, boolean, boolean, BiConsumer)}
     *
     * @param allowStandardLocation if true, the standard location option will be considered and used as a default if no
     *                              other destination is selected. Otherwise, standard output will be used as a default.
     * @param allowStdout           if true, the standard output option will be considered and used as a default if no
     *                              other destination is selected. Otherwise, the current directory will be used as a
     *                              default.
     * @param allowCurrentDirectory if true, the current directory will be used as a default if no other destination is
     *                              selected. Note that a file can be written to inside that directory via
     *                              {@link DataFileOptionValue#getOutputFile(String)}
     * @throws MissingOptionException if no destination is selected and no default location is allowed.
     */
    public static DataFileOptionValue getDataFileOptionValue( CommandLine commandLine,
            boolean allowStandardLocation, boolean allowStdout, boolean allowCurrentDirectory, String forceOption, boolean force ) throws ParseException {
        if ( !OptionsUtils.hasAnyOption( commandLine, STANDARD_LOCATION_OPTION, STANDARD_OUTPUT_OPTION, OUTPUT_FILE_OPTION, OUTPUT_DIR_OPTION ) ) {
            if ( allowStandardLocation ) {
                log.debug( "No data file options provided, defaulting to -standardLocation/--standard-location." );
                return new DataFileOptionValue( true, false, null, null, forceOption, force );
            } else if ( allowStdout ) {
                log.debug( "No data file options provided, defaulting to -standardOutput/--standard-output." );
                return new DataFileOptionValue( false, true, null, null, forceOption, force );
            } else if ( allowCurrentDirectory ) {
                log.debug( "No data file options provided, defaulting to the current directory." );
                return new DataFileOptionValue( false, true, null, Paths.get( "" ), forceOption, force );
            } else {
                throw new MissingOptionException( "No destination is selected and no default location is allowed." );
            }
        }
        return new DataFileOptionValue(
                commandLine.hasOption( STANDARD_LOCATION_OPTION ),
                commandLine.hasOption( STANDARD_OUTPUT_OPTION ),
                commandLine.getParsedOptionValue( OUTPUT_FILE_OPTION ),
                commandLine.getParsedOptionValue( OUTPUT_DIR_OPTION ), forceOption, force );
    }

    public static ExpressionDataFileOptionValue getExpressionDataFileOptionValue( CommandLine commandLine,
            boolean allowStandardLocation, String forceOption, boolean force ) throws ParseException {
        DataFileOptionValue dfvo = getDataFileOptionValue( commandLine, allowStandardLocation, forceOption, force );
        return new ExpressionDataFileOptionValue( dfvo,
                commandLine.hasOption( EXCLUDE_SAMPLE_IDENTIFIERS_OPTION ),
                commandLine.hasOption( USE_BIO_ASSAY_IDS_OPTION ),
                commandLine.hasOption( USE_RAW_COLUMN_NAMES_OPTION ),
                commandLine.getParsedOptionValue( SCALE_TYPE_OPTION )
        );
    }

    public static SingleCellExpressionDataFileOptionValue getSingleCellExpressionDataFileOptionValue( CommandLine commandLine,
            boolean allowStandardLocation, String forceOption, boolean force ) throws ParseException {
        ExpressionDataFileOptionValue dfvo = getExpressionDataFileOptionValue( commandLine, allowStandardLocation, forceOption, force );
        return new SingleCellExpressionDataFileOptionValue( dfvo, commandLine.hasOption( USE_ENSEMBL_IDS_OPTION ) );
    }

    public static DesignFileOptionValue getDesignFileOptionValue( CommandLine commandLine, boolean allowStandardLocation, String forceOption, boolean force ) throws ParseException {
        DataFileOptionValue dfov = getDataFileOptionValue( commandLine, allowStandardLocation, forceOption, force );
        return new DesignFileOptionValue( dfov,
                commandLine.hasOption( USE_MULTIPLE_ROWS_FOR_ASSAYS_OPTION ),
                commandLine.hasOption( SEPARATE_SAMPLE_FROM_ASSAYS_IDENTIFIERS_OPTION ),
                commandLine.hasOption( USE_BIO_ASSAY_IDS_OPTION ),
                commandLine.hasOption( USE_RAW_COLUMN_NAMES_OPTION )
        );
    }

    public static CellMetadataFileOptionValue getCellMetadataFileOptionValue( CommandLine commandLine, boolean allowStandardLocation, String forceOption, boolean force ) throws ParseException {
        DataFileOptionValue dfov = getDataFileOptionValue( commandLine, allowStandardLocation, forceOption, force );
        return new CellMetadataFileOptionValue( dfov,
                commandLine.hasOption( SEPARATE_SAMPLE_FROM_ASSAYS_IDENTIFIERS_OPTION ),
                commandLine.hasOption( USE_BIO_ASSAY_IDS_OPTION ),
                commandLine.hasOption( USE_RAW_COLUMN_NAMES_OPTION )
        );
    }
}
