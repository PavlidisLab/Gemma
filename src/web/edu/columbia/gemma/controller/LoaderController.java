package edu.columbia.gemma.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.validation.BindException;
import org.springframework.web.bind.RequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;
import org.springframework.web.servlet.view.RedirectView;

import edu.columbia.gemma.loader.loaderutils.BulkCreator;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University 
 * 
 * Generic Controller used to decipher which LoaderService is called.
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="loaderController"
 * @spring.property name="sessionForm" value="true"
 * @spring.property name="formView" value="bulkLoadForm"
 */
public class LoaderController extends SimpleFormController {
    private Configuration conf;
    private String ioError;
    private String numberFormatError;
    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog( getClass() );

    /**
     * @throws ConfigurationException
     */
    public LoaderController() throws ConfigurationException {
        conf = new PropertiesConfiguration( "loader.properties" );
        ioError = conf.getString( "loader.ioError.view" );
        numberFormatError = conf.getString( "loader.numberFormatError.view" );
    }

    /**
     * Obtains filename to be read from the form.
     * 
     * @param command
     * @return ModelAndView
     */
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) {
        Map myModel = new HashMap();
        boolean hasHeader = RequestUtils.getBooleanParameter( request, "hasHeader", false );
        String typeOfLoader = RequestUtils.getStringParameter( request, "typeOfLoader", null );
        BulkCreator bc = determineService( getApplicationContext(), typeOfLoader );
        try {
            bc.bulkCreate( determineFilename( typeOfLoader ), hasHeader );
            return new ModelAndView( new RedirectView( getSuccessView() ), "model", myModel );
        } catch ( IOException e ) {
            return new ModelAndView( ioError, "model", myModel );
        } catch ( NumberFormatException e ) {
            return new ModelAndView( numberFormatError, "model", myModel );
        }
    }

    /**
     * Determine file to read based on loader selected.
     * 
     * @param typeOfLoader
     * @return String TODO use reflection
     */
    private String determineFilename( String typeOfLoader ) {
        String filename = null;
        if ( typeOfLoader.equals( "geneLoaderService" ) )
            filename = conf.getString( "loader.filename.gene" );
        else if ( typeOfLoader.equals( "taxonLoaderService" ) )
            filename = conf.getString( "loader.filename.taxon" );
        else if ( typeOfLoader.equals( "chromosomeLoaderService" ) )
            filename = conf.getString( "loader.filename.chromosome" );
        else if ( typeOfLoader.equals( "arrayDesignLoaderService" ) )
                filename = conf.getString( "loader.filename.arrayDesign" );

        return filename;
    }

    /**
     * Determines the type of service to be used (by reflection).
     * 
     * @param obj
     * @param filename
     * @return BulkCreator
     * @throws IllegalArgumentException
     */

    private BulkCreator determineService( ApplicationContext ctx, String typeOfLoader ) throws NullArgumentException {
        return ( BulkCreator ) ctx.getBean( typeOfLoader );
    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }
}