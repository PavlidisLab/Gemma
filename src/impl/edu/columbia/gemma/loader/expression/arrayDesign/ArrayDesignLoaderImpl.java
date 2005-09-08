/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package edu.columbia.gemma.loader.expression.arrayDesign;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.expression.arrayDesign.ArrayDesign;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignExistsException;
import edu.columbia.gemma.expression.arrayDesign.ArrayDesignService;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;
import edu.columbia.gemma.loader.loaderutils.Persister;

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
public class ArrayDesignLoaderImpl implements Persister {

    protected static final Log log = LogFactory.getLog( ArrayDesignLoaderImpl.class );

    private ArrayDesignService arrayDesignService;

    /**
     * @param adCol
     */
    @SuppressWarnings("unchecked")
    public void persist( Collection<Object> adCol ) {

        log.info( "persisting Gemma objects (if object exists it will not be persisted) ..." );

        Collection<ArrayDesign> adColFromDatabase = getArrayDesignService().getAllArrayDesigns();

        int count = 0;
        for ( Object ob : adCol ) {
            assert arrayDesignService != null;
            assert ob instanceof ArrayDesign;
            ArrayDesign ad = ( ArrayDesign ) ob;

            if ( adColFromDatabase.size() == 0 ) {
                persist( ad );
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
                    persist( tmp );
                    count++;
                    ParserAndLoaderTools.objectsPersistedUpdate( count, 1000, "Array Design Entries" );
                }
            }
        }
    }

    /**
     * @param object
     * @throws ArrayDesignExistsException
     */
    public void persist( Object object ) {
        assert object instanceof ArrayDesign;
        try {
            getArrayDesignService().saveArrayDesign( ( ArrayDesign ) object );
        } catch ( ArrayDesignExistsException e ) {
            log.error( e, e );
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
