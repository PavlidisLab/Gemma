package ubic.gemma.core.util.r;

import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.REngine;
import org.rosuda.REngine.REngineException;
import org.springframework.util.Assert;
import ubic.basecode.util.StringUtil;

import java.util.List;

/**
 * A high-level client for interacting with R.
 * @author poirigui
 */
public class RClient implements AutoCloseable {

    /**
     * The R engine used for executing R commands.
     */
    private final REngine rEngine;

    public RClient( REngineFactory rEngine ) {
        try {
            this.rEngine = rEngine.createREngine();
        } catch ( Exception e ) {
            throw new RClientException( e );
        }
    }

    /**
     * <a href="https://www.rdocumentation.org/packages/base/versions/3.6.2/topics/data.frame">data.frame</a>
     */
    public void assignDataFrame( String symbol, List<String> columnNames, List<String> rowNames, List<Object> vectors ) {
        Assert.isTrue( !rowNames.isEmpty() );
        Assert.isTrue( columnNames.size() == vectors.size() );
        try {
            // create valid and unique R identifiers for the column names
            String[] cn = StringUtil.makeNames( columnNames.toArray( new String[0] ), true );
            String[] rn = StringUtil.makeNames( rowNames.toArray( new String[0] ), true );
            rEngine.assign( "rows", rn );
            StringBuilder data = new StringBuilder();
            for ( int i = 0; i < cn.length; i++ ) {
                assignVector( "cols" + i, vectors.get( i ) );
                if ( i > 0 ) {
                    data.append( ", " );
                }
                data.append( rowNames.get( i ) ).append( "=" ).append( "cols" ).append( i );
            }
            rEngine.assign( "rows", rowNames.toArray( new String[0] ) );
            REXP dataFrame = rEngine.parse( "data.frame(" + data + ", row.names=rows)", false );
            rEngine.assign( symbol, dataFrame );
            rEngine.parseAndEval( "rm(rows);" );
            for ( int i = 0; i < cn.length; i++ ) {
                rEngine.parseAndEval( "rm(cols" + i + ")" );
            }
        } catch ( REngineException | REXPMismatchException e ) {
            throw new RClientException( e );
        }
    }

    public void retrieveDataFrame( String symbol, List<String> columnNames, List<String> rowNames, List<Object> vectors ) {

    }

    public void assignVector( String symbol, Object vector ) {
        try {
            if ( vector instanceof double[] ) {
                rEngine.assign( symbol, ( double[] ) vector );
            } else {
                throw new RClientException( vector.getClass() + " is not a supported vector type." );
            }
        } catch ( REngineException e ) {
            throw new RClientException( e );
        }
    }

    public REXP parseAndEval( String cmd ) {
        try {
            return rEngine.parseAndEval( cmd );
        } catch ( REngineException | REXPMismatchException e ) {
            throw new RClientException( e );
        }
    }

    @Override
    public void close() {
        if ( !rEngine.close() ) {
            throw new RClientException( "Failed to close the underlying R engine." );
        }
    }
}
