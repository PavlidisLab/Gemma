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
package ubic.gemma.loader.description;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.persister.Persister;
import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.model.common.description.OntologyEntry;

/**
 * A service to load OntologyEntries.
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="ontologyEntryLoader"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 */
public class OntologyEntryPersister implements Persister {

    protected static final Log log = LogFactory.getLog( OntologyEntryPersister.class );

    private PersisterHelper persisterHelper;

    public Collection<?> persist( Collection<?> oeCol ) {
        int i = 0;
        for ( Object oe : oeCol ) {
            persist( oe );
            if ( log.isDebugEnabled() && i > 0 && i % 5000 == 0 ) {
                log.debug( "Persisted " + i + " ontology entries from GO" );
            }
            i++;
        }
        return oeCol;
    }

    /**
     * @param oe
     */
    public Object persist( Object oe ) {
        assert oe instanceof OntologyEntry;
        assert ( ( OntologyEntry ) oe ).getExternalDatabase() != null;
        for ( OntologyEntry o : ( ( OntologyEntry ) oe ).getAssociations() ) {
            assert o.getExternalDatabase() != null;
        }
        return persisterHelper.persist( oe );
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }
}
