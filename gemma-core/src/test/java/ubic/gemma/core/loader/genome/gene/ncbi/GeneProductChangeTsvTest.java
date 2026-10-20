package ubic.gemma.core.loader.genome.gene.ncbi;

import org.junit.jupiter.api.Test;
import ubic.gemma.persistence.service.genome.gene.GeneProductChange;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeneProductChangeTsvTest {

    private static final GeneProductChange KEPT = new GeneProductChange( GeneProductChange.Kind.REMOVE, false, "human",
            7003, "TEAD1", null, null, 12L, "NM_021961", "4507437", 3, 1, List.of( "GPL570", "GPL96" ) );
    private static final GeneProductChange ORPHAN = new GeneProductChange( GeneProductChange.Kind.REMOVE, true, "mouse",
            null, null, null, null, 13L, "name\twith a tab", null, 0, 0, List.of() );
    private static final GeneProductChange MOVED = new GeneProductChange( GeneProductChange.Kind.SWITCH, true, "fly",
            39070, "Lcp65Ab1", 39071, "Lcp65Ab2", 14L, "BT099970", "1108657489", 2, 0, List.of( "GPL1322" ) );

    @Test
    void rowsAndTheDryRunMarkReadBackAsWritten() throws IOException {
        for ( boolean dryRun : new boolean[] { false, true } ) {
            StringWriter out = new StringWriter();
            try ( GeneProductChangeTsv tsv = new GeneProductChangeTsv( out, dryRun ) ) {
                tsv.accept( KEPT );
                tsv.accept( ORPHAN );
                tsv.accept( MOVED );
            }

            GeneProductChangeTsv.Report report = GeneProductChangeTsv.read( new StringReader( out.toString() ) );

            assertThat( report.dryRun() ).isEqualTo( dryRun );
            assertThat( report.changes() ).containsExactly( KEPT, ORPHAN, MOVED );
        }
    }

    /**
     * A run that fails is not closed normally, so what reached the file is what was flushed.
     */
    @Test
    void eachRowIsFlushedAsItIsWritten() throws IOException {
        StringWriter out = new StringWriter();
        GeneProductChangeTsv tsv = new GeneProductChangeTsv( new BufferedWriter( out ), false );
        tsv.accept( KEPT );

        assertThat( out.toString() )
                .contains( "# dryRun=false\n" )
                .contains( "kind\tapplied\ttaxon\tgene_ncbi_id" )
                .endsWith( "\nREMOVE\tfalse\thuman\t7003\tTEAD1\t\t\t12\tNM_021961\t4507437\t3\t1\tGPL570,GPL96\n" );
    }

    @Test
    void aFileWithOtherColumnsIsRejected() {
        assertThatThrownBy( () -> GeneProductChangeTsv.read( new StringReader( "kind\tapplied\nREMOVE\tfalse\n" ) ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "Not a gene product change report" );
    }
}
