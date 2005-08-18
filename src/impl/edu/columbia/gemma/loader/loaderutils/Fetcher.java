package edu.columbia.gemma.loader.loaderutils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Fetcher {

    protected static Log log = LogFactory.getLog( Fetcher.class.getName() );
    protected String localBasePath = null;
    protected String baseDir = null;
    protected boolean success = false;
    protected boolean force = false;

}
