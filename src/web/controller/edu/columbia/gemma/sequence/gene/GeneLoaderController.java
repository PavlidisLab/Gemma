package controller.edu.columbia.gemma.sequence.gene;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.loader.sequence.gene.GeneLoaderService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author keshav
 * @version $Id keshav $
 */
public class GeneLoaderController extends SimpleFormController {
   private GeneLoaderService geneLoaderService;
   /** Logger for this class and subclasses */
   protected final Log logger = LogFactory.getLog( getClass() );

   /**
    * @return
    */
   public GeneLoaderService getGeneLoaderService() {
      return geneLoaderService;
   }

   //   private BioSequenceManager biosequenceMan;
   //   private CompositeSequenceManager compositesequenceMan;
   //   private ArrayDesignManager arraydesignMan;

   /**
    * @param
    * @return
    */
   public ModelAndView onSubmit( Object command ) throws ServletException {

      String now = ( new java.util.Date() ).toString();
      logger.info( "returning hello view with " + now );

      Map myModel = new HashMap();
      myModel.put( "now", now );
      try {
         //myModel.put( "genes", getGeneLoaderService().parseFile() );
         getGeneLoaderService()
               .loadDatabase(
                     "C:\\Documents and Settings\\keshav\\My Documents\\Gemma\\Database Files\\gene.txt" );
      } catch ( IOException e ) {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }
      //this is defined as ModelAndView("view name", "model name", model object)
      //the model contains a HashMap of Genes (POJOs) keyed for the gene.jsp
      //by "genes", as well as the time/date object keyed by "now"
      return new ModelAndView( "gene", "model", myModel );
   }

   //   public void setBioSequenceManager( BioSequenceManager bsm ) {
   //      biosequenceMan = bsm;
   //   }
   //
   //   public BioSequenceManager getBioSequenceManager() {
   //      return biosequenceMan;
   //   }
   //
   //   public void setCompositeSequenceManager( CompositeSequenceManager csm ) {
   //      compositesequenceMan = csm;
   //   }
   //
   //   public CompositeSequenceManager getCompositeSequenceManager() {
   //      return compositesequenceMan;
   //   }
   //
   /**
    * @param
    */
   public void setGeneLoaderService( GeneLoaderService gls ) {
      this.geneLoaderService = gls;
   }
}