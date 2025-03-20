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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.BusinessKey;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static ubic.gemma.persistence.util.QueryUtils.optimizeIdentifiableParameterList;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet</code>.
 * </p>
 *
 * @see ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet
 */
@Repository
public class ExpressionExperimentSubSetDaoImpl extends AbstractDao<ExpressionExperimentSubSet>
        implements ExpressionExperimentSubSetDao {

    @Autowired
    public ExpressionExperimentSubSetDaoImpl( SessionFactory sessionFactory ) {
        super( ExpressionExperimentSubSet.class, sessionFactory );
    }

    @Override
    public ExpressionExperimentSubSet find( ExpressionExperimentSubSet entity ) {
        Criteria queryObject = this.getSessionFactory().getCurrentSession()
                .createCriteria( ExpressionExperimentSubSet.class );
        BusinessKey.checkKey( entity );
        BusinessKey.createQueryObject( queryObject, entity );
        return ( ExpressionExperimentSubSet ) queryObject.uniqueResult();
    }

    @Nullable
    @Override
    public ExpressionExperimentSubSet loadWithBioAssays( Long id ) {
        ExpressionExperimentSubSet subSet = load( id );
        if ( subSet != null ) {
            Hibernate.initialize( subSet.getAccession() );
            Hibernate.initialize( subSet.getBioAssays() );
            Hibernate.initialize( subSet.getCharacteristics() );
            Hibernate.initialize( subSet.getSourceExperiment().getAccession() );
            Hibernate.initialize( subSet.getSourceExperiment().getCharacteristics() );
            Hibernate.initialize( subSet.getSourceExperiment().getPrimaryPublication() );
        }
        return subSet;
    }

    @Override
    public Collection<ExpressionExperimentSubSet> findByBioAssayIn( Collection<BioAssay> bioAssays ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select distinct eess from ExpressionExperimentSubSet eess join eess.bioAssays ba where ba in :bas" )
                .setParameterList( "bas", optimizeIdentifiableParameterList( bioAssays ) )
                .list();
    }

    @Override
    public Collection<FactorValue> getFactorValuesUsed( ExpressionExperimentSubSet entity, ExperimentalFactor factor ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct fv from ExpressionExperimentSubSet es "
                        + "join es.bioAssays ba "
                        + "join ba.sampleUsed bm "
                        + "join bm.factorValues fv "
                        + "where es=:es and fv.experimentalFactor = :ef" )
                .setParameter( "es", entity )
                .setParameter( "ef", factor )
                .list();
    }

    @Override
    public Collection<FactorValue> getFactorValuesUsed( Long subSetId, Long experimentalFactor ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct fv from ExpressionExperimentSubSet es "
                                + "join es.bioAssays ba "
                                + "join ba.sampleUsed bm "
                                + "join bm.factorValues fv "
                                + "where es.id=:es and fv.experimentalFactor.id = :ef" )
                .setParameter( "es", subSetId )
                .setParameter( "ef", experimentalFactor )
                .list();
    }

    @Override
    public void remove( ExpressionExperimentSubSet entity ) {
        Collection<FactorValue> factorValues = getFactorValueUsed( entity );
        Set<BioAssay> bioAssaysToRemove = new HashSet<>();
        Set<BioMaterial> samplesToRemove = new HashSet<>();
        // remove bioassays that are solely owned by this subset
        // this is currently the case for single-cell population subsets
        for ( BioAssay ba : entity.getBioAssays() ) {
            if ( entity.getSourceExperiment().getBioAssays().contains( ba ) ) {
                continue;
            }
            log.info( "Removing " + ba + " as it does not belong to the source experiment." );
            if ( !getBioAssayDimensions( ba ).isEmpty() ) {
                log.warn( ba + " is still attached to a BioAssayDimension, it will not be deleted." );
                continue;
            }
            if ( !getSingleCellDimensions( ba ).isEmpty() ) {
                log.warn( ba + " is still attached to a SingleCellDimension, it will not be deleted." );
                continue;
            }
            ba.getSampleUsed().getFactorValues().removeAll( factorValues );
            ba.getSampleUsed().getBioAssaysUsedIn().removeAll( entity.getBioAssays() );
            if ( ba.getSampleUsed().getBioAssaysUsedIn().isEmpty() && ba.getSampleUsed().getFactorValues().isEmpty() ) {
                samplesToRemove.add( ba.getSampleUsed() );
            } else {
                log.warn( ba.getSampleUsed() + " is still attached to a BioAssay or FactorValue, it will not be deleted." );
            }
            bioAssaysToRemove.add( ba );
        }
        super.remove( entity );
        if ( !bioAssaysToRemove.isEmpty() ) {
            log.info( "Removing " + bioAssaysToRemove.size() + " BioAssay that are owned by " + entity + " (i.e. they do not belong to the source experiment)." );
            for ( BioAssay ba : bioAssaysToRemove ) {
                getSessionFactory().getCurrentSession().delete( ba );
            }
        }
        if ( !samplesToRemove.isEmpty() ) {
            log.info( "Removing " + samplesToRemove.size() + " BioMaterial that are no longer attached to any BioAssay." );
            for ( BioMaterial bm : samplesToRemove ) {
                getSessionFactory().getCurrentSession().delete( bm );
            }
        }
    }

    private Collection<BioAssayDimension> getBioAssayDimensions( BioAssay ba ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select distinct dim from BioAssayDimension dim join dim.bioAssays ba where ba = :ba" )
                .setParameter( "ba", ba )
                .list();
    }

    private Collection<SingleCellDimension> getSingleCellDimensions( BioAssay ba ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select distinct dim from SingleCellDimension dim join dim.bioAssays ba where ba = :ba" )
                .setParameter( "ba", ba )
                .list();
    }

    /**
     * Obtain all {@link FactorValue} used by this subset.
     */
    private Collection<FactorValue> getFactorValueUsed( ExpressionExperimentSubSet subset ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct fv from ExpressionExperimentSubSet es "
                                + "join es.bioAssays ba "
                                + "join ba.sampleUsed bm "
                                + "join bm.factorValues fv "
                                + "where es=:es" )
                .setParameter( "es", subset )
                .list();
    }
}