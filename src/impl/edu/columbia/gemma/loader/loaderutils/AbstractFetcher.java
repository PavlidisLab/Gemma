package edu.columbia.gemma.loader.loaderutils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class AbstractFetcher {

    protected static Log log = LogFactory.getLog( AbstractFetcher.class.getName() );
    protected String localBasePath = null;
    protected String baseDir = null;
    protected boolean success = false;
    protected boolean force = false;

    /**
     * Set to true if downloads should proceed even if the file already exists.
     * 
     * @param force
     */
    public void setForce( boolean force ) {
        this.force = force;
    }

}
