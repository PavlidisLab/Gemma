package edu.columbia.gemma.controller;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
 * Copyright (c) 2004 Columbia University Generic Controller used to decipher which LoaderService is called.
 * 
 * @author keshav
 * @version $Id$
 */
public class LoaderController extends SimpleFormController {

    private BulkCreatorProxyFactory bulkCreatorProxyFactory;
    /** Logger for this class and subclasses */
    protected final Log logger = LogFactory.getLog( getClass() );
    Configuration conf;
    String errorview;
    String filepath;

    /**
     * @throws ConfigurationException
     */
    public LoaderController() throws ConfigurationException {
        conf = new PropertiesConfiguration( "loader.properties" );
        filepath = conf.getString( "loadercontroller.filepath" );
        errorview = conf.getString( "loader.error.view" );

    }

    /**
     * @param name
     * @return String
     */
    //TODO Make private if not used elsewhere
    public String cleanString( String name ) {
        name = name.trim();
        name = name.toLowerCase();

        return name;
    }

    /**
     * @return BulkCreatorProxyFactory.
     */
    public BulkCreatorProxyFactory getBulkCreatorProxyFactory() {
        return bulkCreatorProxyFactory;
    }

    /**
     * Obtains filename to be read from the form.
     * 
     * @param command
     * @return ModelAndView
     */
    public ModelAndView onSubmit( Object command ) {
        String filename = ( ( FileName ) command ).getFileName();
        filename = cleanString( filename );
        BulkCreator proxy = null;
        Map myModel = new HashMap();
        try {
            proxy = determineService( getBulkCreatorProxyFactory(), filename );
        } catch ( IllegalArgumentException e1 ) {
            return new ModelAndView( errorview, "model", myModel );
        } catch ( IllegalAccessException e1 ) {
            return new ModelAndView( errorview, "model", myModel );
        } catch ( InvocationTargetException e1 ) {
            return new ModelAndView( errorview, "model", myModel );
        }
        String view;
        filename = fullyQualifiedName( filepath, filename, ".txt" );
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
     * Determines the type of service to be used (by reflection).
     * 
     * @param obj
     * @param filename
     * @return BulkCreator
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private BulkCreator determineService( Object obj, String name ) throws IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {

        name = "get" + name.toLowerCase();
        BulkCreator service = null;
        Class type = obj.getClass();
        final Method[] methods = type.getMethods();
        String methodName;
        for ( int i = 0; i < methods.length; i++ ) {
            methodName = methods[i].getName();

            if ( !methodName.toLowerCase().startsWith( name ) ) continue;

            service = ( BulkCreator ) methods[i].invoke( obj, null );
            break;
        }
        return service;
    }

    /**
     * @param name
     * @return String
     */
    private String fullyQualifiedName( String prefix, String name, String suffix ) {
        if ( prefix.equals( null ) ) {
            name = filepath + name;
        } else {
            name = prefix + name;
        }
        if ( !( name.endsWith( suffix ) ) ) name = name.concat( suffix );

        return name;
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