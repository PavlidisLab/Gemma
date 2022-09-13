/*
 * The gemma project
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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.experiment.Geeq;
import ubic.gemma.model.expression.experiment.GeeqValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author paul, tesarst
 */
@Repository
public class GeeqDaoImpl extends AbstractVoEnabledDao<Geeq, GeeqValueObject> implements GeeqDao {

    @Autowired
    public GeeqDaoImpl( SessionFactory sessionFactory ) {
        super( Geeq.class, sessionFactory );
    }

    @Override
    protected GeeqValueObject doLoadValueObject( Geeq entity ) {
        return new GeeqValueObject( entity );
    }

    @Override
    public List<GeeqValueObject> loadValueObjects( Collection<Geeq> entities ) {
        //noinspection unchecked
        List<Object[]> rows = this.getSessionFactory().getCurrentSession()
                .createQuery( "select GQ from Geeq as GQ where GQ.id in (:ids) " )
                .setParameterList( "ids", EntityUtils.getIds( entities ) ).list();

        return this.createVosFromRows( rows );
    }

    @Override
    public List<GeeqValueObject> loadAllValueObjects() {
        //noinspection unchecked
        List<Object[]> rows = this.getSessionFactory().getCurrentSession().createQuery( "select GQ from Geeq as GQ" )
                .list();

        return this.createVosFromRows( rows );
    }

    private List<GeeqValueObject> createVosFromRows( List<Object[]> rows ) {
        ArrayList<GeeqValueObject> vos = new ArrayList<>( rows.size() );
        for ( Object[] row : rows ) {
            vos.add( new GeeqValueObject( row ) );
        }

        return vos;
    }

}
