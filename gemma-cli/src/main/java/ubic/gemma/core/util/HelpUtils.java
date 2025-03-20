package ubic.gemma.core.util;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.PrintWriter;

public class HelpUtils {

    public static final String HEADER = "Options:";
    public static final String FOOTER = "The Gemma project, Copyright (c) 2007-2024 University of British Columbia.";

    private static final HelpFormatter formatter = new HelpFormatter();

    static {
        formatter.setSyntaxPrefix( "Usage: " );
    }

    public static void printHelp( PrintWriter writer, String syntax, Options options, @Nullable String header, @Nullable String footer ) {
        if ( StringUtils.isBlank( header ) ) {
            header = HEADER;
        } else {
            header = "\n" + header + "\n\n" + HEADER;
        }
        if ( StringUtils.isBlank( footer ) ) {
            footer = "\n" + FOOTER;
        } else {
            footer = "\n" + footer + "\n\n" + FOOTER;
        }
        formatter.printHelp( writer, 150, syntax, header, options, HelpFormatter.DEFAULT_LEFT_PAD, HelpFormatter.DEFAULT_DESC_PAD, footer );
    }
}
