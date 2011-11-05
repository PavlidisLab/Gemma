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
package ubic.gemma.model.genome;

import java.util.Collection;

import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.SequenceBinUtils;

/**
 * @see ubic.gemma.model.genome.ProbeAlignedRegion
 * @author paul
 * @version $Id$
 */
@Repository
public class ProbeAlignedRegionDaoImpl extends ubic.gemma.model.genome.ProbeAlignedRegionDaoBase {

    @Autowired
    public ProbeAlignedRegionDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    // private Log log = LogFactory.getLog( ProbeAlignedRegionDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.ProbeAlignedRegionDaoBase#find(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    public Collection<ProbeAlignedRegion> find( BlatResult blatResult ) {
        Chromosome chrom = blatResult.getTargetChromosome();
        final Long targetStart = blatResult.getTargetStart();
        final Long targetEnd = blatResult.getTargetEnd();
        final String strand = blatResult.getStrand();
        return findByPosition( chrom, targetStart, targetEnd, strand );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.genome.ProbeAlignedRegionDaoBase#findByPhysicalLocation(ubic.gemma.model.genome.PhysicalLocation
     * )
     */
    @Override
    public Collection<ProbeAlignedRegion> findByPhysicalLocation( PhysicalLocation location ) {
        Chromosome chrom = location.getChromosome();
        final Long targetStart = location.getNucleotide();
        final Long targetEnd = location.getNucleotide() + location.getNucleotideLength();
        final String strand = location.getStrand();

        return findByPosition( chrom, targetStart, targetEnd, strand );
    }

    /**
     * 
     */
    public void thaw( final ProbeAlignedRegion par ) {
        if ( par == null || par.getId() == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.lock( par, LockMode.NONE );
                Hibernate.initialize( par );
                Hibernate.initialize( par.getProducts() );
                for ( ubic.gemma.model.genome.gene.GeneProduct gp : par.getProducts() ) {
                    Hibernate.initialize( gp.getExons() );
                    if ( gp.getPhysicalLocation() != null ) {
                        Hibernate.initialize( gp.getPhysicalLocation().getChromosome() );
                        Hibernate.initialize( gp.getPhysicalLocation().getChromosome().getTaxon() );
                    }
                }
                Taxon t = ( Taxon ) session.get( TaxonImpl.class, par.getTaxon().getId() );
                Hibernate.initialize( t );
                if ( t.getExternalDatabase() != null ) {
                    Hibernate.initialize( t.getExternalDatabase() );
                }
                session.evict( par );
                return null;
            }
        } );
    }
 
    private Collection<ProbeAlignedRegion> findByPosition( Chromosome chrom, final Long targetStart,
            final Long targetEnd, final String strand ) {

        // the 'fetch'es are so we don't get lazy loads (typical applications of this method)
        // Note: we could avoid the thaw if we did a fetch for the exons, but then this query becomes slow.

        String query = "select distinct par from ProbeAlignedRegionImpl as par inner join fetch par.physicalLocation pl "
                + "inner join fetch par.products prod inner join fetch pl.chromosome "
                + "where ((pl.nucleotide >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end) "
                + "OR (pl.nucleotide <= :start AND (pl.nucleotide + pl.nucleotideLength) >= :end) OR "
                + "(pl.nucleotide >= :start  AND pl.nucleotide <= :end) "
                + "OR  ((pl.nucleotide + pl.nucleotideLength) >= :start AND (pl.nucleotide + pl.nucleotideLength) <= :end )) "
                + "and pl.chromosome = :chromosome ";

        query = query + " and " + SequenceBinUtils.addBinToQuery( "pl", targetStart, targetEnd );

        String[] params;
        Object[] vals;
        if ( strand != null ) {
            query = query + " and pl.strand = :strand ";
            params = new String[] { "chromosome", "start", "end", "strand" };
            vals = new Object[] { chrom, targetStart, targetEnd, strand };
        } else {
            params = new String[] { "chromosome", "start", "end" };
            vals = new Object[] { chrom, targetStart, targetEnd };

        }

        Collection<ProbeAlignedRegion> results = getHibernateTemplate().findByNamedParam( query, params, vals );

        for ( ProbeAlignedRegion par : results ) {
            this.thaw( par );
        }

        return results;

    }

}