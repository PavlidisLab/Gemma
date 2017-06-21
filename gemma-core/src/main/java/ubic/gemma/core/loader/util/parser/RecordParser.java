package ubic.gemma.core.loader.util.parser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.Collection;

/**
 * Abstract record-based parser. Records are defined by lines starting with a given record separator. The default record
 * separator is ">".
 *
 * @author pavlidis
 */
public abstract class RecordParser<T> implements Parser<T> {

    protected static final Log log = LogFactory.getLog( RecordParser.class );
    private String recordSeparator = ">";

    @Override
    public abstract Collection<T> getResults();

    @Override
    public void parse( File file ) throws IOException {
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        try (FileInputStream stream = new FileInputStream( file );) {
            parse( stream );
        }
    }

    @Override
    public void parse( InputStream is ) throws IOException {
        int recordsParsed = 0;
        int nullRecords = 0;
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;
        StringBuilder record = null;
        while ( true ) {

            line = br.readLine();
            String lastRecord = null;

            // start a fresh record? (null condition happens at end)
            if ( line == null || line.startsWith( recordSeparator ) ) {
                if ( record != null ) {
                    lastRecord = record.toString();
                }
                record = new StringBuilder();
            }

            if ( record == null )
                continue;

            record.append( line );
            record.append( "\n" );

            if ( lastRecord == null )
                continue;

            Object newItem = parseOneRecord( lastRecord );

            if ( newItem != null ) {
                addResult( newItem );
                recordsParsed++;
            } else {
                log.debug( "Got null parse from " + line );
                nullRecords++;
            }
            if ( recordsParsed % PARSE_ALERT_FREQUENCY == 0 ) {
                String message = "Parsed " + recordsParsed + " records ...";
                log.debug( message );
            }

            if ( line == null ) { // EOF.
                break;
            }

        }

        if ( log.isInfoEnabled() && recordsParsed > 0 ) {
            log.info( "Successfully parsed " + recordsParsed + " records." + ( nullRecords > 0 ?
                    " Another " + nullRecords + " records yielded no parse result." :
                    "" ) );
        }

    }

    @Override
    public void parse( String filename ) throws IOException {
        File infile = new File( filename );
        parse( infile );
    }

    /**
     * Handle the parsing of a single record from the input.
     */
    public abstract Object parseOneRecord( String record );

    public void setRecordSeparator( String recordSeparator ) {
        this.recordSeparator = recordSeparator;
    }

    /**
     * Add an object to the results collection.
     */
    protected abstract void addResult( Object obj );
}
