package ubic.gemma.core.loader.util.mapper;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Parses a file that contains a mapping between old and new bioassay names.
 * @see RenamingBioAssayMapper
 */
public class RenamingBioAssayMapperParser {

    private final BioAssayMapper delegate;

    public RenamingBioAssayMapperParser( BioAssayMapper delegate ) {
        this.delegate = delegate;
    }

    public BioAssayMapper parse( Path file ) throws IOException {
        Set<String> used = new HashSet<>();
        List<String> from = new ArrayList<>();
        List<String> to = new ArrayList<>();
        try ( CSVParser parser = CSVFormat.TDF.parse( Files.newBufferedReader( file ) ) ) {
            for ( CSVRecord record : parser ) {
                if ( !used.add( record.get( 0 ) ) ) {
                    throw new IllegalArgumentException( "There is already a sample name mapping for " + record.get( 0 ) + "." );
                }
                from.add( record.get( 0 ) );
                to.add( record.get( 1 ) );
            }
        }
        return new RenamingBioAssayMapper( delegate, from.toArray( new String[0] ), to.toArray( new String[0] ) );
    }
}
