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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Taxon;

import java.util.Collection;

/**
 * @author pavlidis
 */
@Repository
public class ChromosomeDaoImpl extends ChromosomeDaoBase {

    @Autowired
    public ChromosomeDaoImpl( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public Collection<Chromosome> find( String name, Taxon taxon ) {
        //noinspection unchecked
        return this.getSession().createQuery( "from ChromosomeImpl c where c.name=:n and c.taxon=:t" )
                .setParameter( "n", name ).setParameter( "t", taxon ).list();
    }

    @Override
    public Chromosome findOrCreate( String name, Taxon taxon ) {
        Collection<Chromosome> hits = this.find( name, taxon );
        if ( hits == null || hits.isEmpty() ) {
            Chromosome c = Chromosome.Factory.newInstance( name, taxon );
            return create( c );
        }
        return hits.iterator().next();
    }

    @Override
    public Chromosome findOrCreate( Chromosome entity ) {
        return this.findOrCreate( entity.getName(), entity.getSequence().getTaxon() );
    }

    @Override
    public Chromosome find( Chromosome entity ) {
        return this.find( entity.getName(), entity.getSequence().getTaxon() ).iterator().next();
    }
}