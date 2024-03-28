/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.BlacklistedValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.BlacklistedPlatform;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

/**
 *
 * @author paul
 */
@Repository
public class BlacklistedEntityDaoImpl extends AbstractVoEnabledDao<BlacklistedEntity, BlacklistedValueObject> implements BlacklistedEntityDao {

    @Autowired
    public BlacklistedEntityDaoImpl( SessionFactory sessionFactory ) {
        super( BlacklistedEntity.class, sessionFactory );
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityDao#findByAccession(java.lang.String)
     */
    @Override
    public BlacklistedEntity findByAccession( String accession ) {
        return ( BlacklistedEntity ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select b from BlacklistedEntity b join b.externalAccession e where e.accession = :accession" )
                .setParameter( "accession", accession )
                .uniqueResult();
    }

    @Override
    public boolean isBlacklisted( ArrayDesign platform ) {
        Set<String> accessions = platform.getExternalReferences().stream().map( DatabaseEntry::getAccession ).collect( Collectors.toSet() );
        Criteria criteria = this.getSessionFactory().getCurrentSession().createCriteria( BlacklistedPlatform.class );
        if ( accessions.isEmpty() ) {
            criteria.add( Restrictions.eq( "shortName", platform.getShortName() ) );
        } else {
            criteria.createAlias( "externalAccessions", "ea" )
                    .add( Restrictions.or(
                            Restrictions.eq( "shortName", platform.getShortName() ),
                            Restrictions.in( "ea.accession", accessions ) ) );
        }
        Long count = ( Long ) criteria
                .setProjection( Projections.countDistinct( "id" ) )
                .uniqueResult();
        return count > 0;
    }

    @Override
    public boolean isBlacklisted( ExpressionExperiment dataset ) {
        Long c = ( Long ) getSessionFactory().getCurrentSession().createQuery(
                        "select count(distinct be) from BlacklistedExperiment be where be.shortName = :shortName or be.externalAccession.accession = :accession" )
                .setParameter( "shortName", dataset.getShortName() )
                .setParameter( "accession", dataset.getAccession() != null ? dataset.getAccession().getAccession() : null )
                .uniqueResult();
        return c > 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityDao#isBlacklisted(java.lang.String)
     */
    @Override
    public boolean isBlacklisted( String accession ) {
        List<?> resultList = this.getSessionFactory().getCurrentSession().createQuery(
                        "select b from BlacklistedEntity b join b.externalAccession e where e.accession = :accession" )
                .setParameter( "accession", accession ).list();
        return !resultList.isEmpty();
    }

    /*
     * (non-Javadoc)
     *
     * @see ubic.gemma.persistence.service.AbstractVoEnabledDao#loadValueObject(ubic.gemma.model.common.Identifiable)
     */
    @Override
    protected BlacklistedValueObject doLoadValueObject( BlacklistedEntity entity ) {
        return BlacklistedValueObject.fromEntity( Objects.requireNonNull( this.load( entity.getId() ),
                String.format( "No BlacklistedEntity with ID %d.", entity.getId() ) ) );
    }

    @Override
    public Collection<ExpressionExperiment> getNonBlacklistedExpressionExperiments( ArrayDesign arrayDesign ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select distinct ee from ExpressionExperiment ee "
                                + "inner join ee.bioAssays bas "
                                + "inner join bas.arrayDesignUsed ad "
                                + "where ad = :ad "
                                + "and (ee.shortName not in (select be.externalAccession.accession from BlacklistedExperiment be)  "
                                + "or ee.accession.accession in (select be.shortName from BlacklistedExperiment be))" )
                .setParameter( "ad", arrayDesign )
                .list();
    }

    @Override
    public int removeAll() {
        //noinspection unchecked
        List<Long> deIds = getSessionFactory().getCurrentSession()
                .createQuery( "select ea.id from BlacklistedEntity be join be.externalAccession ea" ).list();
        int removedBe = getSessionFactory().getCurrentSession()
                .createQuery( "delete from BlacklistedEntity" )
                .executeUpdate();
        int removedDe;
        if ( !deIds.isEmpty() ) {
            removedDe = getSessionFactory().getCurrentSession()
                    .createQuery( "delete from DatabaseEntry where id in :deIds" )
                    .setParameterList( "deIds", optimizeParameterList( deIds ) )
                    .executeUpdate();
        } else {
            removedDe = 0;
        }
        log.debug( String.format( "Removed all %d BlacklistedEntity. %d DatabaseEntry were removed in cascade.", removedBe, removedDe ) );
        return removedBe;
    }
}
