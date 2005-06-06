package edu.columbia.gemma.loader.description;

import java.util.Collection;

import edu.columbia.gemma.common.description.OntologyEntry;
import edu.columbia.gemma.common.description.OntologyEntryDao;

public class OntologyEntryLoader {

    private OntologyEntryDao ontologyEntryDao;

    public void create( Collection col ) {
        getOntologyEntryDao().create( col );
    }

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
