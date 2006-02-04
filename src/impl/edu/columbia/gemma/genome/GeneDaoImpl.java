/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package edu.columbia.gemma.genome;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.genome.Gene
 */
public class GeneDaoImpl extends edu.columbia.gemma.genome.GeneDaoBase {

    private static Log log = LogFactory.getLog( GeneDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.genome.GeneDaoBase#find(edu.columbia.gemma.genome.Gene)
     */
    @Override
    public Gene find( Gene gene ) {
        try {
            Criteria queryObject = super.getSession( false ).createCriteria( Gene.class );

            if ( gene.getOfficialSymbol() != null && gene.getTaxon() != null ) {
                queryObject.add( Restrictions.eq( "officialSymbol", gene.getOfficialSymbol() ) ).add(
                        Restrictions.eq( "taxon", gene.getTaxon() ) );
            }
            if ( gene.getNcbiId() != null ) {
                queryObject.add( Restrictions.eq( "ncbiId", gene.getNcbiId() ) );
            }

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + Gene.class.getName() + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( Gene ) results.iterator().next();
                }
            }
            return ( Gene ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.genome.GeneDaoBase#findOrCreate(edu.columbia.gemma.genome.Gene)
     */
    @Override
    public Gene findOrCreate( Gene gene ) {
        if ( ( gene.getOfficialSymbol() == null || gene.getTaxon() == null ) && gene.getNcbiId() == null ) {
            log.error( "Gene must have official symbol and taxon, or ncbiId" );
            return null;
        }
        Gene newGene = this.find( gene );
        if ( newGene != null ) {
            BeanPropertyCompleter.complete( newGene, gene );
            return newGene;
        }
        log.debug( "Creating new gene: " + gene.getName() );
        return ( Gene ) create( gene );
    }

}