/*
 * TODO use the findByName() to check if an entry already exists. TODO factor out potential common logic (ie. parseFile)
 * TODO remove hard-coded path of file. This was done just to get gene data in db. TODO add assertions (see coding
 * standards email RE: Paul Pavlidis Sun 3/6/2005 10:55 PM
 */

package edu.columbia.gemma.loader.sequence.gene;

//Parser packages.
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.MappingException;
import net.sf.hibernate.SessionFactory;
//import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.cfg.Settings;
import net.sf.hibernate.impl.SessionFactoryImpl;
import org.springframework.orm.hibernate.HibernateTemplate;

import edu.columbia.gemma.sequence.gene.Gene;
import edu.columbia.gemma.sequence.gene.GeneDao;
import edu.columbia.gemma.sequence.gene.GeneDaoImpl;
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
   protected static final Log log = LogFactory.getLog( GeneLoaderService.class );
   private GeneDao geneDao;
   int identifierCol;
   int symbolCol;
   int officialNameCol;
   int refIdCol;
   int ncbiIdCol;
   String localBasePath;
   String filename;
   String filepath;
   public GeneLoaderService(GeneDao gd) {
      this.geneDao = gd;
   }
   
   /**
    * @throws ConfigurationException
    */
   public GeneLoaderService() throws ConfigurationException{            
       Configuration conf = new PropertiesConfiguration("gene.properties");
       localBasePath = (String)conf.getProperty("gene.local.datafile.basepath");
       filename = (String)conf.getProperty("gene.filename");
       filepath = localBasePath + "\\" + filename;
       identifierCol = conf.getInt("gene.identifierCol");
       symbolCol = conf.getInt("gene.symbolCol");
       officialNameCol = conf.getInt("gene.officialNameCol");
       refIdCol = conf.getInt("gene.ncbiIdCol");
       ncbiIdCol = conf.getInt("gene.ncbiIdCol");
   }

   protected GeneDao getGeneDao() {
      return this.geneDao;
   }

   /**
    * @throws IOException
    */
   public void loadDatabase() throws IOException {
      //  parseFile(); 
      saveGenes( openFileAsStream() );
   }
   

   /**
    * @return InputStream
    * @throws IOException
    */
   public InputStream openFileAsStream() throws IOException {
      //Initialization
 
      File file = new File( filepath );
      if ( !file.canRead() )
            throw new IOException( "Can't read from file " + filepath );
      
      return new FileInputStream( file );

   }

   /**
    * @param is 
    * @throws IOException
    */
   public void saveGenes( InputStream is ) throws IOException {
      int maxColIndex = 4;
      int count = 0;
      String line = null;
      BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
      while ( ( line = br.readLine() ) != null ) {
         String[] sArray = line.split( "\t");
         Gene g = Gene.Factory.newInstance();
         g.setIdentifier( "gene::" + count + "::" + sArray[identifierCol] );
         g.setSymbol( sArray[symbolCol] );
         g.setOfficialName( sArray[officialNameCol] );
         //g.setPhysicalMapLocation( Integer.parseInt(sArray[5]) );
         if ( sArray.length > maxColIndex && sArray[refIdCol] == "LocusLink" ) {
            g.setNcbiId( Integer.parseInt(sArray[refIdCol]) );
         }
         getGeneDao().create( g );
         count++;
      }
      log.info( " Objects persisted successfully ... " );

      br.close();
   }

   /**
    * @param geneDao
    */
   public void setGeneDao( GeneDao geneDao ) {
      this.geneDao = geneDao;
   }
   
//   public static void main(String[] args) {
//      
//      try {
//         GeneDaoImpl f =  new GeneDaoImpl();
//         Configuration cfg = new Configuration();
//         cfg.addInputStream(GeneLoaderService.class.getResourceAsStream("/hibernate.cfg.xml"));
//         Settings stg = new Settings();
//         SessionFactory sf = new SessionFactoryImpl(cfg, stg);
//         HibernateTemplate h = new HibernateTemplate();
//         h.setSessionFactory(sf);
//         f.setHibernateTemplate(h);
//         GeneLoaderService gls = new GeneLoaderService();
//         gls.setGeneDao(f);
//         gls.loadDatabase("C:\\Documents and Settings\\keshav\\My Documents\\Gemma\\Database Files\\gene.txt");
//      } catch ( IOException e ) {
//         // TODO Auto-generated catch block
//         e.printStackTrace();
//      } catch ( MappingException e ) {
//         // TODO Auto-generated catch block
//         e.printStackTrace();
//      } catch ( HibernateException e ) {
//         // TODO Auto-generated catch block
//         e.printStackTrace();
//      }
//   }
   
}

