package edu.columbia.gemma.controller.sequence.gene;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.loader.sequence.gene.FileName;
import edu.columbia.gemma.loader.sequence.gene.TaxonLoaderService;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class TaxonLoaderController extends SimpleFormController {
   private TaxonLoaderService taxonLoaderService;
   /** Logger for this class and subclasses */
   protected final Log logger = LogFactory.getLog( getClass() );
   
   /**
    * @param
    */
   public void setTaxonLoaderService( TaxonLoaderService tls ) {
      this.taxonLoaderService = tls;
   }
   
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
   public ModelAndView onSubmit(Object command) throws IOException {
      String filename = ( ( FileName ) command ).getFileName();
      Map myModel = new HashMap();
      getTaxonLoaderService().bulkCreate(filename,true);
      return new ModelAndView( "taxon", "model", myModel );
   }
   
   protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        FileName fileName = new FileName();
        fileName.setFileName("Taxon.txt");
        return fileName;
    }
   
}