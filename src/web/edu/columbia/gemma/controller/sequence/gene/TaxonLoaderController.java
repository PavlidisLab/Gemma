package edu.columbia.gemma.controller.sequence.gene;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.loader.sequence.gene.GeneLoaderService;
import edu.columbia.gemma.loader.sequence.gene.TaxonLoaderService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author keshav
 * @version $Id keshav $
 */
public class TaxonLoaderController extends SimpleFormController {
   private TaxonLoaderService taxonLoaderService;
   /** Logger for this class and subclasses */
   protected final Log logger = LogFactory.getLog( getClass() );

   /**
    * @return
    */
   public TaxonLoaderService getTaxonLoaderService() {
      return taxonLoaderService;
   }

   /**
    * @param
    * @return
    */
   public ModelAndView onSubmit( Object command ) throws IOException, ServletException {
      String now = ( new java.util.Date() ).toString();
      logger.info( "returning hello view with " + now );
      Map myModel = new HashMap();
      myModel.put( "now", now );
      getTaxonLoaderService().loadDatabase();
      return new ModelAndView( "taxon", "model", myModel );
   }
   /**
    * @param
    */
   public void setTaxonLoaderService( TaxonLoaderService tls ) {
      this.taxonLoaderService = tls;
   }
}