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
package ubic.gemma.persistence.service.common.quantitationtype;

import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.persistence.service.VoEnabledDao;
import ubic.gemma.persistence.util.BusinessKey;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * {@link QuantitationType}.
 * </p>
 *
 * @see ubic.gemma.model.common.quantitationtype.QuantitationType
 */
@Repository
public class QuantitationTypeDaoImpl extends VoEnabledDao<QuantitationType, QuantitationTypeValueObject>
        implements QuantitationTypeDao {

    @Autowired
    public QuantitationTypeDaoImpl( SessionFactory sessionFactory ) {
        super( QuantitationType.class, sessionFactory );
    }

    @Override
    public QuantitationType find( QuantitationType quantitationType ) {
        Criteria queryObject = this.getSession().createCriteria( QuantitationType.class );
        BusinessKey.addRestrictions( queryObject, quantitationType );
        return ( QuantitationType ) queryObject.uniqueResult();
    }

    @Override
    public List<QuantitationType> loadByDescription( String description ) {
        return this.findByStringProperty( "description", description );
    }

    @Override
    public QuantitationTypeValueObject loadValueObject( QuantitationType entity ) {
        return new QuantitationTypeValueObject( entity );
    }

    @Override
    public Collection<QuantitationTypeValueObject> loadValueObjects( Collection<QuantitationType> entities ) {
        Collection<QuantitationTypeValueObject> vos = new LinkedHashSet<>();
        for ( QuantitationType e : entities ) {
            vos.add( this.loadValueObject( e ) );
        }
        return vos;
    }
}