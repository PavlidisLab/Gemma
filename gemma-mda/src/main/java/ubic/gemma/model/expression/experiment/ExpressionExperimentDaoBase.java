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
    @Override
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
    @Override
    public Collection<? extends ExpressionExperiment> create( final Collection<? extends ExpressionExperiment> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNewSession( new HibernateCallback<Object>() {
            @Override
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

    @Override
    public ExpressionExperiment create( final ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.create - 'expressionExperiment' can not be null" );
        }
        this.getHibernateTemplate().save( expressionExperiment );
        return expressionExperiment;
    }

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

    @Override
    public Collection<ExpressionExperiment> findByBibliographicReference( final Long bibRefID ) {
        return this.handleFindByBibliographicReference( bibRefID );
    }

    @Override
    public ExpressionExperiment findByBioMaterial( final ubic.gemma.model.expression.biomaterial.BioMaterial bm ) {
        return this.handleFindByBioMaterial( bm );

    }

    @Override
    public Collection<ExpressionExperiment> findByBioMaterials( final Collection<BioMaterial> bioMaterials ) {
        return this.handleFindByBioMaterials( bioMaterials );

    }

    @Override
    public Collection<ExpressionExperiment> findByExpressedGene( final ubic.gemma.model.genome.Gene gene,
            final Double rank ) {
        return this.handleFindByExpressedGene( gene, rank );

    }

    /**
     * @see ExpressionExperimentDao#findByFactor(ExperimentalFactor)
     */
    @Override
    public ExpressionExperiment findByFactor( final ExperimentalFactor factor ) {
        try {
            return this.handleFindByFactor( factor );
        } catch ( Throwable th ) {
            throw new RuntimeException(
                    "Error performing 'ExpressionExperimentDao.findByFactor( ExperimentalFactor factor )' --> " + th,
                    th );
        }
    }

    @Override
    public ExpressionExperiment findByFactorValue( final Long factorValueId ) {
        return this.handleFindByFactorValue( factorValueId );
    }

    @Override
    public ExpressionExperiment findByFactorValue( final FactorValue factorValue ) {
        return this.handleFindByFactorValue( factorValue );

    }

    @Override
    public Collection<ExpressionExperiment> findByFactorValues( final Collection<FactorValue> factorValues ) {
        return this.handleFindByFactorValues( factorValues );

    }

    @Override
    public Collection<ExpressionExperiment> findByGene( final ubic.gemma.model.genome.Gene gene ) {
        return this.handleFindByGene( gene );

    }

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

    @Override
    public Collection<ExpressionExperiment> findByInvestigator( final Contact investigator ) {
        return this
                .findByInvestigator(
                        "from InvestigationImpl i inner join Contact c on c in elements(i.investigators) or c == i.owner where c == :investigator",
                        investigator );
    }

    @Override
    public ExpressionExperiment findByName( final String name ) {
        return this.findByName( "from ExpressionExperimentImpl a where a.name=:name", name );
    }

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

    @Override
    public Collection<ExpressionExperiment> findByParentTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleFindByParentTaxon( taxon );

    }

    @Override
    public ExpressionExperiment findByQuantitationType( QuantitationType quantitationType ) {
        return this.handleFindByQuantitationType( quantitationType );

    }

    @Override
    public ExpressionExperiment findByShortName( final String shortName ) {
        return this.findByShortName( "from ExpressionExperimentImpl a where a.shortName=:shortName", shortName );
    }

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

    @Override
    public Collection<ExpressionExperiment> findByTaxon( final ubic.gemma.model.genome.Taxon taxon ) {
        return this.handleFindByTaxon( taxon );

    }

    @Override
    public Map<Long, Integer> getAnnotationCounts( final Collection<Long> ids ) {
        return this.handleGetAnnotationCounts( ids );

    }

    @Override
    public Map<Long, Collection<AuditEvent>> getAuditEvents( final Collection<Long> ids ) {
        return this.handleGetAuditEvents( ids );

    }

    @Override
    public Integer getBioAssayCountById( final long id ) {
        return this.handleGetBioAssayCountById( id );

    }

    @Override
    public Integer getBioMaterialCount( final ExpressionExperiment expressionExperiment ) {
        return this.handleGetBioMaterialCount( expressionExperiment );

    }

    @Override
    public Integer getDesignElementDataVectorCountById( final long id ) {
        return this.handleGetDesignElementDataVectorCountById( id );

    }

    @Override
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<CompositeSequence> designElements,
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType ) {
        return this.handleGetDesignElementDataVectors( designElements, quantitationType );

    }

    @Override
    public Collection<DesignElementDataVector> getDesignElementDataVectors(
            final Collection<QuantitationType> quantitationTypes ) {
        return this.handleGetDesignElementDataVectors( quantitationTypes );

    }

    @Override
    public Map<Long, Date> getLastArrayDesignUpdate( final Collection<ExpressionExperiment> expressionExperiments ) {
        return this.handleGetLastArrayDesignUpdate( expressionExperiments );

    }

    @Override
    public Date getLastArrayDesignUpdate( final ExpressionExperiment ee ) {
        return this.handleGetLastArrayDesignUpdate( ee );

    }

    @Override
    public ubic.gemma.model.common.quantitationtype.QuantitationType getMaskedPreferredQuantitationType(
            final ExpressionExperiment expressionExperiment ) {
        return this.handleGetMaskedPreferredQuantitationType( expressionExperiment );

    }

    @Override
    public Map<Taxon, Long> getPerTaxonCount() {
        return this.handleGetPerTaxonCount();

    }

    /**
     * @see ExpressionExperimentDao#getPopulatedFactorCounts(Collection)
     */
    @Override
    public Map<Long, Integer> getPopulatedFactorCounts( final Collection<Long> ids ) {
        return this.handleGetPopulatedFactorCounts( ids );

    }

    /**
     * @see ExpressionExperimentDao#getPopulatedFactorCountsExcludeBatch(Collection)
     */
    @Override
    public Map<Long, Integer> getPopulatedFactorCountsExcludeBatch( final Collection<Long> ids ) {
        return this.handleGetPopulatedFactorCountsExcludeBatch( ids );

    }

    /**
     * @see ExpressionExperimentDao#getPreferredDesignElementDataVectorCount(ExpressionExperiment)
     */
    @Override
    public Integer getProcessedExpressionVectorCount( final ExpressionExperiment expressionExperiment ) {
        return this.handleGetProcessedExpressionVectorCount( expressionExperiment );

    }

    /**
     * @see ExpressionExperimentDao#getQuantitationTypeCountById(Long)
     */
    @Override
    public Map<QuantitationType, Integer> getQuantitationTypeCountById( final Long Id ) {
        return this.handleGetQuantitationTypeCountById( Id );

    }

    /**
     * @see ExpressionExperimentDao#getQuantitationTypes(ExpressionExperiment)
     */
    @Override
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment ) {

        return this.handleGetQuantitationTypes( expressionExperiment );

    }

    /**
     * @see ExpressionExperimentDao#getQuantitationTypes(ExpressionExperiment,
     *      ubic.gemma.model.expression.arrayDesign.ArrayDesign)
     */
    @Override
    public Collection<QuantitationType> getQuantitationTypes( final ExpressionExperiment expressionExperiment,
            final ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign ) {
        return this.handleGetQuantitationTypes( expressionExperiment, arrayDesign );

    }

    /**
     * @see ExpressionExperimentDao#getSampleRemovalEvents(Collection)
     */
    @Override
    public Map<ExpressionExperiment, Collection<AuditEvent>> getSampleRemovalEvents(
            final Collection<ExpressionExperiment> expressionExperiments ) {

        return this.handleGetSampleRemovalEvents( expressionExperiments );

    }

    /**
     * @see ExpressionExperimentDao#getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType,
     *      Integer)
     */
    @Override
    public Collection<DesignElementDataVector> getSamplingOfVectors(
            final ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, final Integer limit ) {
        return this.handleGetSamplingOfVectors( quantitationType, limit );

    }

    /**
     * @see ExpressionExperimentDao#getSubSets(ExpressionExperiment)
     */
    @Override
    public Collection<ExpressionExperimentSubSet> getSubSets( final ExpressionExperiment expressionExperiment ) {
        return this.handleGetSubSets( expressionExperiment );

    }

    /**
     * @see ExpressionExperimentDao#getTaxon(Long)
     */
    @Override
    public ubic.gemma.model.genome.Taxon getTaxon( final BioAssaySet ee ) {
        return this.handleGetTaxon( ee );

    }

    /**
     * @see ExpressionExperimentDao#load(int, Long)
     */

    @Override
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
    @Override
    public Collection<ExpressionExperiment> load( final Collection<Long> ids ) {
        return this.handleLoad( ids );

    }

    /**
     * @see ExpressionExperimentDao#loadAll(int)
     */

    @Override
    @SuppressWarnings("unchecked")
    public Collection<ExpressionExperiment> loadAll() {
        final Collection<?> results = this.getHibernateTemplate().loadAll( ExpressionExperimentImpl.class );
        return ( Collection<ExpressionExperiment> ) results;
    }

    /**
     * @see ExpressionExperimentDao#loadValueObjects(Collection, boolean)
     */
    @Override
    public Collection<ExpressionExperimentValueObject> loadValueObjects( final Collection<Long> ids,
            boolean maintainOrder ) {
        return this.handleLoadValueObjects( ids, maintainOrder );

    }

    /**
     * @see ExpressionExperimentDao#remove(Long)
     */

    @Override
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

    @Override
    public void remove( Collection<? extends ExpressionExperiment> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see ExpressionExperimentDao#remove(ExpressionExperiment)
     */
    @Override
    public void remove( ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.remove - 'expressionExperiment' can not be null" );
        }
        this.getHibernateTemplate().delete( expressionExperiment );
    }

    /**
     * @see ExpressionExperimentDao#thaw(ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment thaw( final ExpressionExperiment expressionExperiment ) {
        return this.handleThaw( expressionExperiment, true );

    }

    /**
     * @see ExpressionExperimentDao#thawBioAssays(ExpressionExperiment)
     */
    @Override
    public ExpressionExperiment thawBioAssays( final ExpressionExperiment expressionExperiment ) {
        return this.handleThaw( expressionExperiment, false );

    }

    @Override
    public ExpressionExperiment thawBioAssaysLiter( final ExpressionExperiment expressionExperiment ) {
        return this.handleThawLiter( expressionExperiment, false );

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
    @Override
    public void update( final Collection<? extends ExpressionExperiment> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNewSession( new HibernateCallback<Object>() {
            @Override
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
    @Override
    public void update( ExpressionExperiment expressionExperiment ) {
        if ( expressionExperiment == null ) {
            throw new IllegalArgumentException( "ExpressionExperiment.update - 'expressionExperiment' can not be null" );
        }
        this.getHibernateTemplate().update( expressionExperiment );
    }

    /**
     * Performs the core logic for {@link #countAll()}
     */
    protected abstract Integer handleCountAll();

    /**
     * Performs the core logic for {@link #findByBibliographicReference(Long)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByBibliographicReference( Long bibRefID );

    /**
     * Performs the core logic for {@link #findByBioMaterial(ubic.gemma.model.expression.biomaterial.BioMaterial)}
     */
    protected abstract ExpressionExperiment handleFindByBioMaterial(
            ubic.gemma.model.expression.biomaterial.BioMaterial bm );

    /**
     * Performs the core logic for {@link #findByBioMaterials(Collection)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByBioMaterials( Collection<BioMaterial> bioMaterials );

    /**
     * Performs the core logic for {@link #findByExpressedGene(ubic.gemma.model.genome.Gene, Double)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByExpressedGene( ubic.gemma.model.genome.Gene gene,
            Double rank );

    /**
     * Performs the core logic for {@link #findByFactor(ExperimentalFactor)}
     */
    protected abstract ExpressionExperiment handleFindByFactor( ExperimentalFactor factor );

    /**
     * Performs the core logic for {@link #findByFactorValue(FactorValue)}
     */
    protected abstract ExpressionExperiment handleFindByFactorValue( FactorValue factorValue );

    /**
     * Performs the core logic for {@link #findByFactorValue(FactorValue)}
     */
    protected abstract ExpressionExperiment handleFindByFactorValue( Long factorValueId );

    /**
     * Performs the core logic for {@link #findByFactorValues(Collection)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByFactorValues( Collection<FactorValue> factorValues );

    /**
     * Performs the core logic for {@link #findByGene(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByGene( ubic.gemma.model.genome.Gene gene );

    /**
     * Performs the core logic for {@link #findByParentTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByParentTaxon( ubic.gemma.model.genome.Taxon taxon );

    protected abstract ExpressionExperiment handleFindByQuantitationType( QuantitationType quantitationType );

    /**
     * Performs the core logic for {@link #findByTaxon(ubic.gemma.model.genome.Taxon)}
     */
    protected abstract Collection<ExpressionExperiment> handleFindByTaxon( ubic.gemma.model.genome.Taxon taxon );

    /**
     * Performs the core logic for {@link #getAnnotationCounts(Collection)}
     */
    protected abstract Map<Long, Integer> handleGetAnnotationCounts( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getArrayDesignAuditEvents(Collection)}
     */
    protected abstract Map<Long, Map<Long, Collection<AuditEvent>>> handleGetArrayDesignAuditEvents(
            Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getAuditEvents(Collection)}
     */
    protected abstract Map<Long, Collection<AuditEvent>> handleGetAuditEvents( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getBioAssayCountById(long)}
     */
    protected abstract Integer handleGetBioAssayCountById( long id );

    /**
     * Performs the core logic for {@link #getBioMaterialCount(ExpressionExperiment)}
     */
    protected abstract Integer handleGetBioMaterialCount( ExpressionExperiment expressionExperiment );

    /**
     * Performs the core logic for {@link #getDesignElementDataVectorCountById(long)}
     */
    protected abstract Integer handleGetDesignElementDataVectorCountById( long id );

    /**
     * Performs the core logic for
     * {@link #getDesignElementDataVectors(Collection, ubic.gemma.model.common.quantitationtype.QuantitationType)}
     */
    protected abstract Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<CompositeSequence> designElements,
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType );

    /**
     * Performs the core logic for {@link #getDesignElementDataVectors(Collection)}
     */
    protected abstract Collection<DesignElementDataVector> handleGetDesignElementDataVectors(
            Collection<QuantitationType> quantitationTypes );

    /**
     * Performs the core logic for {@link #getLastArrayDesignUpdate(Collection, Class)}
     */
    protected abstract Map<Long, Date> handleGetLastArrayDesignUpdate(
            Collection<ExpressionExperiment> expressionExperiments );

    /**
     * Performs the core logic for {@link #getLastArrayDesignUpdate(ExpressionExperiment, Class)}
     */
    protected abstract Date handleGetLastArrayDesignUpdate( ExpressionExperiment ee );

    /**
     * Performs the core logic for {@link #getMaskedPreferredQuantitationType(ExpressionExperiment)}
     */
    protected abstract ubic.gemma.model.common.quantitationtype.QuantitationType handleGetMaskedPreferredQuantitationType(
            ExpressionExperiment expressionExperiment );

    /**
     * Performs the core logic for {@link #getPerTaxonCount()}
     */
    protected abstract Map<Taxon, Long> handleGetPerTaxonCount();

    /**
     * Performs the core logic for {@link #getPopulatedFactorCounts(Collection)}
     */
    protected abstract Map<Long, Integer> handleGetPopulatedFactorCounts( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getPopulatedFactorCountsExcludeBatch(Collection)}
     */
    protected abstract Map<Long, Integer> handleGetPopulatedFactorCountsExcludeBatch( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #getProcessedExpressionVectorCount(ExpressionExperiment)}
     */
    protected abstract Integer handleGetProcessedExpressionVectorCount( ExpressionExperiment expressionExperiment );

    /**
     * Performs the core logic for {@link #getQuantitationTypeCountById(Long)}
     */
    protected abstract Map<QuantitationType, Integer> handleGetQuantitationTypeCountById( Long Id );

    /**
     * Performs the core logic for {@link #getQuantitationTypes(ExpressionExperiment)}
     */
    protected abstract Collection<QuantitationType> handleGetQuantitationTypes(
            ExpressionExperiment expressionExperiment );

    /**
     * Performs the core logic for
     * {@link #getQuantitationTypes(ExpressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign)}
     */
    protected abstract Collection<QuantitationType> handleGetQuantitationTypes(
            ExpressionExperiment expressionExperiment, ubic.gemma.model.expression.arrayDesign.ArrayDesign arrayDesign );

    /**
     * Performs the core logic for {@link #getSampleRemovalEvents(Collection)}
     */
    protected abstract Map<ExpressionExperiment, Collection<AuditEvent>> handleGetSampleRemovalEvents(
            Collection<ExpressionExperiment> expressionExperiments );

    /**
     * Performs the core logic for
     * {@link #getSamplingOfVectors(ubic.gemma.model.common.quantitationtype.QuantitationType, Integer)}
     */
    protected abstract Collection<DesignElementDataVector> handleGetSamplingOfVectors(
            ubic.gemma.model.common.quantitationtype.QuantitationType quantitationType, Integer limit );

    /**
     * Performs the core logic for {@link #getSubSets(ExpressionExperiment)}
     */
    protected abstract Collection<ExpressionExperimentSubSet> handleGetSubSets(
            ExpressionExperiment expressionExperiment );

    /**
     * Performs the core logic for {@link #getTaxon(BioAssaySet)}
     */
    protected abstract ubic.gemma.model.genome.Taxon handleGetTaxon( BioAssaySet ee );

    /**
     * Performs the core logic for {@link #load(Collection)}
     */
    protected abstract Collection<ExpressionExperiment> handleLoad( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #loadValueObjects(Collection, boolean)}
     * 
     * @param maintainOrder If true, order of valueObjects returned will correspond to order of ids passed in.
     */
    protected abstract Collection<ExpressionExperimentValueObject> handleLoadValueObjects( Collection<Long> ids,
            boolean maintainOrder );

    /**
     * Performs the core logic for {@link #thaw(ExpressionExperiment)}
     */
    protected abstract ExpressionExperiment handleThaw( ExpressionExperiment expressionExperiment, boolean thawVectors );

    protected abstract ExpressionExperiment handleThawLiter( ExpressionExperiment expressionExperiment,
            boolean thawVectors );

}