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
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.VoEnabledDao;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.expression.biomaterial.BioMaterial</code>.
 *
 * @see ubic.gemma.model.expression.biomaterial.BioMaterial
 */
public abstract class BioMaterialDaoBase extends VoEnabledDao<BioMaterial, BioMaterialValueObject> implements BioMaterialDao {

    public BioMaterialDaoBase( SessionFactory sessionFactory ) {
        super( BioMaterial.class, sessionFactory );
    }

    /**
     * @see BioMaterialDao#copy(BioMaterial)
     */
    @Override
    public BioMaterial copy( final BioMaterial bioMaterial ) {
        try {
            return this.handleCopy( bioMaterial );
        } catch ( Throwable th ) {
            throw new java.lang.RuntimeException(
                    "Error performing 'BioMaterialDao.copy(ubic.gemma.model.expression.biomaterial.BioMaterial bioMaterial)' --> "
                            + th, th );
        }
    }

    /**
     * Performs the core logic for {@link #copy(BioMaterial)}
     */
    protected abstract BioMaterial handleCopy( BioMaterial bioMaterial ) throws Exception;

}