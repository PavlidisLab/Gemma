package ubic.gemma.core.goldenpath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * {@link GoldenPathSequenceAnalysis#checkTablesForProbeMapping} runs before mapPlatformToGenes deletes a platform's
 * associations, so each of its refusals is what keeps a platform from being emptied by a database that cannot map it.
 */
class GoldenPathProbeMappingTablesTest {

    private static final String KNOWN_TO_REFSEQ_SAMPLE = "SELECT value FROM knownToRefSeq WHERE value LIKE ? OR value LIKE ? LIMIT 50";
    private static final String SHOW_INDEX = "SHOW INDEX FROM ncbiRefSeqCurated";

    private final JdbcTemplate jdbc = mock();

    @BeforeEach
    void setUp() {
        // every table is present, knownToRefSeq is versioned and ncbiRefSeqCurated has the (chrom, txStart) index
        knownToRefSeqHolds( "NM_000014.6", "NR_029402.2" );
        when( jdbc.queryForList( SHOW_INDEX ) ).thenReturn( Arrays.asList(
                indexColumn( "chrom", 1, "chrom" ), indexColumn( "chrom", 2, "bin" ),
                indexColumn( "name", 1, "name" ),
                indexColumn( "chrom_txStart", 1, "chrom" ), indexColumn( "chrom_txStart", 2, "txStart" ) ) );
    }

    @Test
    void currentTablesPass() {
        assertThat( check( true, true ) ).isEmpty();
    }

    /**
     * {@code mapPlatformToGenes -mirna} turns every track off; nothing reads {@code useMiRNA}, so the remap deleted the
     * platform's associations and found none.
     */
    @Test
    void noGeneTrackIsRefused() {
        ubic.gemma.core.analysis.sequence.ProbeMapperConfig config = new ubic.gemma.core.analysis.sequence.ProbeMapperConfig();
        config.setAllTracksOff();
        config.setUseMiRNA( true );
        assertThatThrownBy( () -> GoldenPathSequenceAnalysis.checkSomeGeneTrackIsOn( config ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "create none" );
        config.setUseRefGene( true );
        GoldenPathSequenceAnalysis.checkSomeGeneTrackIsOn( config );
    }

    @Test
    void missingTablesAreAllNamedWithTheDatabase() {
        tableIsMissing( "ncbiRefSeqLink" );
        tableIsMissing( "kgXref" );

        assertThatThrownBy( () -> check( true, true ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "GoldenPath database hg38 has no ncbiRefSeqLink, kgXref tables" );
    }

    /**
     * The refseq track reads ncbiRefSeqCurated and ncbiRefSeqLink; before, it read refFlat and kgXref.
     */
    @Test
    void theRefSeqTrackNeedsTheCuratedRefSeqTables() {
        tableIsMissing( "ncbiRefSeqCurated" );

        assertThatThrownBy( () -> check( true, false ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "has no ncbiRefSeqCurated table" );
    }

    /**
     * Rat has the known-gene track switched off, and UCSC publishes no knownGene, knownToRefSeq or kgXref for rn7.
     */
    @Test
    void knownGeneTablesAreOnlyRequiredForTheKnownGeneTrack() {
        tableIsMissing( "knownGene" );
        tableIsMissing( "knownToRefSeq" );
        tableIsMissing( "kgXref" );

        assertThat( check( true, false ) ).isEmpty();
        verify( jdbc, never() ).queryForList( eq( KNOWN_TO_REFSEQ_SAMPLE ), eq( String.class ), any(), any() );
    }

    /**
     * knownToRefSeq loaded before UCSC versioned it (the 2023 tables) against a versioned ncbiRefSeqCurated: the join
     * matches nothing and every known gene silently drops out.
     */
    @Test
    void anUnversionedKnownToRefSeqFails() {
        knownToRefSeqHolds( "NR_162149", "NM_000014" );

        assertThatThrownBy( () -> check( true, true ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "knownToRefSeq in GoldenPath database hg38 holds RefSeq accessions without versions (NR_162149)" );
    }

    @Test
    void aKnownToRefSeqWithoutRefSeqAccessionsFails() {
        knownToRefSeqHolds();

        assertThatThrownBy( () -> check( true, true ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "holds no NM_ or NR_ accessions" );
    }

    @Test
    void theVersionCheckSamplesOnlyNmAndNrAccessions() {
        check( true, true );

        verify( jdbc ).queryForList( KNOWN_TO_REFSEQ_SAMPLE, String.class, "NM\\_%", "NR\\_%" );
    }

    @Test
    void aMissingRangeIndexWarnsAndDoesNotFail() {
        when( jdbc.queryForList( SHOW_INDEX ) ).thenReturn( Arrays.asList(
                indexColumn( "chrom", 1, "chrom" ), indexColumn( "chrom", 2, "bin" ),
                indexColumn( "txStart", 1, "txStart" ) ) );

        assertThat( check( true, false ) )
                .singleElement().asString()
                .contains( "hg38.ncbiRefSeqCurated has no index on (chrom, txStart)" );
    }

    private List<String> check( boolean useRefGene, boolean useKnownGene ) {
        return GoldenPathSequenceAnalysis.checkTablesForProbeMapping( jdbc, "hg38", useRefGene, useKnownGene );
    }

    private void tableIsMissing( String table ) {
        String sql = "SELECT 1 FROM " + table + " LIMIT 0";
        when( jdbc.queryForList( sql ) ).thenThrow( new BadSqlGrammarException( "check", sql,
                new SQLException( "Table 'hg38." + table + "' doesn't exist", "42S02", 1146 ) ) );
    }

    private void knownToRefSeqHolds( String... values ) {
        when( jdbc.queryForList( eq( KNOWN_TO_REFSEQ_SAMPLE ), eq( String.class ), anyString(), anyString() ) )
                .thenReturn( Arrays.asList( values ) );
    }

    private static Map<String, Object> indexColumn( String keyName, long seqInIndex, String columnName ) {
        Map<String, Object> column = new HashMap<>();
        column.put( "Key_name", keyName );
        column.put( "Seq_in_index", seqInIndex );
        column.put( "Column_name", columnName );
        return column;
    }
}
