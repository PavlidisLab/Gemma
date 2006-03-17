/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.loader.expression.arrayDesign;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.persister.Persister;
import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

/**
 * A service to load ArrayDesigns (from any user interface).
 * <hr>
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="arrayDesignPersister"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 */
public class ArrayDesignPersister implements Persister {

    protected static final Log log = LogFactory.getLog( ArrayDesignPersister.class );

    private PersisterHelper persisterHelper;

    /**
     * @param adCol
     */
    @SuppressWarnings("unchecked")
    public Collection<Object> persist( Collection<Object> adCol ) {

        log.info( "persisting Gemma objects (if object exists it will not be persisted) ..." );

        int count = 0;
        for ( Object ob : adCol ) {
            assert ob instanceof ArrayDesign;
            persisterHelper.persist( ob );
            count++;
        }

        return adCol;
    }

    /**
     * @param object
     * @throws ArrayDesignExistsException
     */
    public Object persist( Object object ) {
        assert object instanceof ArrayDesign;
        return persisterHelper.persist( object );
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

}
