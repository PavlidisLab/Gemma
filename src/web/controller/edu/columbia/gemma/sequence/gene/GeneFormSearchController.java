package controller.edu.columbia.gemma.sequence.gene;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.sequence.gene.Gene;
import edu.columbia.gemma.sequence.gene.GeneImpl;
import edu.columbia.gemma.sequence.gene.GeneService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author keshav
 * @version $Id keshav $
 */
public class GeneFormSearchController extends SimpleFormController {
   private GeneService geneService;
   /** Logger for this class and subclasses */
   protected final Log log = LogFactory.getLog( getClass() );

   /**
    * @return
    */
   public GeneService getGeneService() {
      return geneService;
   }

   //   private BioSequenceManager biosequenceMan;
   //   private CompositeSequenceManager compositesequenceMan;
   //   private ArrayDesignManager arraydesignMan;

   /**
    * @param
    * @return
    */
   public ModelAndView onSubmit( Object command ) throws ServletException {
      String view;
      Map myModel = new HashMap();
      String now = ( new java.util.Date() ).toString();
      String officialName = ( ( GeneImpl ) command ).getOfficialName();
      Gene g = Gene.Factory.newInstance();
      try {
         view = "qtl";
         g = ( getGeneService().findByOfficialName( officialName ) );
         int physicalMapLocation = g.getPhysicalMapLocation();
         myModel.put( "now", now );
         myModel.put( "qtls", getGeneService()
               .findAllQtlsByPhysicalMapLocation( physicalMapLocation ) );
      } catch ( NullPointerException e ) {
         view = "error";
      }
      //this is defined as ModelAndView("view name", "model name", model object)
      //the model contains a HashMap of Genes (POJOs) keyed for the gene.jsp
      //by "genes", as well as the time/date object keyed by "now"
      return new ModelAndView( view, "model", myModel );
   }

   protected Object formBackingObject( HttpServletRequest request )
         throws Exception {
      log.info( "formBackingObject" );
      Gene gene = Gene.Factory.newInstance();
      gene.setOfficialName( "RIKEN cDNA 0610005A07 gene" );
      return gene;
   }

   /**
    * @param
    */
   public void setGeneService( GeneService gs ) {
      this.geneService = gs;
   }
}