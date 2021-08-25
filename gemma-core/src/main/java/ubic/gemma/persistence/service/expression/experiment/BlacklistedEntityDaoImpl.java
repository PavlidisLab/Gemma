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

import java.util.List;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.BlacklistedValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;

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
        List<?> resultList = this.getSessionFactory().getCurrentSession().createQuery(
                "select b from BlacklistedEntity b join b.externalAccession e where e.accession = :accession" )
                .setParameter( "accession", accession ).list();
        if ( resultList.isEmpty() ) return null;
        if ( resultList.size() > 1 ) throw new IllegalStateException( "More than one blacklist entry matches " + accession );
        return ( BlacklistedEntity ) resultList.get( 0 );
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
    public BlacklistedValueObject loadValueObject( BlacklistedEntity entity ) {
        return BlacklistedValueObject.fromEntity( this.load( entity.getId() ) );
    }

}
