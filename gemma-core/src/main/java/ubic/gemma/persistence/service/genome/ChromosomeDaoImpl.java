/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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

import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;

/**
 * @author pavlidis
 * @version $Id$
 */
@Repository
public class ChromosomeDaoImpl extends ChromosomeDaoBase {

    @Autowired
    public ChromosomeDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ChromosomeDao#find(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Collection<Chromosome> find( String name, Taxon taxon ) {
        if ( StringUtils.isBlank( name ) ) {
            throw new IllegalArgumentException( "Name cannot be blank" );

        }
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Taxon cannot be null" );
        }

        if ( taxon.getId() == null ) {
            throw new IllegalArgumentException( "Taxon must be a persistent entity" );
        }

        String q = "from ChromosomeImpl c where c.name=:n and c.taxon=:t";
        List<Chromosome> results = this.getHibernateTemplate().findByNamedParam( q, new String[] { "n", "t" },
                new Object[] { name, taxon } );

        if ( results == null || results.isEmpty() ) return null;

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ChromosomeDao#findOrCreate(java.lang.String, ubic.gemma.model.genome.Taxon)
     */
    @Override
    public Chromosome findOrCreate( String name, Taxon taxon ) {
        Collection<Chromosome> hits = this.find( name, taxon );

        if ( hits == null || hits.isEmpty() ) {
            Chromosome c = Chromosome.Factory.newInstance( name, taxon );

            return create( c );
        }
        return hits.iterator().next();

    }

}