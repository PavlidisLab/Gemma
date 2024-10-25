package ubic.gemma.core.datastructure.matrix.io;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Bundles a directory containing MEX files into a TAR archive.
 * @author poirigui
 * @see MexMatrixWriter
 */
public class MexMatrixBundler {

    public void bundle( java.nio.file.Path p, OutputStream stream ) throws IOException {
        try ( TarArchiveOutputStream out = new TarArchiveOutputStream( stream ) ) {
            Files.walkFileTree( p, new FileVisitor<Path>() {

                @Nullable
                private java.nio.file.Path sampleDir;

                @Override
                public FileVisitResult preVisitDirectory( java.nio.file.Path path, BasicFileAttributes basicFileAttributes ) {
                    sampleDir = path;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile( java.nio.file.Path path, BasicFileAttributes basicFileAttributes ) throws IOException {
                    if ( sampleDir != null ) {
                        TarArchiveEntry entry = out.createArchiveEntry( path, sampleDir.getFileName() + "/" + path.getFileName() );
                        out.putArchiveEntry( entry );
                        try ( InputStream in = Files.newInputStream( path ) ) {
                            IOUtils.copy( in, out );
                        }
                        out.closeArchiveEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed( java.nio.file.Path path, IOException e ) {
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory( java.nio.file.Path path, IOException e ) {
                    sampleDir = null;
                    return FileVisitResult.CONTINUE;
                }
            } );
        }
    }
}
