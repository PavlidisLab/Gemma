package edu.columbia.gemma.loader.smd;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.xml.sax.SAXException;

import util.SmdUtil;
import edu.columbia.gemma.loader.smd.model.SMDPublication;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class Publications {

   protected static final Log log = LogFactory.getLog( Publications.class );

   public static void main( String[] args ) {
      try {
         Publications foo = new Publications();
         foo.retrieveByFTP( 5 );
      } catch ( IOException e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch ( SAXException e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch ( ConfigurationException e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
   private String baseDir = "smd/publications/";

   private FTPClient f;

   private Set publications; // set of SMDPublication

   public Publications() throws ConfigurationException {
      publications = new HashSet();
      Configuration config = new PropertiesConfiguration( "smd.properties" );
      baseDir = ( String ) config.getProperty( "smd.publication.baseDir" );

   }

   public Iterator getIterator() {
      return publications.iterator();
   }

   /**
    * @throws IOException
    * @throws SAXException
    */
   public void retrieveByFTP() throws IOException, SAXException {
      this.retrieveByFTP( 0 );
   }

   /**
    * @throws SAXException
    * @throws IOException
    */
   public void retrieveByFTP( int limit ) throws IOException, SAXException {
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

}