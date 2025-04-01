package ubic.gemma.cli.util;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    /**
     * may be tab-delimited, only first column used, commented (#) lines are ignored.
     *
     * @param fileName the file name
     * @return list of ee identifiers
     * @throws IOException in case there is an IO error while reading the file
     */
    public static List<String> readListFileToStrings( String fileName ) throws IOException {
        return readListFileToStrings( Paths.get( fileName ) );
    }

    public static List<String> readListFileToStrings( Path file ) throws IOException {
        List<String> eeNames = new ArrayList<>();
        try ( BufferedReader in = Files.newBufferedReader( file ) ) {
            while ( in.ready() ) {
                String line = in.readLine().trim();
                if ( line.startsWith( "#" ) ) {
                    continue;
                }
                if ( line.isEmpty() )
                    continue;
                String[] split = StringUtils.split( line, "\t" );
                eeNames.add( split[0] );
            }
            return eeNames;
        }
    }
}
