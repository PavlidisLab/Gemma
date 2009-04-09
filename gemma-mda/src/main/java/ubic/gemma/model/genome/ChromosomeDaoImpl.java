/*
 * The Gemma project.
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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.genome;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;

import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ChromosomeDaoImpl extends ubic.gemma.model.genome.ChromosomeDaoBase {

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.ChromosomeDaoBase#find(ubic.gemma.model.genome.Chromosome)
     */
    @Override
    public Chromosome find( Chromosome chromosome ) {
        try {

            BusinessKey.checkValidKey( chromosome );
            Criteria queryObject = super.getSession( false ).createCriteria( Chromosome.class );
            BusinessKey.addRestrictions( queryObject, chromosome );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    String details = debug( results );
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size() + " "
                            + Chromosome.class.getName() + "s were found when executing query\n" + details );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( Chromosome ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    public Chromosome find( String name, Taxon taxon ) {
        if ( StringUtils.isBlank( name ) ) {
            throw new IllegalArgumentException( "Name cannot be blank" );

        }
        if ( taxon == null ) {
            throw new IllegalArgumentException( "Taxon cannot be blank" );
        }
        String q = "from ChromosomeImpl c where c.name=:n and c.taxon=:t";
        List result = this.getHibernateTemplate().findByNamedParam( q, new String[] { "n", "t" },
                new Object[] { name, taxon } );
        if ( result.size() == 1 ) {
            return ( Chromosome ) result.get( 0 );
        } else if ( result.size() > 1 ) {
            throw new IllegalStateException( "Multiple chromosomes match '" + name + "' in " + taxon );
        }
        return null;
    }

    @Override
    public Chromosome findOrCreate( Chromosome chromosome ) {
        Chromosome existing = this.find( chromosome );
        if ( existing != null ) {
            assert existing.getId() != null;
            return existing;
        }
        return create( chromosome );
    }

    private String debug( List results ) {
        StringBuilder buf = new StringBuilder();
        for ( Object object : results ) {
            buf.append( object + "\n" );
        }
        return buf.toString();
    }
}