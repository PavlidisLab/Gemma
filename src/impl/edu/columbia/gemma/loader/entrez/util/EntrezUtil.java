package edu.columbia.gemma.loader.entrez.util;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class EntrezUtil {

    protected static final Log log = LogFactory.getLog( EntrezUtil.class );

    private static String baseURL;

    static {
        Configuration config = null;
        try {
            config = new PropertiesConfiguration( "entrez.properties" );
        } catch ( ConfigurationException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        baseURL = ( String ) config.getProperty( "entrez.efetch.baseurl" );
    }

    public static String getBaseURL() {
        return baseURL;
    }
    

}
