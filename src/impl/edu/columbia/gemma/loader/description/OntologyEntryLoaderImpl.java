package edu.columbia.gemma.loader.description;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.description.ExternalDatabase;
import edu.columbia.gemma.common.description.ExternalDatabaseDao;
import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="ontologyEntryLoader"
 * @spring.property name="externalDatabaseDao" ref="externalDatabaseDao"
 * @spring.property name="ontologyEntryDao" ref="ontologyEntryDao"
 */
public class OntologyEntryLoaderImpl {

    protected static final Log log = LogFactory.getLog( OntologyEntryLoaderImpl.class );

    private ExternalDatabaseDao externalDatabaseDao;

    private OntologyEntryDao ontologyEntryDao;

    /**
     * @param oeCol
     */
    public void create( Collection<OntologyEntry> oeCol ) {

        int count = 0;
        for ( OntologyEntry oe : oeCol ) {

            getOntologyEntryDao().create( oe );
            count++;
            if ( count % 1000 == 0 ) log.info( count + " ontology entries persisted" );
        }

    }

    /**
     * @param ontologyEntry
     */
    public void create( OntologyEntry ontologyEntry ) {
        getOntologyEntryDao().create( ontologyEntry );
    }

    /**
     * @param externalDatabaseName
     * @return
     */
    public Object createExternalDatabase( ExternalDatabase ed ) {

        if ( getExternalDatabaseEntries().size() == 0 )
            this.getExternalDatabaseDao().create( ed );
        else {
            Collection<ExternalDatabase> externalDatabases = getExternalDatabaseEntries();

            for ( ExternalDatabase externalDb : externalDatabases ) {
                if ( externalDb.getName().equalsIgnoreCase( ed.getName() ) ) {
                    log.info( "external database " + ed.getName() + " already exists" );
                    return null;
                }

                this.getExternalDatabaseDao().create( ed );
                log.info( "external database with name: " + ed.getName() + " created." );
            }
        }
        return null;
    }

    /**
     * @return Returns the externalDatabaseDao.
     */
    public ExternalDatabaseDao getExternalDatabaseDao() {
        return externalDatabaseDao;
    }

    /**
     * @return
     */
    public Collection getExternalDatabaseEntries() {

        return this.getExternalDatabaseDao().findAllExternalDb();
    }

    /**
     * @return Returns the ontologyEntryDao.
     */
    public OntologyEntryDao getOntologyEntryDao() {
        return ontologyEntryDao;
    }

    /**
     * @param externalDatabaseDao The externalDatabaseDao to set.
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    /**
     * @param ontologyEntryDao The ontologyEntryDao to set.
     */
    public void setOntologyEntryDao( OntologyEntryDao ontologyEntryDao ) {
        this.ontologyEntryDao = ontologyEntryDao;
    }
}
