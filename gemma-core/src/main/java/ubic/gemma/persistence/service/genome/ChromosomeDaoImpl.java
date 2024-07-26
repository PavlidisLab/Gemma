/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.genome;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.Chromosome</code>.
 * </p>
 *
 * @see ubic.gemma.model.genome.Chromosome
 */
@Repository
public class ChromosomeDaoImpl extends AbstractDao<Chromosome> implements ChromosomeDao {

    @Autowired
    public ChromosomeDaoImpl( SessionFactory sessionFactory ) {
        super( Chromosome.class, sessionFactory );
    }

    @Override
    public Collection<Chromosome> find( String name, Taxon taxon ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Chromosome c where c.name=:n and c.taxon=:t" ).setParameter( "n", name )
                .setParameter( "t", taxon ).list();
    }

    @Override
    protected Chromosome findByBusinessKey( Chromosome entity ) {
        Collection<Chromosome> hits = this.find( entity.getName(), entity.getSequence().getTaxon() );
        if ( hits.isEmpty() ) {
            return null;
        } else {
            return hits.iterator().next();
        }
    }
}