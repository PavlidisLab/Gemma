package edu.columbia.gemma.loader.smd;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.xml.sax.SAXException;

import edu.columbia.gemma.common.auditAndSecurity.PersonDao;
import edu.columbia.gemma.common.bqs.BibliographicReference;
import edu.columbia.gemma.common.bqs.BibliographicReferenceDao;
import edu.columbia.gemma.common.description.DescriptionDao;
import edu.columbia.gemma.common.description.File;
import edu.columbia.gemma.common.description.FileDao;
import edu.columbia.gemma.common.description.FileFormatDao;
import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.expression.bioAssay.BioAssayDao;
import edu.columbia.gemma.expression.experiment.ExpressionExperiment;
import edu.columbia.gemma.expression.experiment.ExpressionExperimentDao;
import edu.columbia.gemma.loader.smd.model.SMDPublication;
import edu.columbia.gemma.sequence.gene.Taxon;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class SMDManagerImpl implements SMDManager {

   
   private BibliographicReferenceDao bibliographicReferenceDAO;
   private ExpressionExperimentDao experimentDAO;
   private FileFormatDao fileFormtDAO;
   private FileDao fileDAO;
   private PersonDao personDAO;
   private BioAssayDao bioAssayDAO;
   private DescriptionDao descriptionDAO;
    
   public void setBibliographicReferenceDAO( BibliographicReferenceDao bibliographicReferenceDAO ) {
      this.bibliographicReferenceDAO = bibliographicReferenceDAO;
   }
   public void setBioAssayDAO( BioAssayDao bioAssayDAO ) {
      this.bioAssayDAO = bioAssayDAO;
   }
   public void setDescriptionDAO( DescriptionDao descriptionDAO ) {
      this.descriptionDAO = descriptionDAO;
   }
   public void setExperimentDAO( ExpressionExperimentDao experimentDAO ) {
      this.experimentDAO = experimentDAO;
   }
   public void setFileDAO( FileDao fileDAO ) {
      this.fileDAO = fileDAO;
   }
   public void setFileFormtDAO( FileFormatDao fileFormtDAO ) {
      this.fileFormtDAO = fileFormtDAO;
   }
   public void setPersonDAO( PersonDao personDAO ) {
      this.personDAO = personDAO;
   }
   
   
   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getPublications()
    */
   public Set fetchSMDPublications() {
      Set result = new HashSet();
      Publications foo;

      try {
         foo = new Publications();
         foo.retrieveByFTP();

         for ( Iterator iter = foo.getIterator(); iter.hasNext(); ) {
            SMDPublication pub = ( SMDPublication ) iter.next();

            BibliographicReference checkMe = pub.toBiblioGraphicReference( pub.toDataBaseEntry() );

      //      bibliographicReferenceDAO.
            
         }

      } catch ( ConfigurationException e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch ( IOException e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      } catch ( SAXException e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

      return result;

   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getPublications(edu.columbia.gemma.sequence.gene.Taxon)
    */
   public Set fetchSMDPublications( Taxon species ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getNewPublications()
    */
   public Set fetchSMDNewPublications() {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getNewPublications(edu.columbia.gemma.sequence.gene.Taxon)
    */
   public Set fetchSMDNewPublications( Taxon species ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getPublication(int)
    */
   public BibliographicReference fetchSMDPublication( int accessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getExperiments()
    */
   public Set fetchSMDExperiments() {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getExperiment(edu.columbia.gemma.expression.experiment.Experiment)
    */
   public ExpressionExperiment fetchSMDExperiment( ExpressionExperiment unfinishedExperiment ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getExperiments(edu.columbia.gemma.sequence.gene.Taxon)
    */
   public Set fetchSMDExperiments( Taxon species ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getNewExperiments()
    */
   public Set fetchSMDNewExperiments() {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getNewExperiments(edu.columbia.gemma.sequence.gene.Taxon)
    */
   public Set fetchSMDNewExperiments( Taxon species ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getExperiment(int)
    */
   public ExpressionExperiment fetchSMDExperiment( int accessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getExperiment(edu.columbia.gemma.common.bqs.BibliographicReference)
    */
   public Set fetchSMDExperiment( BibliographicReference publication ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getBioAssays(edu.columbia.gemma.expression.experiment.Experiment)
    */
   public Set fetchSMDBioAssays( ExpressionExperiment experiment ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getBioAssay(int)
    */
   public BioAssay fetchSMDBioAssay( int accessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getBioAssay(edu.columbia.gemma.expression.bioAssay.BioAssay)
    */
   public BioAssay fetchSMDBioAssay( BioAssay unfinishedBioAssay ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getDataFiles(int)
    */
   public Set fetchSMDDataFiles( int experimentAccessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }

   /*
    * (non-Javadoc)
    * 
    * @see edu.columbia.gemma.loader.smd.SMDManager#getDataFile(int)
    */
   public File fetchSMDDataFile( int bioAssayAccessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchPublications()
    */
   public Set fetchPublications() {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchPublications(edu.columbia.gemma.sequence.gene.Taxon)
    */
   public Set fetchPublications( Taxon species ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchNewPublications()
    */
   public Set fetchNewPublications() {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchNewPublications(edu.columbia.gemma.sequence.gene.Taxon)
    */
   public Set fetchNewPublications( Taxon species ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchPublication(int)
    */
   public BibliographicReference fetchPublication( int accessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchExperiments()
    */
   public Set fetchExperiments() {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchExperiment(edu.columbia.gemma.expression.experiment.ExpressionExperiment)
    */
   public ExpressionExperiment fetchExperiment( ExpressionExperiment unfinishedExperiment ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchExperiments(edu.columbia.gemma.sequence.gene.Taxon)
    */
   public Set fetchExperiments( Taxon species ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchNewExperiments()
    */
   public Set fetchNewExperiments() {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchNewExperiments(edu.columbia.gemma.sequence.gene.Taxon)
    */
   public Set fetchNewExperiments( Taxon species ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchExperiment(int)
    */
   public ExpressionExperiment fetchExperiment( int accessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchExperiment(edu.columbia.gemma.common.bqs.BibliographicReference)
    */
   public Set fetchExperiment( BibliographicReference publication ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchBioAssays(edu.columbia.gemma.expression.experiment.ExpressionExperiment)
    */
   public Set fetchBioAssays( ExpressionExperiment experiment ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchBioAssay(int)
    */
   public BioAssay fetchBioAssay( int accessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchBioAssay(edu.columbia.gemma.expression.bioAssay.BioAssay)
    */
   public BioAssay fetchBioAssay( BioAssay unfinishedBioAssay ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchDataFiles(int)
    */
   public Set fetchDataFiles( int experimentAccessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }
   /* (non-Javadoc)
    * @see edu.columbia.gemma.loader.smd.SMDManager#fetchDataFile(int)
    */
   public File fetchDataFile( int bioAssayAccessionNumber ) {
      // TODO Auto-generated method stub
      return null;
   }

}