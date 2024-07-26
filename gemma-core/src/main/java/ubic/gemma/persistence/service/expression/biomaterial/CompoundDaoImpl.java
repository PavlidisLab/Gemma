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
package ubic.gemma.persistence.service.expression.biomaterial;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Comparator;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.biomaterial.Compound</code>.
 * </p>
 *
 * @see ubic.gemma.model.expression.biomaterial.Compound
 */
@Repository
public class CompoundDaoImpl extends AbstractDao<Compound> implements CompoundDao {

    @Autowired
    public CompoundDaoImpl( SessionFactory sessionFactory ) {
        super( Compound.class, sessionFactory );
    }

    @Override
    protected Compound findByBusinessKey( Compound compound ) {
        return this.findByProperty( "name", compound.getName() )
                .stream()
                .max( Comparator.comparing( Compound::getId ) )
                .orElse( null );
    }
}