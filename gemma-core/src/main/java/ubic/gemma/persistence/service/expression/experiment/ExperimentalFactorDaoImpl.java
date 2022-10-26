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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * @see ubic.gemma.model.expression.experiment.ExperimentalFactor
 */
@Repository
public class ExperimentalFactorDaoImpl extends AbstractVoEnabledDao<ExperimentalFactor, ExperimentalFactorValueObject>
        implements ExperimentalFactorDao {

    @Autowired
    public ExperimentalFactorDaoImpl( SessionFactory sessionFactory ) {
        super( ExperimentalFactor.class, sessionFactory );
    }

    @Override
    public ExperimentalFactor load( @Nullable Long id ) {
        if ( id == null ) {
            return null;
        }
        return ( ExperimentalFactor ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select ef from ExperimentalFactor ef left join fetch ef.factorValues fv left join fetch fv.characteristics c where ef.id=:id" )
                .setParameter( "id", id ).uniqueResult();
    }

    @Override
    @Transactional
    public void remove( ExperimentalFactor experimentalFactor ) {
        ExperimentalDesign ed = experimentalFactor.getExperimentalDesign();

        //language=HQL
        final String queryString = "select distinct ee from ExpressionExperiment as ee where ee.experimentalDesign = :ed";
        //noinspection unchecked
        List<ExpressionExperiment> results = getSessionFactory().getCurrentSession()
                .createQuery( queryString )
                .setParameter( "ed", ed )
                .list();

        if ( results.isEmpty() ) {
            log.warn( "No expression experiment for experimental design " + ed );
        }

        Session session = this.getSessionFactory().getCurrentSession();

        // remove associations with the experimental factor in related expression experiments
        for ( ExpressionExperiment ee : results ) {
            for ( BioAssay ba : ee.getBioAssays() ) {
                BioMaterial bm = ba.getSampleUsed();

                Collection<FactorValue> factorValuesToRemoveFromBioMaterial = new HashSet<>();
                for ( FactorValue factorValue : bm.getFactorValues() ) {
                    if ( experimentalFactor.equals( factorValue.getExperimentalFactor() ) ) {
                        factorValuesToRemoveFromBioMaterial.add( factorValue );
                        this.getSessionFactory().getCurrentSession().evict( factorValue.getExperimentalFactor() );
                    }
                }

                // if there are factor values to remove
                if ( factorValuesToRemoveFromBioMaterial.size() > 0 ) {
                    bm.getFactorValues().removeAll( factorValuesToRemoveFromBioMaterial );
                    session.update( bm );
                }
            }
        }

        // remove the experimental factor this cascades to values.
        super.remove( experimentalFactor );
    }

    @Override
    public ExperimentalFactor find( ExperimentalFactor experimentalFactor ) {

        BusinessKey.checkValidKey( experimentalFactor );
        Criteria queryObject = super.getSessionFactory().getCurrentSession().createCriteria( ExperimentalFactor.class );
        BusinessKey.addRestrictions( queryObject, experimentalFactor );
        return ( ExperimentalFactor ) queryObject.uniqueResult();
    }

    @Override
    public ExperimentalFactor findOrCreate( ExperimentalFactor experimentalFactor ) {
        ExperimentalFactor existing = this.find( experimentalFactor );
        if ( existing != null ) {
            assert existing.getId() != null;
            return existing;
        }
        AbstractDao.log.debug( "Creating new arrayDesign: " + experimentalFactor.getName() );
        return this.create( experimentalFactor );
    }

    @Override
    protected ExperimentalFactorValueObject doLoadValueObject( ExperimentalFactor e ) {
        return new ExperimentalFactorValueObject( e );
    }

    @Override
    public ExperimentalFactor thaw( ExperimentalFactor ef ) {
        ef = this.load( ef.getId() );
        Hibernate.initialize( ef );
        Hibernate.initialize( ef.getExperimentalDesign() );
        return ef;
    }
}