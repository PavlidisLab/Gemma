package edu.columbia.gemma.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import edu.columbia.gemma.loader.loaderutils.BulkCreator;
import edu.columbia.gemma.loader.loaderutils.BulkCreatorProxyFactory;
import edu.columbia.gemma.loader.sequence.gene.FileName;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class LoaderController extends SimpleFormController {
    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog( getClass() );
    
    private BulkCreatorProxyFactory bulkCreatorProxyFactory;
    Configuration conf;
    String filepath;
    String errorview;

    /**
     * @throws ConfigurationException
     */
    public LoaderController() throws ConfigurationException {
        conf = new PropertiesConfiguration( "loader.properties" );
        filepath = conf.getString( "loadercontroller.filepath" );
        errorview = conf.getString("loader.error.view");

    }

    /**
     * @return BulkCreatorProxyFactory.
     */
    public BulkCreatorProxyFactory getBulkCreatorProxyFactory() {
        return bulkCreatorProxyFactory;
    }

    /**
     * @param command
     * @return ModelAndView
     * @throws IOException
     */
    public ModelAndView onSubmit( Object command ) {
        String filename = ( ( FileName ) command ).getFileName();
        filename = resolveFilename( filename );
        Map myModel = new HashMap();
        BulkCreator proxy = getBulkCreatorProxyFactory().getBulkCreatorProxy( filename );
        String view;
        try {
            view = proxy.bulkCreate( filename, true );
        } catch ( IOException e ) {
            return new ModelAndView( errorview, "model", myModel );
        }
        return new ModelAndView( view, "model", myModel );
    }

    /**
     * @param bulkCreatorProxyFactory The bulkCreatorProxyFactory to set.
     */
    public void setBulkCreatorProxyFactory( BulkCreatorProxyFactory bulkCreatorProxyFactory ) {
        this.bulkCreatorProxyFactory = bulkCreatorProxyFactory;
    }

    /**
     * @param filename
     * @return String
     */
    private String resolveFilename( String filename ) {
        filename = filename.toLowerCase();
        if ( !(filename.startsWith( filepath )) ){
            filename = filepath + filename;
        }
        
        return filename;
    }

    /**
     * @param request
     * @return Object
     * @throws Exception
     */
//    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
//        FileName fileName = new FileName();
//        fileName.setFileName( filepath );
//        return fileName;
//    }
}