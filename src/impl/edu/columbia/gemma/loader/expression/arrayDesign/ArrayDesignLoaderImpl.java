package edu.columbia.gemma.loader.expression.arrayDesign;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignDao;
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
 * @spring.property name="arrayDesignDao" ref="arrayDesignDao"
 */
public class ArrayDesignLoaderImpl {

    protected static final Log log = LogFactory.getLog( ArrayDesignLoaderImpl.class );

    private ArrayDesignDao arrayDesignDao;

    /**
     * @param adCol
     */
    public void create( Collection<ArrayDesign> adCol ) {

        log.info( "persisting Gemma objects (if object exists it will not be persisted) ..." );

        Collection<ArrayDesign> adColFromDatabase = getArrayDesignDao().findAllArrayDesigns();

        int count = 0;
        for ( ArrayDesign ad : adCol ) {
            assert arrayDesignDao != null;

            if ( adColFromDatabase.size() == 0 ) {
                getArrayDesignDao().create( ad );
                count++;
                ParserAndLoaderTools.objectsPersistedUpdate( count, 1000, "Array Design Entries" );

            } else {
                ArrayDesign tmp = null;
                for ( ArrayDesign adFromDatabase : adColFromDatabase ) {
                    if ( ad.getName().equals( adFromDatabase.getName() ) ) break;

                    tmp = ad;
                }
                if ( tmp != null ) {
                    getArrayDesignDao().create( tmp );
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
        getArrayDesignDao().create( arrayDesign );
    }

    /**
     * @return Returns the arrayDesignDao.
     */
    public ArrayDesignDao getArrayDesignDao() {
        return arrayDesignDao;
    }

    /**
     * @param arrayDesignDao The arrayDesignDao to set.
     */
    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }
}
