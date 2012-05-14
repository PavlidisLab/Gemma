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
package ubic.gemma.model.expression.experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.orm.hibernate3.HibernateCallback;

import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ExpressionExperiment</code>.
 * 
 * @see ExpressionExperiment
 * @version $Id$
 * @author paul based on generated code
 */
public abstract class ExpressionExperimentDaoBase extends BioAssaySetDaoImpl<ExpressionExperiment> implements
        ExpressionExperimentDao {

    /**
     * @see ExpressionExperimentDao#countAll()
     */
    public Integer countAll() {
        try {
            return this.handleCountAll();
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'ExpressionExperimentDao.countAll()' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#create(int, Collection)
     */
    public Collection<? extends ExpressionExperiment> create( final Collection<? extends ExpressionExperiment> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNewSession( new HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( Iterator<? extends ExpressionExperiment> entityIterator = entities.iterator(); entityIterator
                        .hasNext(); ) {
                    create( entityIterator.next() );
                }
                return null;
            }
        } );
        return entities;
    }

    /**
     * @see ExpressionExperimentDao#create(int transform, ExpressionExperiment)
     */
    public ExpressionExperiment create( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.create - 'expressionExperiment' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperiment );
        return expressionExperiment;
    }

    /**
     * @see ExpressionExperimentDao#expressionExperimentValueObjectToEntity(ExpressionExperimentValueObject,
     *      ExpressionExperiment)
     */
    public void expressionExperimentValueObjectToEntity( ExpressionExperimentValueObject source,
            ExpressionExperiment target, boolean copyIfNull ) {
        if ( copyIfNull || source.getSource() != null ) {
            target.setSource( source.getSource() );
        }
        if ( copyIfNull || source.getShortName() != null ) {
            target.setShortName( source.getShortName() );
        }
        if ( copyIfNull || source.getName() != null ) {
            target.setName( source.getName() );
        }
    }

    /**
     * @see ExpressionExperimentDao#findByBibliographicReference(Long)
     */
    public Collection<ExpressionExperiment> findByBibliographicReference( final Long bibRefID ) {
        try {
            return this.handleFindByBibliographicReference( bibRefID );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByBibliographicReference(Long bibRefID)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentDao#findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)
     */
    public ExpressionExperiment findByBioMaterial( final ubic.gemma.model.expression.biomaterial.BioMaterial bm ) {
        try {
            return this.handleFindByBioMaterial( bm );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial bm)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#findByBioMaterials(Collection)
     */
    public Collection<ExpressionExperiment> findByBioMaterials( final Collection<BioMaterial> bioMaterials ) {
        try {
            return this.handleFindByBioMaterials( bioMaterials );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByBioMaterials(Collection bioMaterials)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentDao#findByExpressedGene(ubic.gemma.model.genome.Gene, Double)
     */
    public Collection<ExpressionExperiment> findByExpressedGene( final ubic.gemma.model.genome.Gene gene,
            final Double rank ) {
        try {
            return this.handleFindByExpressedGene( gene, rank );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByExpressedGene(ubic.gemma.model.genome.Gene gene, Double rank)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#findByFactorValue(FactorValue)
     */
    public ExpressionExperiment findByFactorValue( final FactorValue factorValue ) {
        try {
            return this.handleFindByFactorValue( factorValue );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByFactorValue(FactorValue factorValue)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentDao#findByFactorValues(Collection)
     */
    public Collection<ExpressionExperiment> findByFactorValues( final Collection<FactorValue> factorValues ) {
        try {
            return this.handleFindByFactorValues( factorValues );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByFactorValues(Collection factorValues)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentDao#findByGene(ubic.gemma.model.genome.Gene)
     */
    public Collection<ExpressionExperiment> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindByGene( gene );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByGene(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#findByInvestigator(int, String, Contact)
     */

    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperiment> findByInvestigator( final String queryString, final Contact investigator ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( investigator );
        argNames.add( "investigator" );
        List<?> results = this.getHibernateTemplate().findByNamedParam( queryString,
                argNames.toArray( new String[argNames.size()] ), args.toArray() );

        return ( Collection<ExpressionExperiment> ) results;
    }

    /**
     * @see ExpressionExperimentDao#findByInvestigator(int, Contact)
     */

    public Collection<ExpressionExperiment> findByInvestigator( final Contact investigator ) {
        return this
                .findByInvestigator(
                        "from InvestigationImpl i inner join Contact c on c in elements(i.investigators) or c == i.owner where c == :investigator",
                        investigator );
    }

    /**
     * @see ExpressionExperimentDao#findByName(int, String)
     */
    public ExpressionExperiment findByName( final String name ) {
        return this.findByName( "from ExpressionExperimentImpl a where a.name=:name", name );
    }

    /**
     * @see ExpressionExperimentDao#findByName(int, String, String)
     */

    public ExpressionExperiment findByName( final String queryString, final String name ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( name );
        argNames.add( "name" );
        Set<ExpressionExperiment> results = new LinkedHashSet<ExpressionExperiment>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        ExpressionExperiment result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ExpressionExperiment" + "' was found when executing query --> '"
                            + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see ExpressionExperimentDao#findByParentTaxon(ubic.gemma.model.genome.Taxon)
     */
    public Collection<ExpressionExperiment> findByParentTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByParentTaxon( taxon );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByByTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /*
     * 
     */
    public ExpressionExperiment findByQuantitationType( QuantitationType quantitationType ) {
        try {
            return this.handleFindByQuantitationType( quantitationType );
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'ExpressionExperimentDao.findByQuantitationType  --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentDao#findByShortName(int, String)
     */
    public ExpressionExperiment findByShortName( final String shortName ) {
        return this.findByShortName( "from ExpressionExperimentImpl a where a.shortName=:shortName", shortName );
    }

    /**
     * @see ExpressionExperimentDao#findByShortName(int, String, String)
     */

    public ExpressionExperiment findByShortName( final String queryString, final String shortName ) {
        List<String> argNames = new ArrayList<String>();
        List<Object> args = new ArrayList<Object>();
        args.add( shortName );
        argNames.add( "shortName" );
        Set<ExpressionExperiment> results = new LinkedHashSet<ExpressionExperiment>( this.getHibernateTemplate()
                .findByNamedParam( queryString, argNames.toArray( new String[argNames.size()] ), args.toArray() ) );
        ExpressionExperiment result = null;

        if ( results.size() > 1 ) {
            throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                    "More than one instance of 'ExpressionExperiment" + "' was found when executing query --> '"
                            + queryString + "'" );
        } else if ( results.size() == 1 ) {
            result = results.iterator().next();
        }

        return result;
    }

    /**
     * @see ExpressionExperimentDao#findByTaxon(ubic.gemma.model.genome.Taxon)
     */
    public Collection<ExpressionExperiment> findByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        try {
            return this.handleFindByTaxon( taxon );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByTaxon(ubic.gemma.model.genome.Taxon taxon)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getAnnotationCounts(Collection)
     */
    public Map<Long, Integer> getAnnotationCounts( final Collection<Long> ids ) {
        try {
            return this.handleGetAnnotationCounts( ids );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getAnnotationCounts(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getArrayDesignAuditEvents(Collection)
     */
    @Deprecated
    public Map<Long, Map<Long, Collection<AuditEvent>>> getArrayDesignAuditEvents( final Collection<Long> ids ) {
        try {
            return this.handleGetArrayDesignAuditEvents( ids );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getArrayDesignAuditEvents(Collection ids)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getAuditEvents(Collection)
     */
    public Map<Long, Collection<AuditEvent>> getAuditEvents( final Collection<Long> ids ) {
        try {
            return this.handleGetAuditEvents( ids );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getAuditEvents(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getBioAssayCountById(long)
     */
    public Integer getBioAssayCountById( final long id ) {
        try {
            return this.handleGetBioAssayCountById( id );
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'ExpressionExperimentDao.getBioAssayCountById(long id)' --> "
                    + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getBioMaterialCount(ExpressionExperiment)
     */
    public Integer getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetBioMaterialCount( expressionExperiment );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getBioMaterialCount(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getDesignElementDataVectorCountById(long)
     */
    public Integer getDesignElementDataVectorCountById( final long id ) {
        try {
            return this.handleGetDesignElementDataVectorCountById( id );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getDesignElementDataVectorCountById(long id)' --> " + th,
                    th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getDesignElementDataVectors(Collection,
     *      ubic.gemma.model.common.quantitationtype.QuantitationType)
     */
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<CompositeSequence> designElements,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        try {
            return this.handleGetDesignElementDataVectors( designElements, quantitationType );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getDesignElementDataVectors(Collection designElements, ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getDesignElementDataVectors(Collection)
     */
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<QuantitationType> quantitationTypes ) {
        try {
            return this.handleGetDesignElementDataVectors( quantitationTypes );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getDesignElementDataVectors(Collection quantitationTypes)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getLastArrayDesignUpdate(Collection, Class)
     */
    public Map<Long, Date> getLastArrayDesignUpdate( final Collection<ExpressionExperiment> expressionExperiments ) {
        try {
            return this.handleGetLastArrayDesignUpdate( expressionExperiments );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getLastArrayDesignUpdate(Collection expressionExperiments, Class type)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getLastArrayDesignUpdate(ExpressionExperiment, Class)
     */
    public Date getLastArrayDesignUpdate( final ExpressionExperiment ee ) {
        try {
            return this.handleGetLastArrayDesignUpdate( ee );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getLastArrayDesignUpdate(ExpressionExperiment ee, Class eventType)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getMaskedPreferredQuantitationType(ExpressionExperiment)
     */
    public ubic.gemma.model.common.quantitationtype.QuantitationType getMaskedPreferredQuantitationType(
            final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetMaskedPreferredQuantitationType( expressionExperiment );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getMaskedPreferredQuantitationType(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getPerTaxonCount()
     */
    public Map<Taxon, Long> getPerTaxonCount() {
        try {
            return this.handleGetPerTaxonCount();
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'ExpressionExperimentDao.getPerTaxonCount()' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getPopulatedFactorCounts(Collection)
     */
    public Map<Long, Integer> getPopulatedFactorCounts( final Collection<Long> ids ) {
        try {
            return this.handleGetPopulatedFactorCounts( ids );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getPopulatedFactorCounts(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getPopulatedFactorCountsExcludeBatch(Collection)
     */
    public Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( final Collection<Long> ids ) {
        try {
            return this.handleGetPopulatedFactorCountsExcludeBatch( ids );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getPopulatedFactorCountsExcludeBatch(Collection ids)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getPreferredDesignElementDataVectorCount(ExpressionExperiment)
     */
    public Integer getProcessedExpressionVectorCount( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetProcessedExpressionVectorCount( expressionExperiment );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getPreferredDesignElementDataVectorCount(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getQuantitationTypeCountById(Long)
     */
    public Map<QuantitationType, Integer> getQuantitationTypeCountById( final Long Id ) {
        try {
            return this.handleGetQuantitationTypeCountById( Id );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getQuantitationTypeCountById(Long Id)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getQuantitationTypes(ExpressionExperiment)
     */
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetQuantitationTypes( expressionExperiment );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getQuantitationTypes(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getQuantitationTypes(ExpressionExperiment,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        try {
            return this.handleGetQuantitationTypes( expressionExperiment, arrayDesign );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getQuantitationTypes(ExpressionExperiment expressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getSampleRemovalEvents(Collection)
     */
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            final Collection<ExpressionExperiment> expressionExperiments ) {

        try {
            return this.handleGetSampleRemovalEvents( expressionExperiments );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getSampleRemovalEvents(Collection expressionExperiments)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      Integer)
     */
    public Collection<DesignElementDataVector> getSamplingOfVectors(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, final Integer limit ) {
        try {
            return this.handleGetSamplingOfVectors( quantitationType, limit );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, Integer limit)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getSubSets(ExpressionExperiment)
     */
    public Collection<ExpressionExperimentSubSet> getSubSets( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleGetSubSets( expressionExperiment );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getSubSets(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#getTaxon(Long)
     */
    public ubic.gemma.model.genome.Taxon getTaxon( final BioAssaySet ee ) {
        try {
            return this.handleGetTaxon( ee );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.getTaxon(Long ExpressionExperimentID)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#load(int, Long)
     */

    public ExpressionExperiment load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( ExpressionExperimentImpl.class, id );
        return ( ExpressionExperiment ) entity;
    }

    /**
     * @see ExpressionExperimentDao#load(Collection)
     */
    public Collection<ExpressionExperiment> load( final Collection<Long> ids ) {
        try {
            return this.handleLoad( ids );
        } catch ( Throwable th ) {
            throw new RuntimeException( "Error performing 'ExpressionExperimentDao.load(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#loadAll(int)
     */

    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperiment> loadAll() {
        final Collection<?> results = this.getHibernateTemplate().loadAll( ExpressionExperimentImpl.class );
        return ( Collection<ExpressionExperiment> ) results;
    }

    /**
     * @see ExpressionExperimentDao#loadValueObjects(Collection, boolean)
     */
    public Collection<ExpressionExperimentValueObject> loadValueObjects( final Collection<Long> ids,
            boolean maintainOrder ) {
        try {
            return this.handleLoadValueObjects( ids, maintainOrder );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.loadValueObjects(Collection ids)' --> " + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#remove(Long)
     */

    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.remove - 'id' can not be null" );
        }
        ExpressionExperiment entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(Collection)
     */

    public void remove( Collection<? extends ExpressionExperiment> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ExpressionExperimentDao#remove(ExpressionExperiment)
     */
    public void remove( ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.remove - 'expressionExperiment' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentDao#thaw(ExpressionExperiment)
     */
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleThaw( expressionExperiment, true );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.thaw(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#thawBioAssays(ExpressionExperiment)
     */
    public ExpressionExperiment thawBioAssays( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleThaw( expressionExperiment, false );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.thawBioAssays(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    public ExpressionExperiment thawBioAssaysLiter( final ExpressionExperiment expressionExperiment ) {
        try {
            return this.handleThawLiter( expressionExperiment, false );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.thawBioAssays(ExpressionExperiment expressionExperiment)' --> "
                            + th, th );
        }
    }

    /**
     * @see ExpressionExperimentDao#toExpressionExperimentValueObject(ExpressionExperiment)
     */
    public ExpressionExperimentValueObject toExpressionExperimentValueObject( final ExpressionExperiment entity ) {
        final ExpressionExperimentValueObject target = new ExpressionExperimentValueObject();
        this.toExpressionExperimentValueObject( entity, target );
        return target;
    }

    /**
     * @see ExpressionExperimentDao#toExpressionExperimentValueObject(ExpressionExperiment,
     *      ExpressionExperimentValueObject)
     */
    public void toExpressionExperimentValueObject( ExpressionExperiment source, ExpressionExperimentValueObject target ) {
        target.setId( source.getId() );
        target.setName( source.getName() );
        target.setSource( source.getSource() );
        // No conversion for target.accession (can't convert
        // source.getAccession():ubic.gemma.model.common.description.DatabaseEntry to String)
        target.setShortName( source.getShortName() );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(Collection)
     */
    public void update( final Collection<? extends ExpressionExperiment> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNewSession( new HibernateCallback<Object>() {
            public Object doInHibernate( org.hibernate.Session session ) throws org.hibernate.HibernateException {
                for ( Iterator<? extends ExpressionExperiment> entityIterator = entities.iterator(); entityIterator
                        .hasNext(); ) {
                    update( entityIterator.next() );
                }
                return null;
            }
        } );
    }

    /**
     * @see ExpressionExperimentDao#update(ExpressionExperiment)
     */
    public void update( ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.update - 'expressionExperiment' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperiment );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract Integer handleCountAll() throws Exception;

    /**
     * Performs the core logic for {@link #findByBibliographicReference(Long)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByBibliographicReference( Long bibRefID )
            throws Exception;

    /**
     * Performs the core logic for {@link #findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ExpressionExperiment handleFindByBioMaterial(
            ubic.gemma.model.expression.biomaterial.BioMaterial bm ) throws Exception;

    /**
     * Performs the core logic for {@link #findByBioMaterials(Collection)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByBioMaterials( Collection<BioMaterial> bioMaterials )
            throws Exception;

    /**
     * Performs the core logic for {@link #findByExpressedGene(ubic.gemma.model.genome.Gene, Double)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByExpressedGene( ubic.gemma.model.genome.Gene gene,
            Double rank ) throws Exception;

    /**
     * Performs the core logic for {@link #findByFactorValue(FactorValue)}
     */
    protected abstract ExpressionExperiment handleFindByFactorValue( FactorValue factorValue ) throws Exception;

    /**
     * Performs the core logic for {@link #findByFactorValues(Collection)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByFactorValues( Collection<FactorValue> factorValues )
            throws Exception;

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByGene( ubic.gemma.model.genome.Gene gene )
            throws Exception;

    /**
     * Performs the core logic for {@link #findByParentTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByParentTaxon( ubic.gemma.model.genome.Taxon taxon )
            throws Exception;

    protected abstract ExpressionExperiment handleFindByQuantitationType( QuantitationType quantitationType )
            throws Exception;

    /**
     * Performs the core logic for {@link #findByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByTaxon( ubic.gemma.model.genome.Taxon taxon )
            throws Exception;

    /**
     * Performs the core logic for {@link #getAnnotationCounts(Collection)}
     */
    protected abstract Map<Long, Integer> handleGetAnnotationCounts( Collection<Long> ids ) throws Exception;

    /**
     * Performs the core logic for {@link #getArrayDesignAuditEvents(Collection)}
     */
    protected abstract Map<Long, Map<Long, Collection<AuditEvent>>> handleGetArrayDesignAuditEvents(
            Collection<Long> ids ) throws Exception;

    /**
     * Performs the core logic for {@link #getAuditEvents(Collection)}
     */
    protected abstract Map<Long, Collection<AuditEvent>> handleGetAuditEvents( Collection<Long> ids ) throws Exception;

    /**
     * Performs the core logic for {@link #getBioAssayCountById(long)}
     */
    protected abstract Integer handleGetBioAssayCountById( long id ) throws Exception;

    /**
     * Performs the core logic for {@link #getBioMaterialCount(ExpressionExperiment)}
     */
    protected abstract Integer handleGetBioMaterialCount( ExpressionExperiment expressionExperiment ) throws Exception;

    /**
     * Performs the core logic for {@link #getDesignElementDataVectorCountById(long)}
     */
    protected abstract Integer handleGetDesignElementDataVectorCountById( long id ) throws Exception;

    /**
     * Performs the core logic for
     * {@link #getDesignElementDataVectors(Collection, ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<CompositeSequence> designElements,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) throws Exception;

    /**
     * Performs the core logic for {@link #getDesignElementDataVectors(Collection)}
     */
    protected abstract Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<QuantitationType> quantitationTypes ) throws Exception;

    /**
     * Performs the core logic for {@link #getLastArrayDesignUpdate(Collection, Class)}
     */
    protected abstract Map<Long, Date> handleGetLastArrayDesignUpdate(
            Collection<ExpressionExperiment> expressionExperiments ) throws Exception;

    /**
     * Performs the core logic for {@link #getLastArrayDesignUpdate(ExpressionExperiment, Class)}
     */
    protected abstract Date handleGetLastArrayDesignUpdate( ExpressionExperiment ee ) throws Exception;

    /**
     * Performs the core logic for {@link #getMaskedPreferredQuantitationType(ExpressionExperiment)}
     */
    protected abstract ubic.gemma.model.common.quantitationtype.QuantitationType handleGetMaskedPreferredQuantitationType(
            ExpressionExperiment expressionExperiment ) throws Exception;

    /**
     * Performs the core logic for {@link #getPerTaxonCount()}
     */
    protected abstract Map<Taxon, Long> handleGetPerTaxonCount() throws Exception;

    /**
     * Performs the core logic for {@link #getPopulatedFactorCounts(Collection)}
     */
    protected abstract Map<Long, Integer> handleGetPopulatedFactorCounts( Collection<Long> ids ) throws Exception;

    /**
     * Performs the core logic for {@link #getPopulatedFactorCountsExcludeBatch(Collection)}
     */
    protected abstract Map<Long, Integer> handleGetPopulatedFactorCountsExcludeBatch( Collection<Long> ids )
            throws Exception;

    /**
     * Performs the core logic for {@link #getProcessedExpressionVectorCount(ExpressionExperiment)}
     */
    protected abstract Integer handleGetProcessedExpressionVectorCount( ExpressionExperiment expressionExperiment )
            throws Exception;

    /**
     * Performs the core logic for {@link #getQuantitationTypeCountById(Long)}
     */
    protected abstract Map<QuantitationType, Integer> handleGetQuantitationTypeCountById( Long Id ) throws Exception;

    /**
     * Performs the core logic for {@link #getQuantitationTypes(ExpressionExperiment)}
     */
    protected abstract Collection<QuantitationType> handleGetQuantitationTypes(
            ExpressionExperiment expressionExperiment ) throws Exception;

    /**
     * Performs the core logic for
     * {@link #getQuantitationTypes(ExpressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract Collection<QuantitationType> handleGetQuantitationTypes(
            ExpressionExperiment expressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign )
            throws Exception;

    /**
     * Performs the core logic for {@link #getSampleRemovalEvents(Collection)}
     */
    protected abstract Map<ExpressionExperiment, Collection<AuditEvent>> handleGetSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments ) throws Exception;

    /**
     * Performs the core logic for
     * {@link #getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType, Integer)}
     */
    protected abstract Collection<DesignElementDataVector> handleGetSamplingOfVectors(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, Integer limit )
            throws Exception;

    /**
     * Performs the core logic for {@link #getSubSets(ExpressionExperiment)}
     */
    protected abstract Collection<ExpressionExperimentSubSet> handleGetSubSets(
            ExpressionExperiment expressionExperiment ) throws Exception;

    /**
     * Performs the core logic for {@link #getTaxon(BioAssaySet)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleGetTaxon( BioAssaySet ee ) throws Exception;

    /**
     * Performs the core logic for {@link #load(Collection)}
     */
    protected abstract Collection<ExpressionExperiment> handleLoad( Collection<Long> ids ) throws Exception;

    /**
     * Performs the core logic for {@link #loadValueObjects(Collection, boolean)}
     * 
     * @param maintainOrder If true, order of valueObjects returned will correspond to order of ids passed in.
     */
    protected abstract Collection<ExpressionExperimentValueObject> handleLoadValueObjects( Collection<Long> ids,
            boolean maintainOrder ) throws Exception;

    /**
     * Performs the core logic for {@link #thaw(ExpressionExperiment)}
     */
    protected abstract ExpressionExperiment handleThaw( ExpressionExperiment expressionExperiment, boolean thawVectors )
            throws Exception;

    protected abstract ExpressionExperiment handleThawLiter( ExpressionExperiment expressionExperiment,
            boolean thawVectors ) throws Exception;

}