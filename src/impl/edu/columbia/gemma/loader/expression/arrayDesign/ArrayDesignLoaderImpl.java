package edu.columbia.gemma.loader.expression.arrayDesign;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignExistsException;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;

/**
 * A service to load ArrayDesigns (from any user interface).
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="arrayDesignLoader"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 */
public class ArrayDesignLoaderImpl {

    protected static final Log log = LogFactory.getLog( ArrayDesignLoaderImpl.class );

    private ArrayDesignService arrayDesignService;

    /**
     * @param adCol
     */
    public void create( Collection<ArrayDesign> adCol ) {

        log.info( "persisting Gemma objects (if object exists it will not be persisted) ..." );

        Collection<ArrayDesign> adColFromDatabase = getArrayDesignService().getAllArrayDesigns();

        int count = 0;
        for ( ArrayDesign ad : adCol ) {
            assert arrayDesignService != null;

            if ( adColFromDatabase.size() == 0 ) {
                try {
                    getArrayDesignService().saveArrayDesign( ad );
                } catch ( ArrayDesignExistsException e ) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                count++;
                ParserAndLoaderTools.objectsPersistedUpdate( count, 1000, "Array Design Entries" );

            } else {
                ArrayDesign tmp = null;
                for ( ArrayDesign adFromDatabase : adColFromDatabase ) {
                    if ( ad.getName().equals( adFromDatabase.getName() ) ) {
                        tmp = null;
                        break;
                    }

                    tmp = ad;
                }
                if ( tmp != null ) {
                    try {
                        getArrayDesignService().saveArrayDesign( tmp );
                    } catch ( ArrayDesignExistsException e ) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    ;
                    count++;
                    ParserAndLoaderTools.objectsPersistedUpdate( count, 1000, "Array Design Entries" );
                }
            }
        }
    }

    /**
     * @param arrayDesign
     */
    public void create( ArrayDesign arrayDesign ) {
        try {
            getArrayDesignService().saveArrayDesign( arrayDesign );
        } catch ( ArrayDesignExistsException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @return Returns the arrayDesignService.
     */
    public ArrayDesignService getArrayDesignService() {
        return arrayDesignService;
    }

    /**
     * @param arrayDesignService The arrayDesignService to set.
     */
    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }
}
