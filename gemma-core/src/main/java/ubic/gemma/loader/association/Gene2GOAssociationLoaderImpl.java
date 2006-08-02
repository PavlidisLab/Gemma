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
package ubic.gemma.loader.association;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.persister.Persister;
import ubic.gemma.loader.util.persister.PersisterHelper;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.Gene2GOAssociationDao;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.OntologyEntry;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @version $Id$
 * @spring.bean id="gene2GOAssociationLoader"
 * @spring.property name="gene2GOAssociationDao" ref="gene2GOAssociationDao"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 */
public class Gene2GOAssociationLoaderImpl implements Persister {

    protected static final Log log = LogFactory.getLog( Gene2GOAssociationLoaderImpl.class );

    private Gene2GOAssociationDao gene2GOAssociationDao;
    private PersisterHelper persisterHelper;

    /**
     * @param oeCol
     */
    public Collection<Object> persist( Collection<?> g2GoCol ) {
        assert gene2GOAssociationDao != null;
        Collection<Object> results = new HashSet<Object>();
        for ( Object ob : g2GoCol ) {
            if ( ob == null ) continue;
            assert ob instanceof Gene2GOAssociation;
            Gene2GOAssociation g2Go = ( Gene2GOAssociation ) ob;
            results.add( persist( g2Go ) );
        }
        return results;
    }

    /**
     * @param object
     */
    public Object persist( Object object ) {
        if ( object instanceof Collection ) {
            this.persist( ( Collection ) object );
        }
        assert object instanceof Gene2GOAssociation : "Expected a Gene2GOAssociation, got a "
                + object.getClass().getSimpleName();
        return persistGene2GOAssociation( ( Gene2GOAssociation ) object );
    }

    /**
     * @param gene2GOAssociationDao The gene2GOAssociationDao to set.
     */
    public void setGene2GOAssociationDao( Gene2GOAssociationDao gene2GOAssociationDao ) {
        this.gene2GOAssociationDao = gene2GOAssociationDao;
    }

    /**
     * @param persisterHelper The persisterHelper to set.
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param entity
     * @return
     */
    private Object persistGene2GOAssociation( Gene2GOAssociation entity ) {
        assert entity.getGene() != null;
        assert entity.getOntologyEntry() != null;
        entity.setGene( ( Gene ) persisterHelper.persist( entity.getGene() ) );
        entity.setOntologyEntry( ( OntologyEntry ) persisterHelper.persist( entity.getOntologyEntry() ) );
        entity.setSource( ( ExternalDatabase ) persisterHelper.persist( entity.getSource() ) );
        return gene2GOAssociationDao.create( entity );
    }
}
