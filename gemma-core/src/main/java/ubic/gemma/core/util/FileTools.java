/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.core.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Ported in-tree from <code>ubic.basecode.util.FileTools</code> (baseCode project, Apache 2.0,
 * University of British Columbia). Trimmed to the methods used by Gemma; unused helpers
 * (file-extension predicates, line readers, string writers, touch, etc.) were dropped.
 *
 * @author keshav
 * @author Pavlidis
 * @author Will Braynen
 */
public class FileTools {

    private static final Logger log = LoggerFactory.getLogger( FileTools.class );

    /**
     * @return the filename with its final extension stripped (everything after the last '.'),
     *         or the original filename if no extension is present.
     */
    public static String chompExtension( String filename ) {
        int j = filename.lastIndexOf( '.' );
        if ( j > 1 ) {
            return filename.substring( 0, filename.lastIndexOf( '.' ) );
        }
        return filename;
    }

    /**
     * Avoid getting file names with spaces, slashes, quotes, # etc; replace them with "_".
     *
     * @throws IllegalArgumentException if the resulting string is empty, or if the input is blank.
     */
    public static String cleanForFileName( String name ) {
        if ( StringUtils.isBlank( name ) ) throw new IllegalArgumentException( "'name' cannot be blank" );
        String result = name.replaceAll( "[\\s\'\";,\\/#]+", "_" ).replaceAll( "(^_|_$)", "" );
        if ( StringUtils.isBlank( result ) ) {
            throw new IllegalArgumentException( "'" + name + "' was stripped down to an empty string" );
        }
        return result;
    }

    /**
     * Creates the directory if it does not exist.
     */
    public static File createDir( String directory ) {
        File dirPath = new File( directory );
        if ( !dirPath.exists() ) {
            dirPath.mkdirs();
        }
        return dirPath;
    }

    /**
     * Deletes the specified collection of files.
     *
     * @return int The number of files deleted.
     * @see java.io.File#delete()
     */
    public static int deleteFiles( Collection<File> files ) {
        int numDeleted = 0;
        Iterator<File> iter = files.iterator();
        while ( iter.hasNext() ) {
            File file = iter.next();
            if ( file.isDirectory() ) {
                log.warn( "Cannot delete a directory." );
                continue;
            }
            if ( log.isDebugEnabled() ) log.debug( "Deleting file " + file.getAbsolutePath() + "." );
            if ( file.delete() ) {
                numDeleted++;
            } else {
                log.warn( "Failed to delete: " + file + " read=" + file.canRead() + " write=" + file.canWrite() );
            }
        }
        if ( numDeleted > 0 ) log.info( "Deleted " + numDeleted + " files." );
        return numDeleted;
    }

    /**
     * Open a non-compressed, zipped, or gzipped file. Uses the file name pattern to figure this out.
     *
     * @param fileName if zipped, only the first file in the archive is used.
     */
    @SuppressWarnings("resource")
    public static InputStream getInputStreamFromPlainOrCompressedFile( String fileName )
            throws IOException, FileNotFoundException {
        if ( !FileTools.testFile( fileName ) ) {
            throw new IOException( "Could not read from " + fileName );
        }
        InputStream i;
        if ( FileTools.isZipped( fileName ) ) {
            log.debug( "Reading from zipped file" );
            ZipFile f = new ZipFile( fileName );
            ZipEntry entry = f.entries().nextElement();

            if ( entry == null ) {
                f.close();
                throw new IOException( "No zip entries" );
            }

            if ( f.entries().hasMoreElements() ) {
                log.debug( "ZIP archive has more then one file, reading the first one." );
            }

            i = f.getInputStream( entry );
        } else if ( FileTools.isGZipped( fileName ) ) {
            log.debug( "Reading from gzipped file" );
            i = new GZIPInputStream( new FileInputStream( fileName ) );
        } else {
            log.debug( "Reading from uncompressed file" );
            i = new FileInputStream( fileName );
        }
        return i;
    }

    /**
     * @return true if the filename ends with {@code .gz} or {@code .gzip} (case-insensitive).
     */
    public static boolean isGZipped( String fileName ) {
        String capfileName = fileName.toUpperCase();
        return capfileName.endsWith( ".GZ" ) || capfileName.endsWith( ".GZIP" );
    }

    /**
     * @return true if the filename ends with {@code .zip} (case-insensitive).
     */
    public static boolean isZipped( String filename ) {
        String capfileName = filename.toUpperCase();
        return capfileName.endsWith( ".ZIP" );
    }

    /**
     * Given a File representing a directory, return the (non-directory) files it contains.
     */
    public static Collection<File> listDirectoryFiles( File directory ) {
        if ( !directory.isDirectory() ) throw new IllegalArgumentException( "Must be a directory" );
        FileFilter fileFilter = File::isFile;
        File[] files = directory.listFiles( fileFilter );
        return Arrays.asList( files );
    }

    /**
     * Given a File representing a directory, return the subdirectories it contains.
     */
    public static Collection<File> listSubDirectories( File directory ) {
        if ( !directory.isDirectory() ) throw new IllegalArgumentException( "Must be a directory" );
        FileFilter fileFilter = File::isDirectory;
        File[] files = directory.listFiles( fileFilter );
        return Arrays.asList( files );
    }

    /**
     * Resolve a classpath resource to an absolute filesystem path.
     */
    public static String resourceToPath( String resourcePath ) throws URISyntaxException {
        if ( StringUtils.isBlank( resourcePath ) ) throw new IllegalArgumentException();
        URL resource = FileTools.class.getResource( resourcePath );
        if ( resource == null ) throw new IllegalArgumentException( "Could not get URL for resource=" + resourcePath );
        return new File( resource.toURI() ).getAbsolutePath();
    }

    /**
     * Given the path to a gzipped file, unzip it into the same directory. If the output file
     * already exists it will be overwritten.
     *
     * @return absolute path to the unzipped file.
     */
    public static String unGzipFile( final String seekFile ) throws IOException {
        if ( !isGZipped( seekFile ) ) {
            throw new IllegalArgumentException();
        }
        checkPathIsReadableFile( seekFile );
        String outputFilePath = chompExtension( seekFile );
        File outputFile = copyPlainOrCompressedFile( seekFile, outputFilePath );
        return outputFile.getAbsolutePath();
    }

    /**
     * Unzip every entry of a zip archive next to the archive.
     */
    @SuppressWarnings("resource")
    public static Collection<File> unZipFiles( final String seekFile ) throws IOException {
        if ( !isZipped( seekFile ) ) {
            throw new IllegalArgumentException();
        }
        checkPathIsReadableFile( seekFile );
        String outputFilePath = chompExtension( seekFile );

        Collection<File> result = new HashSet<>();
        try {
            ZipFile f = new ZipFile( seekFile );
            for ( Enumeration<? extends ZipEntry> entries = f.entries(); entries.hasMoreElements(); ) {
                ZipEntry entry = entries.nextElement();
                String outputFileTitle = entry.getName();
                InputStream is = f.getInputStream( entry );

                File out = new File( outputFilePath + outputFileTitle );
                OutputStream os = new FileOutputStream( out );
                copy( is, os );

                result.add( out );
                log.debug( outputFileTitle );
            }
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        return result;
    }

    /* ------------------------------------------------------------------
     * Internal helpers (package-private; used only by the public methods
     * above).
     * ------------------------------------------------------------------ */

    private static void checkPathIsReadableFile( String file ) throws IOException {
        File infile = new File( file );
        if ( !infile.exists() || !infile.canRead() ) {
            throw new IOException( "Could not find file: " + file );
        }
    }

    /**
     * On completion both streams are closed.
     */
    private static void copy( InputStream input, OutputStream output ) throws IOException {
        if ( input.available() == 0 ) return;
        byte[] buf = new byte[1024];
        int len;
        while ( ( len = input.read( buf ) ) > 0 ) {
            output.write( buf, 0, len );
        }
        input.close();
        output.close();
    }

    @SuppressWarnings("resource")
    private static File copyPlainOrCompressedFile( final String sourcePath, String outputFilePath ) throws IOException {
        File sourceFile = new File( sourcePath );
        if ( !sourceFile.exists() ) {
            throw new IllegalArgumentException( "Source file (" + sourcePath + ") does not exist" );
        }
        if ( sourceFile.isDirectory() ) {
            throw new UnsupportedOperationException( "Don't know how to copy directories (" + sourceFile + ")" );
        }

        File outputFile = new File( outputFilePath );
        if ( outputFile.exists() && outputFile.isDirectory() ) {
            throw new UnsupportedOperationException( "Don't know how to copy to directories (" + outputFile + ")" );
        }

        OutputStream out = new FileOutputStream( outputFile );
        InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( sourcePath );
        copy( is, out );
        return outputFile;
    }

    private static boolean testFile( String filename ) {
        if ( filename != null && filename.length() > 0 ) {
            File f = new File( filename );
            return f.isFile() && f.canRead();
        }
        return false;
    }
}
