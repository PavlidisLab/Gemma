package edu.columbia.gemma.loader.smd;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SmdUtil {

   public static final String SMD_DELIM="\n";
   
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
}