package edu.columbia.gemma.loader.smd;

import java.util.HashMap;
import java.util.Map;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SMDSpecies {

   // private final String[] speciesCodes = {"AL" , "AT" , "BS" , "CE" , "CJ" , "CR" , "DM" , "EC" , "HP" , "HS" , "MM"
   // , "SC" , "SE" , "SP" , "SS" , "ST" , "TB" , "TG" , "VC"};

   //  private final String[] speciesNames = {"AL" , "AT" , "BS" , "CE" , "CJ" , "CR" , "Drosophila melanogaster" , "EC"
   // , "HP" , "Homo sapiens" , "Mus musculus" , "SC" , "SE" , "SP" , "SS" , "ST" , "TB" , "TG" , "VC"};

   //  private final String[] shortSpeciesNames = {"AL" , "AT" , "BS" , "CE" , "CJ" , "CR" , "fly" , "EC" , "HP" ,
   // "human" , "mouse" , "SC" , "SE" , "SP" , "SS" , "ST" , "TB" , "TG" , "VC"};

   // trimmed down for testing.
   private final String[] speciesCodes = {
         "CE", "DM", "EC", "HS", "MM", "SC"
   };

   private final String[] speciesNames = {
         "Caenorhabditis elegans", "Drosophila melanogaster", "Escherichia coli", "Homo sapiens", "Mus musculus",
         "Saccharomyces cerevisiae"
   };

   private final String[] shortSpeciesNames = {
         "worm", "fly", "ecoli", "human", "mouse", "yeast"
   };

   private Map speciesMap;

   /**
    * 
    *
    */
   public SMDSpecies() {
      speciesMap = new HashMap();
      for ( int i = 0; i < speciesCodes.length; i++ ) {
         String speciesName = speciesNames[i];
         speciesMap.put( speciesName, speciesCodes[i] );
         speciesMap.put( shortSpeciesNames[i], speciesCodes[i] );
      }
   }

   /**
    * @param speciesName
    * @return
    */
   public String getCode( String speciesName ) {
      return ( String ) speciesMap.get( speciesName );
   }

   public String[] getShortSpeciesNames() {
      return shortSpeciesNames;
   }
}