package ubic.gemma.loader.util.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract record-based parser. Records are defined by lines starting with a given record separator. The default record
 * separator is ">".
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class RecordParser implements Parser {

    private String recordSeparator = ">";

    protected static final Log log = LogFactory.getLog( RecordParser.class );

    private Collection<Object> results;

    public RecordParser() {
        results = new HashSet<Object>();
    }

    public Collection<Object> getResults() {
        return results;
    }

    public void parse( InputStream is ) throws IOException {
        int recordsParsed = 0;
        int nullRecords = 0;
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;
        StringBuilder record = null;
        while ( true ) {

            line = br.readLine();
            String lastRecord = null;

            // start a fresh record?
            if ( line == null || line.startsWith( recordSeparator ) ) {
                if ( record != null ) {
                    lastRecord = record.toString();
                }
                record = new StringBuilder();
            }

            record.append( line );
            record.append( "\n" );

            if ( lastRecord == null ) continue;

            Object newItem = parseOneRecord( lastRecord );

            if ( newItem != null ) {
                results.add( newItem );
                recordsParsed++;
            } else {
                log.debug( "Got null parse from " + line );
                nullRecords++;
            }
            if ( recordsParsed % PARSE_ALERT_FREQUENCY == 0 ) log.debug( "Parsed " + recordsParsed + " lines..." );

            if ( line == null ) { // EOF.
                break;
            }

        }
        log.info( "Parsed " + recordsParsed + " records; " + nullRecords + " yielded no parse result." );

    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.File)
     */
    public void parse( File file ) throws IOException {
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        FileInputStream stream = new FileInputStream( file );
        parse( stream );
        stream.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#pasre(java.lang.String)
     */
    public void parse( String filename ) throws IOException {
        File infile = new File( filename );
        parse( infile );
    }

    /**
     * Add an object to the results collection.
     * 
     * @param obj
     */
    protected void addResult( Object obj ) {
        this.results.add( obj );
    }

    public void setRecordSeparator( String recordSeparator ) {
        this.recordSeparator = recordSeparator;
    }

    /**
     * Handle the parsing of a single line from the input.
     * 
     * @param line
     */
    public abstract Object parseOneRecord( String record );
}
