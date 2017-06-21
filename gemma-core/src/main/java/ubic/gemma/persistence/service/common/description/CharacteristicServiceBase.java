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
package ubic.gemma.persistence.service.common.description;

import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.VoEnabledService;

/**
 * Service base class for <code>CharacteristicService</code>, provides access to all
 * services and entities referenced by this service.
 *
 * @see CharacteristicService
 */
public abstract class CharacteristicServiceBase extends VoEnabledService<Characteristic, CharacteristicValueObject>
        implements CharacteristicService {

    public CharacteristicServiceBase( CharacteristicDao dao ) {
        super( dao );
    }

    /**
     * @see CharacteristicService#findByUri(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Characteristic> findByUri( final java.lang.String searchString ) {
        return this.handleFindByUri( searchString );
    }

    /**
     * @see CharacteristicService#findByUri(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Characteristic> findByUri( final java.util.Collection<String> uris ) {
        return this.handleFindByUri( uris );
    }

    /**
     * @see CharacteristicService#findByValue(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Characteristic> findByValue( final java.lang.String search ) {
        return this.handleFindByValue( search );
    }

    /**
     * @see CharacteristicService#getParents(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Characteristic, Object> getParents(
            final java.util.Collection<Characteristic> characteristics ) {
        return this.handleGetParents( characteristics );

    }

    /**
     * Performs the core logic for {@link #findByUri(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.lang.String searchString );

    /**
     * Performs the core logic for {@link #findByUri(java.util.Collection)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.util.Collection<String> uris );

    /**
     * Performs the core logic for {@link #findByValue(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByValue( java.lang.String search );

    /**
     * Performs the core logic for {@link #getParents(java.util.Collection)}
     */
    protected abstract java.util.Map<Characteristic, Object> handleGetParents(
            java.util.Collection<Characteristic> characteristics );

}