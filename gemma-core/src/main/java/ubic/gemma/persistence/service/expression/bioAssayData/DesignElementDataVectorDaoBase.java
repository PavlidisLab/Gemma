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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.SessionFactory;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>DesignElementDataVector</code>.
 * </p>
 *
 * @see DesignElementDataVector
 */
public abstract class DesignElementDataVectorDaoBase<T extends DesignElementDataVector> extends AbstractDao<T>
        implements DesignElementDataVectorDao<T> {

    DesignElementDataVectorDaoBase( Class<T> elementClass, SessionFactory sessionFactory ) {
        super( elementClass, sessionFactory );
    }

    /**
     * @see DesignElementDataVectorDao#thaw(Collection)
     */
    @Override
    public void thaw( final Collection<? extends DesignElementDataVector> designElementDataVectors ) {
        this.handleThaw( designElementDataVectors );
    }

    /**
     * @see DesignElementDataVectorDao#thaw(DesignElementDataVector)
     */
    @Override
    public void thaw( final T designElementDataVector ) {
        this.handleThaw( designElementDataVector );
    }

    /**
     * Performs the core logic for {@link #thaw(Collection)}
     */
    protected abstract void handleThaw( Collection<? extends DesignElementDataVector> designElementDataVectors );

    /**
     * Performs the core logic for {@link #thaw(DesignElementDataVector)}
     */
    protected abstract void handleThaw( T designElementDataVector );

}