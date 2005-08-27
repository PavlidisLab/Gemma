package edu.columbia.gemma.loader.expression.smd;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPFile;
import org.xml.sax.SAXException;

import edu.columbia.gemma.common.description.LocalFile;
import edu.columbia.gemma.loader.expression.smd.model.SMDPublication;
import edu.columbia.gemma.loader.expression.smd.util.SmdUtil;
import edu.columbia.gemma.loader.loaderutils.FtpFetcher;

/**
 * Retrieve information on SMD publications.
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PublicationFetcher extends FtpFetcher {

    private Set<SMDPublication> publications;

    public PublicationFetcher() throws ConfigurationException {
        publications = new HashSet<SMDPublication>();
        Configuration config = new PropertiesConfiguration( "Gemma.properties" );
        baseDir = ( String ) config.getProperty( "smd.publication.baseDir" );

    }

    public Iterator<SMDPublication> getIterator() {
        return publications.iterator();
    }

    /**
     * @throws IOException
     * @throws SAXException
     */
    public void fetch() throws IOException, SAXException {
        this.fetch( 0 );
    }

    /**
     * @throws SAXException
     * @throws IOException
     */
    public void fetch( int limit ) throws IOException, SAXException {
        if ( !f.isConnected() ) f = SmdUtil.connect( FTP.ASCII_FILE_TYPE );

        FTPFile[] files = f.listFiles( baseDir );

        for ( int i = 0; i < files.length; i++ ) {
            if ( files[i].isDirectory() ) {
                String pubNum = files[i].getName();

                FTPFile[] pubfiles = f.listFiles( baseDir + "/" + pubNum );
                for ( int j = 0; j < pubfiles.length; j++ ) {

                    if ( !pubfiles[j].isDirectory() ) {
                        String pubFile = pubfiles[j].getName();

                        if ( !pubFile.matches( "publication_[0-9]+.meta" ) ) continue;

                        InputStream is = f.retrieveFileStream( baseDir + "/" + pubNum + "/" + pubFile );
                        if ( is == null ) throw new IOException( "Could not get stream for " + pubFile );

                        SMDPublication newPub = new SMDPublication();
                        newPub.read( is );
                        boolean success = f.completePendingCommand();
                        is.close();

                        if ( !success ) {
                            log.error( "Failed to complete download of " + pubFile );
                            continue;
                        }

                        newPub.setId( Integer.parseInt( pubNum ) );
                        publications.add( newPub );
                        log.info( "Retrieved " + pubFile );

                    }
                }
            }
            if ( publications.size() >= limit && limit > 0 ) {
                log.info( "Reached requested limit of " + limit + " publications retrieved" );
                break;
            }
        }

        log.info( publications.size() + " publications retrieved." );

        f.disconnect();
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.loader.loaderutils.Fetcher#fetch(java.lang.String)
     */
    public Collection<LocalFile> fetch( String identifier ) {
        // TODO Auto-generated method stub FIXME
        throw new UnsupportedOperationException();
    }

}