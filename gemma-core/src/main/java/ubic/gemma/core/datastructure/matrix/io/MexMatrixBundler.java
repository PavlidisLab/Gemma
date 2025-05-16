package ubic.gemma.core.datastructure.matrix.io;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.lang3.ArrayUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bundles a directory containing MEX files into a TAR archive.
 * @author poirigui
 * @see MexMatrixWriter
 */
public class MexMatrixBundler {

    /**
     * A list of MEX files to include in the TAR archive.
     */
    private static final String[] MEX_FILES = { "barcodes.tsv.gz", "features.tsv.gz", "matrix.mtx.gz" };

    /**
     * Calculate the size of the resulting TAR without actually producing it.
     */
    public long calculateSize( Path path ) throws IOException {
        long headerSize;
        AtomicLong dataSize = new AtomicLong();
        try ( TarArchiveOutputStream out = new TarArchiveOutputStream( NullOutputStream.INSTANCE ) ) {
            Files.walkFileTree( path, new MexVisitor() {
                @Override
                public void visitSample( Path sampleDir, Path path ) throws IOException {
                    TarArchiveEntry entry = out.createArchiveEntry( path, sampleDir.getFileName() + "/" + path.getFileName() );
                    out.putArchiveEntry( entry );
                    dataSize.addAndGet( Files.size( path ) );
                    out.closeArchiveEntry();
                }
            } );
            out.finish();
            headerSize = out.getBytesWritten();
        }
        return headerSize + dataSize.get();
    }

    public void bundle( java.nio.file.Path p, OutputStream stream ) throws IOException {
        try ( TarArchiveOutputStream out = new TarArchiveOutputStream( stream ) ) {
            Files.walkFileTree( p, new MexVisitor() {
                @Override
                public void visitSample( Path sampleDir, Path path ) throws IOException {
                    TarArchiveEntry entry = out.createArchiveEntry( path, sampleDir.getFileName() + "/" + path.getFileName() );
                    out.putArchiveEntry( entry );
                    Files.copy( path, out );
                    out.closeArchiveEntry();
                }
            } );
        }
    }

    private static abstract class MexVisitor implements FileVisitor<Path> {

        @Nullable
        private Path sampleDir;

        public abstract void visitSample( Path sampleDir, Path path ) throws IOException;

        @Override
        public FileVisitResult preVisitDirectory( Path path, BasicFileAttributes basicFileAttributes ) {
            sampleDir = path;
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile( Path path, BasicFileAttributes basicFileAttributes ) throws IOException {
            if ( sampleDir != null && ArrayUtils.contains( MEX_FILES, path.getFileName().toString() ) ) {
                visitSample( sampleDir, path );
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed( Path path, IOException e ) {
            return FileVisitResult.TERMINATE;
        }

        @Override
        public FileVisitResult postVisitDirectory( Path path, IOException e ) {
            sampleDir = null;
            return FileVisitResult.CONTINUE;
        }
    }
}
