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

package ubic.gemma.model.expression.designElement;

import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;

/**
 * @author pavlidis
 * @version $Id$
 */
public class CompositeSequenceDaoImpl extends ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase {

    private static Log log = LogFactory.getLog( CompositeSequenceDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#find(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public CompositeSequence find( CompositeSequence compositeSequence ) {

        if ( compositeSequence.getName() == null ) return null;

        try {

            Criteria queryObject = super.getSession( false ).createCriteria( CompositeSequence.class );

            queryObject.add( Restrictions.eq( "name", compositeSequence.getName() ) );

            // TODO make this use the full arraydesign
            // business key.
            queryObject.createCriteria( "arrayDesign" ).add(
                    Restrictions.eq( "name", compositeSequence.getArrayDesign().getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + CompositeSequence.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = results.iterator().next();
                }
            }
            return ( CompositeSequence ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#findOrCreate(ubic.gemma.model.expression.designElement.CompositeSequence)
     */
    @Override
    public CompositeSequence findOrCreate( CompositeSequence compositeSequence ) {
        if ( compositeSequence.getName() == null || compositeSequence.getArrayDesign() == null ) {
            throw new IllegalArgumentException( "compositeSequence must have name and arrayDesign." );
        }

        CompositeSequence existingCompositeSequence = this.find( compositeSequence );
        if ( existingCompositeSequence != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing compositeSequence: " + existingCompositeSequence );
            return existingCompositeSequence;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new compositeSequence: " + compositeSequence );
        return ( CompositeSequence ) create( compositeSequence );
    }

    @Override
    protected Integer handleCountAll() throws Exception {
        final String query = "select count(*) from CompositeSequenceImpl";
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
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct compositeSequence from CompositeSequenceImpl compositeSequence where compositeSequence.id in (:ids)";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "ids", ids );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    /**
     * FIXME duplicated code from ArrayDesignDao
     * 
     * @param id
     * @param queryString
     * @return
     */
    private Collection nativeQueryByIdReturnCollection( Long id, final String queryString ) {
        try {

            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            queryObject.setLong( "id", id );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    private Collection nativeQuery( final String queryString ) {
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createSQLQuery( queryString );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetRawSummary(ubic.gemma.model.expression.designElement.CompositeSequence)
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase
     */
    @Override
    protected Collection handleGetRawSummary( CompositeSequence compositeSequence ) throws Exception {
        if ( compositeSequence == null || compositeSequence.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = compositeSequence.getId();

        final String nativeQueryString = "SELECT de.ID as deID, de.NAME as deName, bs.NAME as bsName, bsDb.ACCESSION, bs2gp.BLAT_RESULT_FK,"
                + "geneProductRNA.ID as gpId,geneProductRNA.NAME as gpName,geneProductRNA.NCBI_ID as gpNcbi, geneProductRNA.GENE_FK, "
                + "geneProductRNA.TYPE, gene.ID as gId,gene.OFFICIAL_SYMBOL as gSymbol,gene.NCBI_ID as gNcbi "
                + " from "
                + "COMPOSITE_SEQUENCE cs join DESIGN_ELEMENT de on cs.ID=de.ID "
                + "left join BIO_SEQUENCE bs on BIOLOGICAL_CHARACTERISTIC_FK=bs.ID "
                + "left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=bs.ID "
                + "left join DATABASE_ENTRY bsDb on SEQUENCE_DATABASE_ENTRY_FK=bsDb.ID "
                + "left join CHROMOSOME_FEATURE geneProductRNA on (geneProductRNA.ID=bs2gp.GENE_PRODUCT_FK) "
                + "left join CHROMOSOME_FEATURE gene on (geneProductRNA.GENE_FK=gene.ID) " + "WHERE cs.ID = :id";
        Collection retVal = nativeQueryByIdReturnCollection( id, nativeQueryString );
        return retVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetRawSummary(java.util.Collection)
     * @see ubic.gemma.model.expression.arrayDesign.ArrayDesignDaoBase
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleGetRawSummary( Collection compositeSequences ) throws Exception {
        if ( compositeSequences == null || compositeSequences.size() == 0 ) return null;
        StringBuilder buf = new StringBuilder();

        for ( Iterator<CompositeSequence> it = compositeSequences.iterator(); it.hasNext(); ) {
            CompositeSequence compositeSequence = it.next();
            if ( compositeSequence == null || compositeSequence.getId() == null ) {
                throw new IllegalArgumentException();
            }
            long id = compositeSequence.getId();
            buf.append( id );
            if ( it.hasNext() ) buf.append( "," );
        }

        final String nativeQueryString = "SELECT de.ID as deID, de.NAME as deName, bs.NAME as bsName, bsDb.ACCESSION, bs2gp.BLAT_RESULT_FK,"
                + "geneProductRNA.ID as gpId,geneProductRNA.NAME as gpName,geneProductRNA.NCBI_ID as gpNcbi, geneProductRNA.GENE_FK, "
                + "geneProductRNA.TYPE, gene.ID as gId,gene.OFFICIAL_SYMBOL as gSymbol,gene.NCBI_ID as gNcbi "
                + " from "
                + "COMPOSITE_SEQUENCE cs join DESIGN_ELEMENT de on cs.ID=de.ID "
                + "left join BIO_SEQUENCE bs on BIOLOGICAL_CHARACTERISTIC_FK=bs.ID "
                + "left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=bs.ID "
                + "left join DATABASE_ENTRY bsDb on SEQUENCE_DATABASE_ENTRY_FK=bsDb.ID "
                + "left join CHROMOSOME_FEATURE geneProductRNA on (geneProductRNA.ID=bs2gp.GENE_PRODUCT_FK) "
                + "left join CHROMOSOME_FEATURE gene on (geneProductRNA.GENE_FK=gene.ID) "
                + "WHERE cs.ID IN ("
                + buf.toString() + ")";
        Collection retVal = nativeQuery( nativeQueryString );
        return retVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleGetRawSummary(ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    protected Collection handleGetRawSummary( ArrayDesign arrayDesign ) throws Exception {
        if ( arrayDesign == null || arrayDesign.getId() == null ) {
            throw new IllegalArgumentException();
        }
        long id = arrayDesign.getId();

        final String nativeQueryString = "SELECT de.ID as deID, de.NAME as deName, bs.NAME as bsName, bsDb.ACCESSION, bs2gp.BLAT_RESULT_FK,"
                + "geneProductRNA.ID as gpId,geneProductRNA.NAME as gpName,geneProductRNA.NCBI_ID as gpNcbi, geneProductRNA.GENE_FK, "
                + "geneProductRNA.TYPE, gene.ID as gId,gene.OFFICIAL_SYMBOL as gSymbol,gene.NCBI_ID as gNcbi "
                + " from "
                + "COMPOSITE_SEQUENCE cs join DESIGN_ELEMENT de on cs.ID=de.ID "
                + "left join BIO_SEQUENCE bs on BIOLOGICAL_CHARACTERISTIC_FK=bs.ID "
                + "left join BIO_SEQUENCE2_GENE_PRODUCT bs2gp on BIO_SEQUENCE_FK=bs.ID "
                + "left join DATABASE_ENTRY bsDb on SEQUENCE_DATABASE_ENTRY_FK=bsDb.ID "
                + "left join CHROMOSOME_FEATURE geneProductRNA on (geneProductRNA.ID=bs2gp.GENE_PRODUCT_FK) "
                + "left join CHROMOSOME_FEATURE gene on (geneProductRNA.GENE_FK=gene.ID) "
                + "WHERE cs.ARRAY_DESIGN_FK = :id";
        Collection retVal = nativeQueryByIdReturnCollection( id, nativeQueryString );
        return retVal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleFindByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByBioSequence( BioSequence bioSequence ) throws Exception {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl"
                + " cs where cs.biologicalCharacteristic = :id";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "id", bioSequence );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<CompositeSequence> findByGene( Gene gene ) {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl cs, BioSequenceImpl bs, BlatAssociationImpl ba, GeneProductImpl   gp, GeneImpl gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.geneProduct=gp  and ba.bioSequence=bs and gene = :gene";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "gene", gene );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<CompositeSequence> findByGene( Gene gene, ArrayDesign arrayDesign ) {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl cs, BioSequenceImpl bs, BlatAssociationImpl ba, GeneProductImpl   gp, GeneImpl gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.bioSequence=bs and ba.geneProduct=gp  and gene = :gene and cs.arrayDesign=:arrayDesign ";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "gene", gene );
            queryObject.setParameter( "arrayDesign", arrayDesign );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<Gene> handleGetGenes( CompositeSequence compositeSequence ) throws Exception {
        Collection<Gene> genes = null;
        final String queryString = "select distinct gene from CompositeSequenceImpl cs, BioSequenceImpl bs, BlatAssociationImpl ba, GeneProductImpl gp, GeneImpl gene  "
                + "where gp.gene=gene and cs.biologicalCharacteristic=bs and ba.bioSequence=bs and ba.geneProduct=gp and cs = :cs";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "cs", compositeSequence );
            genes = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

        return genes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.expression.designElement.CompositeSequenceDaoBase#handleFindByBioSequenceName(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindByBioSequenceName( String name ) throws Exception {
        Collection<CompositeSequence> compositeSequences = null;
        final String queryString = "select distinct cs from CompositeSequenceImpl"
                + " cs inner join cs.biologicalCharacteristic b where b.name = :name";
        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameter( "name", name );
            compositeSequences = queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
        return compositeSequences;
    }

    @Override
    protected void handleThaw( final Collection compositeSequences ) throws Exception {
        HibernateTemplate templ = this.getHibernateTemplate();
        templ.execute( new org.springframework.orm.hibernate3.HibernateCallback() {
            @SuppressWarnings("unchecked")
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                int i = 0;
                /*
                 * Note this code is copied from ArrayDesignDaoImpl
                 */
                int numToDo = compositeSequences.size();
                for ( CompositeSequence cs : ( Collection<CompositeSequence> ) compositeSequences ) {
                    BioSequence bs = cs.getBiologicalCharacteristic();
                    if ( bs == null ) {
                        continue;
                    }

                    session.update( bs );

                    bs.getTaxon();

                    if ( bs.getBioSequence2GeneProduct() == null ) {
                        continue;
                    }

                    for ( BioSequence2GeneProduct bs2gp : bs.getBioSequence2GeneProduct() ) {
                        if ( bs2gp == null ) {
                            continue;
                        }
                        GeneProduct geneProduct = bs2gp.getGeneProduct();
                        if ( geneProduct != null && geneProduct.getGene() != null ) {
                            Gene g = geneProduct.getGene();
                            g.getAliases().size();
                            session.evict( g );
                            session.evict( geneProduct );
                        }

                    }

                    if ( ++i % 2000 == 0 ) {
                        log.info( "Progress: " + i + "/" + numToDo + "..." );
                        try {
                            Thread.sleep( 10 );
                        } catch ( InterruptedException e ) {
                            //
                        }
                    }

                    session.update( cs.getArrayDesign() );
                    cs.getArrayDesign().getName();

                    if ( bs.getSequenceDatabaseEntry() != null ) session.evict( bs.getSequenceDatabaseEntry() );
                    session.evict( bs );
                }
                session.clear();
                return null;
            }
        }, true );

    }

}