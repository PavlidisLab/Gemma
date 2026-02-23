package ubic.gemma.cli.util;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.Strings;
import ubic.gemma.cli.completion.CompletionType;
import ubic.gemma.cli.completion.CompletionUtils;

/**
 * Utilities for adding options to the CLI that can subsequently be retrieved with {@link EntityLocator}.
 * @author poirigui
 */
public class EntityOptionsUtils {


    /**
     * @see EntityLocator#locateEntity(Class, String, boolean)
     */
    public static void addEntityOption( Options options, String optionName, String longOpt, String description ) {
        options.addOption( Option.builder( optionName )
                .longOpt( longOpt )
                .hasArg()
                .argName( "ID, short name, name, NCBI ID, common name, scientific name, alternate name" )
                .desc( Strings.CS.appendIfMissing( description, "." ) + " Entity ID, short name, name, NCBI ID, common name, scientific name, alternate name" )
                .get() );
    }

    /**
     * Add an option for supplying a dataset.
     */
    public static void addDatasetOption( Options options, String optionName, String longOpt, String description ) {
        addOption( options, optionName, longOpt, "ID, short name, name", description, CompletionType.DATASET );
    }

    /**
     * Add an option for supplying an EE set.
     */
    public static void addDatasetGroupOption( Options options, String optionName, String longOpt, String description ) {
        addOption( options, optionName, longOpt, "ID, name", description, CompletionType.DATASET_GROUP );
    }

    /**
     * Add an option for supplying a taxon.
     */
    public static void addTaxonOption( Options options, String optionName, String longOpt, String description ) {
        addOption( options, optionName, longOpt, "ID, NCBI ID, common name, scientific name", description, CompletionType.TAXON );
    }

    /**
     * Add an option for supplying a comma-delimited list of platforms.
     */
    public static void addCommaDelimitedPlatformOption( Options options, String optionName, String longOpt, String description ) {
        addCommaDelimitedOption( options, optionName, longOpt, "ID, short name, name, alternate name; or a comma-delimited list of those", description, CompletionType.PLATFORM );
    }

    /**
     * Add an option for supplying a platform.
     */
    public static void addPlatformOption( Options options, String optionName, String longOpt, String description ) {
        addOption( options, optionName, longOpt, "ID, short name, name, alternate name", description, CompletionType.PLATFORM );
    }

    /**
     * Add an option for supplying a generic platform.
     */
    public static void addGenericPlatformOption( Options options, String optionName, String longOpt, String description ) {
        addOption( options, optionName, longOpt, "ID, short name, name, alternate name", description, CompletionType.PLATFORM, "generic" );
    }

    private static void addOption( Options options, String optionName, String longOpt, String argName, String description, CompletionType completionType, String... completionArgs ) {
        options.addOption( Option.builder( optionName )
                .longOpt( longOpt )
                .hasArg()
                .argName( argName )
                .converter( EnumeratedByCommandStringConverter.of( CompletionUtils.generateCompleteCommand( completionType, completionArgs ) ) )
                .desc( description )
                .get() );
    }

    private static void addCommaDelimitedOption( Options options, String optionName, String longOpt, String argName, String description, CompletionType completionType, String... completionArgs ) {
        options.addOption( Option.builder( optionName )
                .longOpt( longOpt )
                .hasArg()
                .argName( argName )
                .converter( EnumeratedByCommandStringConverter.of( CompletionUtils.generateCompleteCommand( completionType, completionArgs ) ) )
                .valueSeparator( ',' )
                .desc( description )
                .get() );
    }
}
