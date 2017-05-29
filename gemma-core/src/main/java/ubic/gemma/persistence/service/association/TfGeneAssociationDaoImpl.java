/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.association;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.association.TfGeneAssociation;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;

/**
 * @author paul
 */
@Repository
public class TfGeneAssociationDaoImpl extends AbstractDao<TfGeneAssociation> implements TfGeneAssociationDao {

    @Autowired
    public TfGeneAssociationDaoImpl( SessionFactory sessionFactory ) {
        super( TfGeneAssociation.class, sessionFactory );
    }

    @Override
    public Collection<TfGeneAssociation> findByTargetGene( Gene gene ) {
        //noinspection unchecked
        return this.getSession().createQuery(
                "from PazarAssociationImpl p inner join fetch p.secondGene inner join fetch p.firstGene where p.secondGene = :g" )
                .setParameter( "g", gene ).list();
    }

    @Override
    public Collection<TfGeneAssociation> findByTf( Gene tf ) {
        //noinspection unchecked
        return this.getSession().createQuery(
                "from PazarAssociationImpl p inner join fetch p.secondGene inner join fetch p.firstGene where p.firstGene = :g" )
                .setParameter( "g", tf ).list();
    }

    @Override
    public void removeAll() {
        for ( TfGeneAssociation tf : this.loadAll() ) {
            this.remove( tf );
        }
    }

    @Override
    public Collection<TfGeneAssociation> load( Collection<Long> ids ) {
        //noinspection unchecked
        return this.getSession().createQuery(
                "from PazarAssociationImpl  p inner join fetch p.secondGene inner join fetch p.firstGene where p.id in (:ids)" )
                .setParameterList( "ids", ids ).list();
    }

    @Override
    public void update( Collection<TfGeneAssociation> entities ) {
        throw new UnsupportedOperationException( "Immutable, update not supported" );
    }

    @Override
    public void update( TfGeneAssociation entity ) {
        throw new UnsupportedOperationException( "Immutable, update not supported" );
    }

}
