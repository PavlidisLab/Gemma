package edu.columbia.gemma.loader.smd.util;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import edu.columbia.gemma.loader.smd.DataFileFetcher;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SmdUtil {

   protected static final Log log = LogFactory.getLog( SmdUtil.class );
   private static String host = "smd-ftp.stanford.edu";
   private static  String login = "anonymous";
   private static  String password = "pavlidis@dbmi.columbia.edu";
   public static final String SMD_DELIM="\n";
   
   static {
      Configuration config = null;
      try {
         config = new PropertiesConfiguration( "smd.properties" );
      } catch ( ConfigurationException e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      host = ( String ) config.getProperty( "smd.host" );
      login = ( String ) config.getProperty( "smd.login" );
      password = ( String ) config.getProperty( "smd.password" );
   }
   
   /**
    * Split a SMD-formatted key-value string. These are preceded by 0 or more white-space, a "!", and then a
    * "="-delimited key-value pair.
    * 
    * @param k
    * @return String array containing the key and value, or null if the input was not a valid SMD-formatted key-value.
    */
   public static String[] smdSplit( String k ) {
      String f = k.trim();

      if ( !f.startsWith( "!" ) ) return null;
      f = f.replaceFirst( "^!", "" );
      String[] vals = f.split( "=" ); // could be nothing after the equals.
      if ( vals.length < 1 ) throw new IllegalStateException( "Could not parse " + k );
      return vals;
   }
   
   /**
    * Convenient method to get a FTP connection.
    * @param host
    * @param login
    * @param password
    * @param mode
    * @return
    * @throws SocketException
    * @throws IOException
    */
   public static FTPClient connect( int mode ) throws SocketException, IOException {
      FTPClient f = new FTPClient();

      boolean success = false;
      f.connect( host );
      int reply = f.getReplyCode();
      if ( FTPReply.isPositiveCompletion( reply ) ) success = f.login( login, password );
      if ( !success ) {
         f.disconnect();
         throw new IOException( "Couldn't connect to " + host );
      }
      f.setFileType( mode );
      log.info( "Connected to " + host );
      return f;
   }

   /**
    * @return
    */
   public static String getHostName() {
      // TODO Auto-generated method stub
      return host;
   }
   
}