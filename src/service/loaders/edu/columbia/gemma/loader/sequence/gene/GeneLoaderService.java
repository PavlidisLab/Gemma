/*
 * TODO use the findByName() to check if an entry already exists.
 * TODO factor out potential common logic (ie. parseFile)
 * TODO remove hard-coded path of file.  This was done just to get gene data in db.
 * TODO add assertions (see coding standards email RE: Paul Pavlidis Sun 3/6/2005 10:55 PM
 */

package edu.columbia.gemma.loader.sequence.gene;

//Parser packages.
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import edu.columbia.gemma.sequence.gene.GeneDao;
import edu.columbia.gemma.sequence.gene.GeneImpl;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class GeneLoaderService {
   private static int count = 0;
   private BufferedReader br;
   private GeneDao geneDao;
   private BufferedReader in;

   protected GeneDao getGeneDao() {
      return this.geneDao;
   }

   /**
    * 
    * 
    */
   public void loadDatabase() {
      parseFile();
   }

   /**
    *
    *
    */
   public void parseFile() {
      //Initialization
      boolean fileExists = false;
      String filename = "C:\\Documents and Settings\\keshav\\My Documents\\Gemma\\Database Files\\gene.txt";
      in = new BufferedReader( new InputStreamReader( System.in ) );
      try {
         File file = new File( filename );
         br = new BufferedReader( new FileReader( file ) );
         fileExists = true;
      } catch ( Exception e ) {
         System.out.println( "Cannot find file." );
         fileExists = false;
      }

      System.out.println( "\nThe filename is " + filename );
      StopWatch stopWatch = new StopWatch();
      stopWatch.start();
      try {
         //         Resource resource = new ClassPathResource( "tools/remotingclient.xml" );
         //         BeanFactory bf = new XmlBeanFactory( resource );
         //         GeneManagerInterface gmi = ( GeneManagerInterface ) bf
         //               .getBean( "geneManagerProxy-hessian" );

         System.out.println( " Reading file ... " );
         saveGene();
         stopWatch.stop();
         in.close();
         br.close();
         System.out.println( " Time taken = " + stopWatch.getTime() + " ms" );
      } catch ( IOException ioe ) {
         ioe.printStackTrace();
      } catch ( Exception e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
   }

   /**
    * 
    *
    */
   public void saveGene() {
      String[] sArray = null;
      String line = null;
      long totalLinesProcessed = 0l;
      GeneImpl g = new GeneImpl();
      int maxCol = 8;
      try {
         while ( ( line = br.readLine() ) != null ) {
            totalLinesProcessed++;
            sArray = StringUtils.split( line, "\t" );
            //System.err.println("sArray Length: " + sArray.length);
            g.setIdentifier( "gene::" + count + "::" + sArray[0] );
            g.setSymbol( sArray[0] );
            g.setOfficialName( sArray[1] );
            g.setPhysicalMapLocation( sArray[4] );
            if ( sArray.length > maxCol && sArray[7] == "LocusLink" ) {
               g.setNcbiId( Integer.parseInt( sArray[8] ) );
            }
            getGeneDao().create( g );
            count++;
         }
         System.out.println( " Objects persisted successfully ... " );
      } catch ( IOException e ) {
         e.printStackTrace();
      }
   }

   /**
    * @param geneDao
    */
   public void setGeneDao( GeneDao geneDao ) {
      this.geneDao = geneDao;
   }
}

