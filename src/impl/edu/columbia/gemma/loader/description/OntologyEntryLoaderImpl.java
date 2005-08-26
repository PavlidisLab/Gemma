package edu.columbia.gemma.loader.description;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;
import edu.columbia.gemma.loader.loaderutils.ParserAndLoaderTools;

/**
 * A service to load OntologyEntries (from any user interface).
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="ontologyEntryLoader"
 * @spring.property name="ontologyEntryDao" ref="ontologyEntryDao"
 */
public class OntologyEntryLoaderImpl {

    protected static final Log log = LogFactory.getLog( OntologyEntryLoaderImpl.class );

    private OntologyEntryDao ontologyEntryDao;

    /**
     * @param oeCol
     * @param dbEntry TODO
     */
    @SuppressWarnings("unchecked")
    public void create( Collection<OntologyEntry> oeCol ) {
        assert ontologyEntryDao != null;
        log.info( "persisting Gemma objects (if object exists it will not be persisted) ..." );

        int count = 0;
        for ( OntologyEntry oe : oeCol ) {
            ontologyEntryDao.findOrCreate( oe );
            count++;
            ParserAndLoaderTools.objectsPersistedUpdate( count, 1000, "Ontology Entries" );
        }
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
