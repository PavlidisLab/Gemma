package ubic.gemma.cli.util;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Converter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.ocpsoft.prettytime.nlp.PrettyTimeParser;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import ubic.basecode.util.DateUtil;

import javax.annotation.Nullable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.appendIfMissing;

public class OptionsUtils {

    /**
     * When parsing dates, use this as a reference for 'now'.
     */
    private static final Date DEFAULT_RELATIVE_TO = new Date();

    private static final TimeZone DEFAULT_TIME_ZONE = TimeZone.getDefault();

    /**
     * Add a date option with support for fuzzy dates (i.e. one month ago).
     * @see DateConverterImpl
     */
    public static void addDateOption( String name, @Nullable String longOpt, String desc, Options options ) {
        options.addOption( Option.builder( name )
                .longOpt( longOpt )
                .desc( desc )
                .hasArg()
                .type( Date.class )
                .converter( new DateConverterImpl( DEFAULT_RELATIVE_TO, DEFAULT_TIME_ZONE ) ).build() );
    }

    /**
     * A converter for parsing dates supporting various formats.
     * <ul>
     *     <li>most ISO 8601 date and date time with or without UTC offset</li>
     *     <li>{@code +1d, -1m, -1h} as per {@link DateUtil#getRelativeDate(Date, String)}</li>
     *     <li>natural language (i.e. five hours ago, last week, etc. using {@link PrettyTimeParser}</li>
     * </ul>
     * @author poirigui
     */
    static class DateConverterImpl implements Converter<Date, ParseException> {

        /**
         * Exact date formats to attempt before resorting to natural language parsing.
         */
        private final SimpleDateFormat[] exactDateFormats = {
                // ISO 8601
                new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssX", Locale.ENGLISH ),
                new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH ),
                new SimpleDateFormat( "yyyy-MM-dd", Locale.ENGLISH ),
                new SimpleDateFormat( "yyyy-MM", Locale.ENGLISH ),
                new SimpleDateFormat( "yyyy", Locale.ENGLISH )
        };

        private final Date relativeTo;
        private final PrettyTimeParser parser;

        /**
         * @param relativeTo date relative to which duration are interpreted
         * @param timeZone   when parsing date, use this time zone as a reference
         */
        public DateConverterImpl( Date relativeTo, TimeZone timeZone ) {
            this.relativeTo = relativeTo;
            this.parser = new PrettyTimeParser( timeZone );
            for ( SimpleDateFormat format : exactDateFormats ) {
                format.setTimeZone( timeZone );
            }
        }

        @Override
        public Date apply( String string ) throws ParseException {
            for ( SimpleDateFormat format : exactDateFormats ) {
                try {
                    return format.parse( string );
                } catch ( ParseException e ) {
                    // ignore
                }
            }
            try {
                return DateUtil.getRelativeDate( relativeTo, string );
            } catch ( IllegalArgumentException e ) {
                // ignore
            }
            List<Date> candidates = parser.parse( string, relativeTo );
            if ( candidates.isEmpty() ) {
                throw new ParseException( "No suitable date found.", 0 );
            }
            if ( candidates.size() > 1 ) {
                throw new ParseException( "More than one date is specified.", 0 );
            }
            return candidates.iterator().next();
        }
    }

    /**
     * Add an option with three possible values: {@code true}, {@code false}, or {@code null}.
     * <p>
     * Use {@link #getAutoOptionValue(CommandLine, String, String)} to retrieve its value later on.
     */
    public static void addAutoOption( Options options, String optionName, String longOptionName, String description, String noOptionName, String longNoOptionName, String noDescription ) {
        options.addOption( optionName, longOptionName, false, description + " This option is incompatible with " + formatOption( noOptionName, longNoOptionName ) + ". Default is to auto-detect." );
        options.addOption( noOptionName, longNoOptionName, false, noDescription + " This option is incompatible with " + formatOption( optionName, longOptionName ) + ". Default is to auto-detect." );
    }

    /**
     * Format an option for display.
     */
    public static String formatOption( Options options, String optionName ) {
        return formatOption( requireNonNull( options.getOption( optionName ),
                () -> "No option with name " + optionName + " exists. Is it possible it hasn't been declared yet?" ) );
    }

    @Nullable
    public static Boolean getAutoOptionValue( CommandLine commandLine, String optionName, String noOptionName ) throws org.apache.commons.cli.ParseException {
        if ( commandLine.hasOption( optionName ) && commandLine.hasOption( noOptionName ) ) {
            throw new org.apache.commons.cli.ParseException( String.format( "Cannot specify %s and %s at the same time.",
                    formatOption( commandLine, optionName ), formatOption( commandLine, noOptionName ) ) );
        }
        if ( commandLine.hasOption( optionName ) ) {
            return true;
        } else if ( commandLine.hasOption( noOptionName ) ) {
            return true;
        } else {
            return null;
        }
    }

    /**
     * Add an enumerated option with localized descriptions.
     * <p>
     * The code pattern for the message source is {@code <enum class name>.<enum value>.label}.
     */
    public static <T extends Enum<T>> void addEnumOption( Options options, String optionName, String longOption,
            String description, Class<T> enumClass ) {
        EnumMap<T, MessageSourceResolvable> descriptions = new EnumMap<>( enumClass );
        for ( T v : EnumSet.allOf( enumClass ) ) {
            descriptions.put( v, new DefaultMessageSourceResolvable( new String[] { enumClass.getSimpleName() + "." + v.name() + ".label" }, null, "" ) );
        }
        addEnumOption( options, optionName, longOption, description, enumClass, descriptions );
    }

    /**
     * Add an enumerated option with descriptions.
     */
    public static <T extends Enum<T>> void addEnumOption( Options options, String optionName, String longOption,
            String description, Class<T> enumClass, EnumMap<T, MessageSourceResolvable> descriptions ) {
        options.addOption( Option.builder( optionName )
                .longOpt( longOption )
                .hasArg()
                .converter( EnumConverter.of( enumClass, descriptions ) )
                .desc( String.format( "%s Possible values are: %s.",
                        appendIfMissing( description, "." ),
                        Arrays.stream( enumClass.getEnumConstants() ).map( Enum::name ).collect( Collectors.joining( ", " ) ) ) )
                .build() );
    }

    public static <T extends Enum<T>> void addEnumSetOption( Options options, String optionName, String longOption, String description, Class<T> enumClass ) {
        options.addOption( Option.builder( optionName )
                .longOpt( longOption )
                .hasArgs()
                .valueSeparator( ',' )
                .converter( EnumConverter.of( enumClass ) )
                .desc( String.format( "%s Possible values are: %s.",
                        appendIfMissing( description, "." ),
                        Arrays.stream( enumClass.getEnumConstants() ).map( Enum::name ).collect( Collectors.joining( ", " ) ) ) )
                .build() );
    }

    /**
     * Obtain the value of an enumerated option.
     * <p>
     * The option must have been previously declared with {@link #addEnumOption(Options, String, String, String, Class)}.
     */
    @Nullable
    public static <T extends Enum<T>> T getEnumOptionValue( CommandLine commandLine, String optionName ) throws
            org.apache.commons.cli.ParseException {
        return commandLine.getParsedOptionValue( optionName );
    }

    @Nullable
    public static <T extends Enum<T>> EnumSet<T> getEnumSetOptionValue( CommandLine commandLine, String optionName ) throws
            org.apache.commons.cli.ParseException {
        for ( Option o : commandLine.getOptions() ) {
            if ( o.getOpt().equals( optionName ) ) {
                Set<T> result = new HashSet<>();
                for ( String s : o.getValues() ) {
                    try {
                        //noinspection unchecked
                        result.add( ( T ) o.getConverter().apply( s ) );
                    } catch ( Throwable e ) {
                        throw new org.apache.commons.cli.ParseException( e );
                    }
                }
                return EnumSet.copyOf( result );
            }
        }
        return null;
    }

    /**
     * Obtain the value of an enumerated option and if present, make sure that it satisfies a predicate.
     * <p>
     * The option must have been previously declared with {@link #addEnumOption(Options, String, String, String, Class)}.
     */
    @Nullable
    public static <T extends Enum<T>> T getEnumOptionValue( CommandLine commandLine, String
            optionName, Predicate<CommandLine> predicate ) throws org.apache.commons.cli.ParseException {
        return getParsedOptionValue( commandLine, optionName, predicate );
    }

    /**
     * Obtain the value of an option and if present, make sure that it satisfies a predicate.
     */
    @Nullable
    public static String getOptionValue( CommandLine commandLine, String
            optionName, Predicate<CommandLine> predicate ) throws org.apache.commons.cli.ParseException {
        if ( hasOption( commandLine, optionName, predicate ) ) {
            return commandLine.getOptionValue( optionName );
        } else {
            return null;
        }
    }

    @Nullable
    public static String[] getOptionValues( CommandLine commandLine, String
            optionName, Predicate<CommandLine> predicate ) throws org.apache.commons.cli.ParseException {
        if ( hasOption( commandLine, optionName, predicate ) ) {
            return commandLine.getOptionValues( optionName );
        } else {
            return null;
        }
    }

    /**
     * @see #getOptionValue(CommandLine, String, Predicate)
     */
    @Nullable
    public static <T> T getParsedOptionValue( CommandLine commandLine, String optionName, Predicate<CommandLine> predicate ) throws
            org.apache.commons.cli.ParseException {
        if ( hasOption( commandLine, optionName, predicate ) ) {
            return commandLine.getParsedOptionValue( optionName );
        } else {
            return null;
        }
    }

    /**
     * Check if an option is present, and if so make sure that the predicate is satisfied.
     * <p>
     * The predicate can be any {@link Predicate}, but using the ones defined in this class will produce more
     * informative error messages.
     */
    public static boolean hasOption( CommandLine commandLine, String
            optionName, Predicate<CommandLine> predicate ) throws org.apache.commons.cli.ParseException {
        if ( commandLine.hasOption( optionName ) ) {
            // make sure that all the required options are set
            if ( !predicate.test( commandLine ) ) {
                throw new org.apache.commons.cli.ParseException( String.format( "The %s option %s.",
                        formatOption( commandLine, optionName ), formatPredicate( predicate, commandLine, 0 ) ) );
            }
            return true;
        }
        return false;
    }

    /**
     * Check if any of the given options is present in the command line.
     */
    public static boolean hasAnyOption( CommandLine commandLine, String... options ) {
        return Arrays.stream( options ).anyMatch( commandLine::hasOption );
    }

    /**
     * Make sure that the given predicate is satisfied.
     * <p>
     * This is useful as a top-level predicate as it prepents "requires " to the description.
     */
    public static Predicate<CommandLine> requires( Predicate<CommandLine> predicate ) {
        return new OptionRequirement( predicate, ( cl, depth ) -> "requires " + formatPredicate( predicate, cl, depth ) );
    }

    /**
     * Make sure that at least one of the given predicate is true.
     */
    @SafeVarargs
    public static Predicate<CommandLine> anyOf( Predicate<CommandLine>... optionNames ) {
        return new OptionRequirement( cl -> Arrays.stream( optionNames ).anyMatch( p -> p.test( cl ) ),
                ( cl, depth ) -> formatPredicates( optionNames, cl, " or ", depth ) );
    }


    /**
     * Make sure that all the given predicates are true.
     */
    @SafeVarargs
    public static Predicate<CommandLine> allOf( Predicate<CommandLine>... optionNames ) {
        return new OptionRequirement( cl -> Arrays.stream( optionNames ).allMatch( p -> p.test( cl ) ),
                ( cl, depth ) -> formatPredicates( optionNames, cl, " and ", depth ) );
    }

    /**
     * Make sure that none of the given predicates are true.
     */
    @SafeVarargs
    public static Predicate<CommandLine> noneOf( Predicate<CommandLine>... optionNames ) {
        return new OptionRequirement( cl -> Arrays.stream( optionNames ).noneMatch( p -> p.test( cl ) ),
                ( cl, depth ) -> "none of " + formatPredicates( optionNames, cl, " nor ", depth ) );
    }

    /**
     * Make sure that the given option is present.
     */
    public static Predicate<CommandLine> toBeSet( String optionName ) {
        return new OptionRequirement(
                cl -> cl.hasOption( optionName ),
                ( cl, depth ) -> formatOption( cl, optionName ) + " to be set" );
    }

    /**
     * Make sure that the given option is missing.
     */
    public static Predicate<CommandLine> toBeUnset( String optionName ) {
        return new OptionRequirement(
                cl -> !cl.hasOption( optionName ),
                ( cl, depth ) -> formatOption( cl, optionName ) + " to be unset" );
    }

    private static String formatOption( CommandLine cl, String optionName ) {
        // FIXME: this only works for options that the user has provided
        return Arrays.stream( cl.getOptions() )
                .filter( o -> o.getOpt().equals( optionName ) )
                .findFirst()
                .map( OptionsUtils::formatOption )
                .orElse( "-" + optionName );
    }

    private static String formatOption( Option opt ) {
        return formatOption( opt.getOpt(), opt.getLongOpt() );
    }

    private static String formatOption( String opt, @Nullable String longOpt ) {
        return "-" + opt + ( longOpt != null ? ",--" + longOpt : "" );
    }

    private static String formatPredicates( Predicate<CommandLine>[] predicates, CommandLine cl, String w,
            int depth ) {
        String s = Arrays.stream( predicates )
                .map( p -> formatPredicate( p, cl, depth + 1 ) )
                .collect( Collectors.joining( w ) );
        return depth > 0 ? ( "(" + s + ")" ) : s;
    }

    private static String formatPredicate( Predicate<CommandLine> p, CommandLine cl, int depth ) {
        if ( p instanceof OptionRequirement ) {
            return ( ( OptionRequirement ) p ).describe( cl, depth );
        } else {
            return p.toString();
        }
    }

    private static class OptionRequirement implements Predicate<CommandLine> {

        private final Predicate<CommandLine> delegate;
        private final BiFunction<CommandLine, Integer, String> description;

        private OptionRequirement( Predicate<CommandLine> delegate, BiFunction<CommandLine, Integer, String> description ) {
            this.delegate = delegate;
            this.description = description;
        }

        @Override
        public boolean test( CommandLine commandLine ) {
            return delegate.test( commandLine );
        }

        public String describe( CommandLine cl, int depth ) {
            return description.apply( cl, depth );
        }
    }
}
