package ubic.gemma.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileUtilsTest {

    @TempDir
    Path dir;

    @Test
    void writeAtomically_writesTheContentAndLeavesNoTemporary() throws IOException {
        Path dest = dir.resolve( "data.txt" );
        FileUtils.writeAtomically( dest, tmp -> Files.write( tmp, "new".getBytes( StandardCharsets.UTF_8 ) ) );
        assertThat( dest ).hasContent( "new" );
        assertThat( list( dir ) ).containsExactly( "data.txt" );
    }

    /**
     * Nothing may be at the destination while the content is being written: if the JVM dies at that point, whatever
     * is there is served as complete on the next request.
     */
    @Test
    void writeAtomically_destinationHoldsThePreviousContentWhileWriting() throws IOException {
        Path dest = dir.resolve( "data.txt" );
        Files.write( dest, "old".getBytes( StandardCharsets.UTF_8 ) );
        AtomicBoolean checked = new AtomicBoolean();
        FileUtils.writeAtomically( dest, tmp -> {
            Files.write( tmp, "partial".getBytes( StandardCharsets.UTF_8 ) );
            assertThat( dest ).hasContent( "old" );
            assertThat( tmp.getParent() ).isEqualTo( dir );
            assertThat( tmp.getFileName().toString() ).startsWith( ".data.txt.tmp-" );
            checked.set( true );
        } );
        assertThat( checked ).isTrue();
        assertThat( dest ).hasContent( "partial" );
    }

    @Test
    void writeAtomically_whenTheWriterThrows_keepsThePreviousFileAndDeletesTheTemporary() throws IOException {
        Path dest = dir.resolve( "data.txt" );
        Files.write( dest, "old".getBytes( StandardCharsets.UTF_8 ) );
        assertThatThrownBy( () -> FileUtils.writeAtomically( dest, tmp -> {
            Files.write( tmp, "partial".getBytes( StandardCharsets.UTF_8 ) );
            throw new IOException( "build failed" );
        } ) ).isInstanceOf( IOException.class ).hasMessage( "build failed" );
        assertThat( dest ).hasContent( "old" );
        assertThat( list( dir ) ).containsExactly( "data.txt" );
    }

    /**
     * A catch of {@link Exception} does not see an {@link Error}; try-with-resources then closes a gzip stream and
     * writes a valid trailer onto truncated content.
     */
    @Test
    void writeAtomically_whenTheWriterThrowsAnError_leavesNoFile() {
        Path dest = dir.resolve( "data.txt.gz" );
        assertThatThrownBy( () -> FileUtils.writeAtomically( dest, tmp -> {
            Files.write( tmp, "partial".getBytes( StandardCharsets.UTF_8 ) );
            throw new StackOverflowError( "simulated" );
        } ) ).isInstanceOf( StackOverflowError.class );
        assertThat( dest ).doesNotExist();
        assertThat( list( dir ) ).isEmpty();
    }

    @Test
    void writeAtomically_replacesADirectory() throws IOException {
        Path dest = dir.resolve( "mex" );
        Files.createDirectories( dest.resolve( "sample1" ) );
        Files.write( dest.resolve( "sample1/matrix.mtx" ), "old".getBytes( StandardCharsets.UTF_8 ) );
        FileUtils.writeAtomically( dest, tmp -> {
            Files.createDirectories( tmp.resolve( "sample2" ) );
            Files.write( tmp.resolve( "sample2/matrix.mtx" ), "new".getBytes( StandardCharsets.UTF_8 ) );
        } );
        assertThat( dest.resolve( "sample1" ) ).doesNotExist();
        assertThat( dest.resolve( "sample2/matrix.mtx" ) ).hasContent( "new" );
        assertThat( list( dir ) ).containsExactly( "mex" );
    }

    @Test
    void writeAtomically_whenADirectoryWriterThrows_keepsThePreviousDirectoryAndDeletesTheTemporary() throws IOException {
        Path dest = dir.resolve( "mex" );
        Files.createDirectories( dest.resolve( "sample1" ) );
        assertThatThrownBy( () -> FileUtils.writeAtomically( dest, tmp -> {
            Files.createDirectories( tmp.resolve( "sample2" ) );
            throw new IOException( "build failed" );
        } ) ).isInstanceOf( IOException.class );
        assertThat( dest.resolve( "sample1" ) ).isDirectory();
        assertThat( list( dir ) ).containsExactly( "mex" );
    }

    private static List<String> list( Path dir ) {
        try ( Stream<Path> s = Files.list( dir ) ) {
            return s.map( p -> p.getFileName().toString() ).sorted().collect( Collectors.toList() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }
}
