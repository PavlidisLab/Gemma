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
package ubic.gemma.model.expression.arrayDesign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.BusinessKey;

/**
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.expression.arrayDesign.ArrayDesign
 */
public class ArrayDesignDaoImpl extends ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase {

    static Log log = LogFactory.getLog( ArrayDesignDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#find(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign find( ArrayDesign arrayDesign ) {
        try {

            BusinessKey.checkValidKey( arrayDesign );
            Criteria queryObject = super.getSession( false ).createCriteria( ArrayDesign.class );
            BusinessKey.addRestrictions( queryObject, arrayDesign );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    debug( results );
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException( results.size() + " "
                            + ArrayDesign.class.getName() + "s were found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( ArrayDesign ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * 
     */
    private void debug( List results ) {
        for ( Object ad : results ) {
            log.error( ad );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDao#findOrCreate(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public ArrayDesign findOrCreate( ArrayDesign arrayDesign ) {
        ArrayDesign existingArrayDesign = this.find( arrayDesign );
        if ( existingArrayDesign != null ) {
            assert existingArrayDesign.getId() != null;
            return existingArrayDesign;
        }
        log.debug( "Creating new arrayDesign: " + arrayDesign.getName() );
        return ( ArrayDesign ) create( arrayDesign );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequences(java.lang.Long)
     */
    @Override
    protected Integer handleNumCompositeSequences( Long id ) throws Exception {
        final String queryString = "select count (*) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return queryByIdReturnInteger( id, queryString );

    }

    /**
     * @param queryString
     * @return
     */
    private Integer queryReturnInteger( final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of 'Integer" + "' was found when executing query --> '"
                                    + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }

            return ( Integer ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param id
     * @param queryString
     * @return
     */
    private Integer queryByIdReturnInteger( Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "id", id );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of 'Integer" + "' was found when executing query --> '"
                                    + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }

            return ( Integer ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param ids
     * @param queryString
     * @return
     */
    private Integer queryByIdsReturnInteger( Collection<Long> ids, final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of 'Integer" + "' was found when executing query --> '"
                                    + queryString + "'" );
                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }

            return ( Integer ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    private Object queryByIdReturnObject( Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setFirstResult( 1 );
            queryObject.setMaxResults( 1 ); // this should gaurantee that there is only one or no element in the
            // collection returned
            queryObject.setParameter( "id", id );
            java.util.List results = queryObject.list();

            if ( ( results == null ) || ( results.size() == 0 ) ) return null;

            return results.iterator().next();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /**
     * @param id
     * @param queryString
     * @return
     */
    private Collection queryByIdReturnCollection( Long id, final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "id", id );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumReporters(java.lang.Long)
     */
    @Override
    protected Integer handleNumReporters( Long id ) throws Exception {
        final String queryString = "select count (*) from ArrayDesignImpl as ar inner join ar.compositeSequences as compositeSequences inner join compositeSequences.componentReporters as rep where ar.id = :id";
        return queryByIdReturnInteger( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    @Override
    protected Collection handleLoadCompositeSequences( Long id ) throws Exception {
        final String queryString = "select cs from CompositeSequenceImpl as cs inner join cs.arrayDesign as ar where ar.id = :id";
        return queryByIdReturnCollection( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleLoadCompositeSequences(java.lang.Long)
     */
    @Override
    protected Collection handleGetAllAssociatedBioAssays( Long id ) throws Exception {
        final String queryString = "select bioAssay from BioAssayImpl as bioAssay where bioAssay.arrayDesignUsed.id = :id";
        return queryByIdReturnCollection( id, queryString );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetTaxon(java.lang.Long) This only returns
     *      1 taxon, the 1st taxon as decided by the join which ever that is.
     */
    @Override
    protected Taxon handleGetTaxon( Long id ) throws Exception {

        final String queryString = "select bioC.taxon from ArrayDesignImpl as arrayD inner join arrayD.compositeSequences as compositeS inner join compositeS.biologicalCharacteristic as bioC inner join bioC.taxon where arrayD.id = :id";

        return ( Taxon ) queryByIdReturnObject( id, queryString );

    }

    @Override
    protected void handleThaw( final ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null ) return;
        if ( arrayDesign.getId() == null ) return;
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                session.update( arrayDesign );
                if ( arrayDesign.getCompositeSequences() == null ) return null;
                arrayDesign.getLocalFiles().size();
                arrayDesign.getExternalReferences().size();
                arrayDesign.getAuditTrail().getEvents().size();
                int numToDo = arrayDesign.getCompositeSequences().size();
                int i = 0;
                for ( CompositeSequence cs : arrayDesign.getCompositeSequences() ) {
                    if ( cs.getBiologicalCharacteristic() != null ) cs.getBiologicalCharacteristic().getTaxon();
                    if ( ++i % 2000 == 0 ) {
                        log.info( "Progress: " + i + "/" + numToDo + "..." );
                        try {
                            Thread.sleep( 100 );
                        } catch ( InterruptedException e ) {
                            //
                        }
                    }
                }
                return null;
            }
        }, true );

    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from ArrayDesignImpl";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( query );

            return ( Integer ) queryObject.iterate().next();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumBioSequencesById(long)
     */
    @Override
    protected long handleNumBioSequences( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs.biologicalCharacteristic) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic where ar.id = :id and "
                + "cs.biologicalCharacteristic.sequence IS NOT NULL";
        return queryByIdReturnInteger( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumBlatResultsById(long)
     */
    @Override
    protected long handleNumBlatResults( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct bs2gp) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BioSequence2GeneProductImpl as bs2gp "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and ar.id = :id";
        return queryByIdReturnInteger( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumGeneProductsById(long)
     */
    @Override
    protected long handleNumGenes( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BioSequence2GeneProductImpl bs2gp, GeneImpl gene "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                + "bs2gp.geneProduct.id=gene.products.id and ar.id = :id";
        return queryByIdReturnInteger( id, queryString );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleDeleteGeneProductAssociations( ArrayDesign arrayDesign ) {
        final String sequenceQueryString = "select ba from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatAssociationImpl ba "
                + "where ba.bioSequence = bs and ad=:arrayDesign";
        org.hibernate.Query queryObject = super.getSession( false ).createQuery( sequenceQueryString );
        queryObject.setParameter( "arrayDesign", arrayDesign );
        final Collection<BlatAssociation> toBeRemoved = queryObject.list();

        if ( toBeRemoved.size() == 0 ) {
            log.info( "No old associations to be removed for " + arrayDesign );
            return;
        }
        log.info( "Have " + toBeRemoved.size() + " BlatAssociations to remove for " + arrayDesign );
        this.getHibernateTemplate().execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                int i = 0;
                for ( java.util.Iterator entityIterator = toBeRemoved.iterator(); entityIterator.hasNext(); ) {
                    session.delete( entityIterator.next() );
                    if ( ++i % 1000 == 0 ) {
                        log.info( "Delete Progress: " + i + "/" + toBeRemoved.size() + "..." );
                        try {
                            Thread.sleep( 100 );
                        } catch ( InterruptedException e ) {
                            //
                        }
                    }
                }
                return null;
            }
        }, true );
        log.info( "Done deleting." );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleGetExpressionExperimentsById(long)
     */
    @Override
    protected Collection handleGetExpressionExperiments( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select distinct ee from ArrayDesignImpl ad, BioAssayImpl ba, ExpressionExperimentImpl ee where ba.arrayDesignUsed=ad and ee.bioAssays.id=ba.id and ad.id = :id";
        return queryByIdReturnCollection( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBioSequences()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences() throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic where " + "cs.biologicalCharacteristic.sequence IS NOT NULL";
        return queryReturnInteger( queryString ).longValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBlatResults()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults() throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BlatResultImpl as blat "
                + "where blat.querySequence=cs.biologicalCharacteristic";
        return queryReturnInteger( queryString ).longValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithGenes()
     */
    @Override
    protected long handleNumAllCompositeSequenceWithGenes() throws Exception {
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BioSequence2GeneProductImpl bs2gp, GeneImpl gene "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct.id=gene.products.id";
        return queryReturnInteger( queryString ).longValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllGenes()
     */
    @Override
    protected long handleNumAllGenes() throws Exception {
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BioSequence2GeneProductImpl bs2gp, GeneImpl gene "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and " + "bs2gp.geneProduct.id=gene.products.id";
        return queryReturnInteger( queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBioSequences(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleNumAllCompositeSequenceWithBioSequences( Collection ids ) throws Exception {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic where ar.id in (:ids) and "
                + "cs.biologicalCharacteristic.sequence IS NOT NULL";
        return queryByIdsReturnInteger( ids, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithBlatResults(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleNumAllCompositeSequenceWithBlatResults( Collection ids ) throws Exception {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BlatResultImpl as blat "
                + "where blat.querySequence=cs.biologicalCharacteristic and ar.id in (:ids)";
        return queryByIdsReturnInteger( ids, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllCompositeSequenceWithGenes(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleNumAllCompositeSequenceWithGenes( Collection ids ) throws Exception {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BioSequence2GeneProductImpl bs2gp, GeneImpl gene "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                + "bs2gp.geneProduct.id=gene.products.id and ar.id in (:id)";
        return queryByIdsReturnInteger( ids, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumAllGenes(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected long handleNumAllGenes( Collection ids ) throws Exception {
        if ( ids == null || ids.size() == 0 ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select count (distinct gene) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BioSequence2GeneProductImpl bs2gp, GeneImpl gene "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                + "bs2gp.geneProduct.id=gene.products.id and ar.id in (:ids)";
        return queryByIdsReturnInteger( ids, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithBioSequence(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithBioSequences( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic where ar.id = :id and "
                + "cs.biologicalCharacteristic.sequence IS NOT NULL";
        return queryByIdReturnInteger( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithBlatResults()
     */
    @Override
    protected long handleNumCompositeSequenceWithBlatResults( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BlatResultImpl as blat "
                + "where blat.querySequence=cs.biologicalCharacteristic and ar.id = :id";
        return queryByIdReturnInteger( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleNumCompositeSequenceWithGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected long handleNumCompositeSequenceWithGenes( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select count (distinct cs) from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + "inner join cs.biologicalCharacteristic, BioSequence2GeneProductImpl bs2gp, GeneImpl gene "
                + "where bs2gp.bioSequence=cs.biologicalCharacteristic and "
                + "bs2gp.geneProduct.id=gene.products.id and ar.id = :id";
        return queryByIdReturnInteger( id, queryString );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void handleDeleteAlignmentData( ArrayDesign arrayDesign ) throws Exception {
        final String sequenceQueryString = "select br from ArrayDesignImpl ad inner join ad.compositeSequences as cs "
                + "inner join cs.biologicalCharacteristic bs, BlatResultImpl br "
                + "where br.querySequence = bs and ad=:arrayDesign";
        org.hibernate.Query queryObject = super.getSession( false ).createQuery( sequenceQueryString );
        queryObject.setParameter( "arrayDesign", arrayDesign );
        final Collection<BlatResult> toBeRemoved = queryObject.list();

        if ( toBeRemoved.size() == 0 ) {
            log.info( "No old alignments to be removed for " + arrayDesign );
            return;
        }
        log.info( "Have " + toBeRemoved.size() + " BlatResults to remove for " + arrayDesign );
        this.getHibernateTemplate().deleteAll( toBeRemoved );
        log.info( "Done deleting." );
    }

    @Override
    protected Collection handleLoadValueObjects( Collection ids ) throws Exception {
        Collection<ArrayDesignValueObject> vo = new ArrayList<ArrayDesignValueObject>();
        // sanity check
        if ( ids == null || ids.size() == 0 ) {
            return vo;
        }

        // get the expression experiment counts
        Map eeCounts = this.getExpressionExperimentCountMap();
        // get the composite sequence counts
        // Map csCounts = this.getCompositeSequenceCountMap();

        // removed join from taxon as it is slowing down the system
        final String queryString = "select ad.id as id, " + " ad.name as name, " + " ad.shortName as shortName, "
                + " ad.technologyType " + " from ArrayDesignImpl as ad " + " where ad.id in (:ids) "
                + " group by ad order by ad.name";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            while ( list.next() ) {
                ArrayDesignValueObject v = new ArrayDesignValueObject();
                v.setId( list.getLong( 0 ) );
                v.setName( list.getString( 1 ) );
                v.setShortName( list.getString( 2 ) );
                TechnologyType color = ( TechnologyType ) list.get( 3 );
                v.setColor( color.getValue() );
                // v.setTaxon( list.getString( 3 ) );

                // v.setDesignElementCount( (Long) csCounts.get( v.getId() ) );
                v.setExpressionExperimentCount( ( Long ) eeCounts.get( v.getId() ) );

                vo.add( v );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return vo;
    }

    @Override
    protected Collection handleLoadAllValueObjects() throws Exception {

        // get the expression experiment counts
        Map eeCounts = this.getExpressionExperimentCountMap();
        // get the composite sequence counts
        // Map csCounts = this.getCompositeSequenceCountMap();
        Collection<ArrayDesignValueObject> vo = new ArrayList<ArrayDesignValueObject>();
        // removed join from taxon as it is slowing down the system
        final String queryString = "select ad.id as id, " + " ad.name as name, " + " ad.shortName as shortName, "
                + " ad.technologyType " + " " + " from ArrayDesignImpl as ad " + " " + " group by ad order by ad.name";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );

            if ( list != null ) {
                while ( list.next() ) {
                    ArrayDesignValueObject v = new ArrayDesignValueObject();
                    v.setId( list.getLong( 0 ) );
                    v.setName( list.getString( 1 ) );
                    v.setShortName( list.getString( 2 ) );

                    TechnologyType color = ( TechnologyType ) list.get( 3 );
                    if ( color != null ) v.setColor( color.getValue() );
                    // v.setTaxon( list.getString( 3 ) );

                    // v.setDesignElementCount( (Long) csCounts.get( v.getId() ) );
                    v.setExpressionExperimentCount( ( Long ) eeCounts.get( v.getId() ) );

                    vo.add( v );
                }
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return vo;
    }

    /**
     * queries the database and gets the number of expression experiments per ArrayDesign
     * 
     * @return Map
     */
    private Map getExpressionExperimentCountMap() {
        final String queryString = "select ad.id, count(distinct ee) from ArrayDesignImpl ad, BioAssayImpl ba, ExpressionExperimentImpl ee where ba.arrayDesignUsed=ad and ee.bioAssays.id=ba.id group by ad";

        Map<Long, Long> eeCount = new HashMap<Long, Long>();
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
            while ( list.next() ) {
                Long id = list.getLong( 0 );
                Long count = new Long( list.getInteger( 1 ) );
                eeCount.put( id, count );
            }
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return eeCount;
    }

    // /**
    // * queries the database and gets the number of composite sequences per ArrayDesign
    // * @return Map
    // */
    // private Map getCompositeSequenceCountMap() {
    // final String queryString = "select ad.id, count(distinct cs) from ArrayDesignImpl ad inner join
    // ad.compositeSequences as cs group by ad";
    //        
    // Map<Long,Long> csCount = new HashMap<Long,Long>();
    // try {
    // org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
    // ScrollableResults list = queryObject.scroll( ScrollMode.FORWARD_ONLY );
    // while ( list.next() ) {
    // Long id = list.getLong( 0 );
    // Long count = new Long(list.getInteger( 1 ));
    // csCount.put( id, count );
    // }
    // } catch ( org.hibernate.HibernateException ex ) {
    // throw super.convertHibernateAccessException( ex );
    // }
    // return csCount;
    // }

    public ArrayDesign arrayDesignValueObjectToEntity( ArrayDesignValueObject arrayDesignValueObject ) {
        Long id = arrayDesignValueObject.getId();
        return ( ArrayDesign ) this.load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutBioSequences(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutBioSequences( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        final String queryString = "select distinct cs from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " left join cs.biologicalCharacteristic where ar.id = :id and "
                + " cs.biologicalCharacteristic.sequence IS NULL";
        return queryByIdReturnCollection( arrayDesign.getId(), queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutBlatResults(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutBlatResults( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select distinct cs from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " left join cs.biologicalCharacteristic left join BlatResultImpl as blat on blat.querySequence=cs.biologicalCharacteristic "
                + " where ar.id = :id";
        return queryByIdReturnCollection( id, queryString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase#handleCompositeSequenceWithoutGenes(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleCompositeSequenceWithoutGenes( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();
        final String queryString = "select distinct cs from  CompositeSequenceImpl as cs inner join cs.arrayDesign as ar "
                + " left join cs.biologicalCharacteristic left join BioSequence2GeneProductImpl bs2gp on bs2gp.bioSequence=cs.biologicalCharacteristic "
                + " left join GeneImpl gene on bs2gp.geneProduct.id=gene.products.id " + " where ar.id = :id";
        return queryByIdReturnCollection( id, queryString );
    }
}