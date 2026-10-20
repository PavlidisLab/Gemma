package ubic.gemma.core.goldenpath;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import ubic.gemma.model.genome.Taxon;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The configured GoldenPath URL names a server and no database, so the database has to reach every connection the
 * pool opens.
 *
 * @author pavlidis
 */
@Tag("goldenpath")
class GoldenPathConnectionPoolTest {

    /**
     * A remap queries GoldenPath once per probe — tens of thousands of times — so it long outlives the connection it
     * started on. A connection the pool opens later, to grow or to replace one the server closed, used to arrive with
     * no database selected, and the run died on it. mapPlatformToGenes deletes a platform's associations before it
     * queries anything, so that left GPL6887 with 4.5% of its probe-to-gene mappings.
     */
    @Test
    void everyConnectionThePoolOpensHasTheDatabase() throws SQLException {
        GoldenPathSequenceAnalysis gp;
        try {
            gp = new GoldenPathSequenceAnalysis( Taxon.Factory.newInstance( "human" ) );
        } catch ( DataAccessException e ) {
            gp = Assumptions.abort( "No GoldenPath database to connect to: " + e.getMessage() );
        }
        try ( GoldenPathSequenceAnalysis goldenPath = gp ) {
            String expected = goldenPath.getSearchedDatabase().getName();

            // held, not closed, so each request has to come from a different physical connection
            List<Connection> held = new ArrayList<>();
            try {
                for ( int i = 0; i < 4; i++ ) {
                    Connection connection = goldenPath.getDataSource().getConnection();
                    held.add( connection );
                    try ( Statement statement = connection.createStatement();
                            ResultSet rs = statement.executeQuery( "SELECT DATABASE()" ) ) {
                        assertTrue( rs.next() );
                        assertEquals( expected, rs.getString( 1 ),
                                "connection " + ( i + 1 ) + " of the pool has the wrong database selected" );
                    }
                }
            } finally {
                for ( Connection connection : held ) {
                    connection.close();
                }
            }
        }
    }
}
