package edu.columbia.gemma.loader.smd;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A class to determine the species for all smdexperiments (bio assays)
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SpeciesExperimentMap {

   Map speciesExperimentMap;

   /**
    * @throws IOException
    */
   public SpeciesExperimentMap() throws IOException {
      speciesExperimentMap = new HashMap();
      SMDSpecies smds = new SMDSpecies();
      String[] species = smds.getShortSpeciesNames();

      for ( int i = 0; i < species.length; i++ ) {
         String s = species[i];
         SpeciesBioAssayList list = new SpeciesBioAssayList();
         list.retrieveByFTP( s );
         Set exps = list.getExperiments();
         for ( Iterator iter = exps.iterator(); iter.hasNext(); ) {
            String name = ( String ) iter.next();
            speciesExperimentMap.put( name, s );
         }
      }
   }

   public String getSpecies( int experimentId ) {
      String key = ( new Integer( experimentId ) ).toString();

      if ( !speciesExperimentMap.containsKey( key ) ) return null;
      return ( String ) speciesExperimentMap.get( key );
   }

   public static void main( String[] args ) {
      try {
         SpeciesExperimentMap foo = new SpeciesExperimentMap();
         for (int i = 0; i < 10000; i++) {
            System.err.println(foo.getSpecies(i));
         }
      } catch ( IOException e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }
}