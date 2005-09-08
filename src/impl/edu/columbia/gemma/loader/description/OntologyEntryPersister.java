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
package edu.columbia.gemma.loader.description;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;
import edu.columbia.gemma.loader.loaderutils.Persister;

/**
 * A service to load OntologyEntries (from any user interface).
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="ontologyEntryLoader"
 * @spring.property name="ontologyEntryDao" ref="ontologyEntryDao"
 */
public class OntologyEntryPersister implements Persister {

    protected static final Log log = LogFactory.getLog( OntologyEntryPersister.class );

    private OntologyEntryDao ontologyEntryDao;

    public Collection<Object> persist( Collection<Object> oeCol ) {
        assert ontologyEntryDao != null;
        log.info( "persisting Gemma objects (if object exists it will not be persisted) ..." );

        int count = 0;
        for ( Object oe : oeCol ) {
            persist( oe );
            ParserAndLoaderTools.objectsPersistedUpdate( count, 1000, "Ontology Entries" );
            count++;
        }
        return oeCol;
    }

    /**
     * @param oe
     */
    public Object persist( Object oe ) {
        assert oe instanceof OntologyEntry;
        return ontologyEntryDao.findOrCreate( ( OntologyEntry ) oe );
    }

    /**
     * @param ontologyEntry
     */
    public void create( OntologyEntry ontologyEntry ) {
        getOntologyEntryDao().create( ontologyEntry );
    }

    /**
     * @return Returns the ontologyEntryDao.
     */
    public OntologyEntryDao getOntologyEntryDao() {
        return ontologyEntryDao;
    }

    /**
     * @param ontologyEntryDao The ontologyEntryDao to set.
     */
    public void setOntologyEntryDao( OntologyEntryDao ontologyEntryDao ) {
        this.ontologyEntryDao = ontologyEntryDao;
    }
}
