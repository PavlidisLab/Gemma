package ubic.gemma.core.analysis.sequence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static ubic.gemma.core.util.test.TestProcessUtils.FILL_STDERR;
import static ubic.gemma.core.util.test.TestProcessUtils.writeShellScript;

/**
 * {@link RepeatScan} against shell scripts standing in for RepeatMasker. The query file is the last argument.
 */
@DisabledOnOs(OS.WINDOWS)
class RepeatScanFakeRepeatMaskerTest {

    /**
     * Sets {@code $query} to the last argument.
     */
    private static final String LAST_ARG = "for a in \"$@\"; do query=\"$a\"; done";

    @TempDir
    Path tmp;

    private BioSequence sequence;

    @BeforeEach
    void setUp() {
        Taxon human = Taxon.Factory.newInstance( "Homo sapiens", "human", 9606, true );
        sequence = BioSequence.Factory.newInstance( "seq1", human );
        sequence.setSequence( "ACGTACGTACGTACGTACGT" );
    }

    /**
     * stderr was read only after RepeatMasker exited, so one that wrote more than a pipe buffer's worth to it blocked,
     * and the scan waited forever.
     */
    @Test
    void aRunThatWritesALotToStandardErrorFinishes() throws IOException {
        Path exe = writeShellScript( tmp, "RepeatMasker", LAST_ARG + "\n"
                + FILL_STDERR + "\n"
                // mask the whole sequence
                + "tr 'ACGT' 'acgt' < \"$query\" > \"$query.masked\"" );

        Collection<BioSequence> masked = assertTimeoutPreemptively( Duration.ofSeconds( 60 ),
                () -> new RepeatScan( exe.toString() ).repeatScan( Collections.singleton( sequence ) ) );

        assertThat( masked ).containsExactly( sequence );
        assertThat( sequence.getFractionRepeats() ).isEqualTo( 1.0 );
    }

    /**
     * No {@code -dir} was passed, so the output went wherever RepeatMasker put it by default, and the scan looked only
     * beside the query file (issue #53). This stand-in writes only where {@code -dir} says, and nothing without it.
     */
    @Test
    void theOutputIsWrittenWhereItIsRead() throws IOException {
        Path exe = writeShellScript( tmp, "RepeatMasker", "dir=\n"
                + "while [ $# -gt 1 ]; do if [ \"$1\" = -dir ]; then dir=\"$2\"; shift; fi; shift; done\n"
                + "query=\"$1\"\n"
                + "[ -n \"$dir\" ] || exit 0\n"
                + "tr 'ACGT' 'acgt' < \"$query\" > \"$dir/$(basename \"$query\").masked\"" );

        Collection<BioSequence> masked = new RepeatScan( exe.toString() ).repeatScan( Collections.singleton( sequence ) );

        assertThat( masked ).containsExactly( sequence );
    }

    /**
     * A summary that did not say "no repeats" was logged as a warning, and the scan returned nothing, which
     * platformRepeatScan recorded as a completed scan.
     */
    @Test
    void anUnrecognizedSummaryWithNoMaskedSequencesIsAFailure() throws IOException {
        Path exe = writeShellScript( tmp, "RepeatMasker", LAST_ARG + "\n"
                + "echo 'cannot find the rmblast engine' > \"$query.out\"" );

        assertThatThrownBy( () -> new RepeatScan( exe.toString() ).repeatScan( Collections.singleton( sequence ) ) )
                .hasMessageContaining( "RepeatMasker exited normally but wrote no masked sequences" )
                .hasMessageEndingWith( "cannot find the rmblast engine" );
    }

    @Test
    void aSummarySayingThereAreNoRepeatsIsNotAFailure() throws IOException {
        Path exe = writeShellScript( tmp, "RepeatMasker", LAST_ARG + "\n"
                + "echo \"There were no repetitive sequences detected in $query\" > \"$query.out\"" );

        assertThat( new RepeatScan( exe.toString() ).repeatScan( Collections.singleton( sequence ) ) ).isEmpty();
    }

    /**
     * Masked sequences whose names matched none of the ones written were each logged as a warning, and the scan
     * returned nothing.
     */
    @Test
    void maskedSequencesMatchingNoneWrittenAreAFailure() throws IOException {
        Path exe = writeShellScript( tmp, "RepeatMasker", LAST_ARG + "\n"
                + "printf '>renamed\\nacgtacgtacgtacgtacgt\\n' > \"$query.masked\"" );

        assertThatThrownBy( () -> new RepeatScan( exe.toString() ).repeatScan( Collections.singleton( sequence ) ) )
                .hasMessageContaining( "None of the 1 sequences was found in" );
    }

    @Test
    void aFailedRunThatWritesALotToStandardErrorReportsItsEnd() throws IOException {
        Path exe = writeShellScript( tmp, "RepeatMasker", FILL_STDERR + "\n"
                + "echo 'species human is not in the library' >&2\n"
                + "exit 3" );

        assertTimeoutPreemptively( Duration.ofSeconds( 60 ), () ->
                assertThatThrownBy( () -> new RepeatScan( exe.toString() ).repeatScan( Collections.singleton( sequence ) ) )
                        .hasMessageContaining( "RepeatMasker failed with exit value 3" )
                        .hasMessageEndingWith( "species human is not in the library" ) );
    }
}
