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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.util.EntityUtils;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Luke
 * @see    CharacteristicService
 */
@Service
public class CharacteristicServiceImpl extends AbstractVoEnabledService<Characteristic, CharacteristicValueObject>
        implements CharacteristicService {

    /**
     * Classes examined when getting the "parents" of characteristics.
     */
    private static final Class<?>[] CLASSES_WITH_CHARACTERISTICS = new Class[] { ExpressionExperiment.class,
            BioMaterial.class, FactorValue.class, ExperimentalFactor.class, Gene2GOAssociation.class,
            PhenotypeAssociation.class };
    private final CharacteristicDao characteristicDao;

    @Autowired
    public CharacteristicServiceImpl( CharacteristicDao characteristicDao ) {
        super( characteristicDao );
        this.characteristicDao = characteristicDao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Characteristic> browse( Integer start, Integer limit ) {
        return this.characteristicDao.browse( start, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Characteristic> browse( Integer start, Integer limit, String sortField, boolean descending ) {
        return this.characteristicDao.browse( start, limit, sortField, descending );
    }

    @Override
    public Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, @Nullable Taxon taxon, int limit ) {
        return this.characteristicDao.findExperimentsByUris( uris, taxon, limit );
    }

    @Override
    public Collection<Characteristic> findByUri( Collection<String> uris ) {
        return this.characteristicDao.findByUri( uris );
    }

    @Override
    public Collection<Characteristic> findByUri( String searchString ) {
        return this.characteristicDao.findByUri( searchString );
    }

    @Override
    public Collection<Characteristic> findByValue( java.lang.String search ) {
        return this.characteristicDao.findByValue( search + '%' );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, CharacteristicDao.CharacteristicByValueUriOrValueCount> countCharacteristicValueLikeByValueUriOrValue( String search ) {
        return this.characteristicDao.countCharacteristicValueLikeByNormalizedValue( search + '%' );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, CharacteristicDao.CharacteristicByValueUriOrValueCount> countCharacteristicValueUriInByValueUriOrValue( Collection<String> search ) {
        return this.characteristicDao.countCharacteristicValueUriInByNormalizedValue( search );
    }

    @Override
    public Map<Characteristic, Object> getParents( Collection<Characteristic> characteristics ) {
        Map<Characteristic, Object> charToParent = new HashMap<>();
        Collection<Characteristic> needToSearch = new HashSet<>( characteristics );
        for ( Class<?> parentClass : CharacteristicServiceImpl.CLASSES_WITH_CHARACTERISTICS ) {
            Map<Characteristic, Object> found = this.characteristicDao.getParents( parentClass, needToSearch );
            charToParent.putAll( found );
            needToSearch.removeAll( found.keySet() );
        }
        return charToParent;
    }

    @Override
    public Map<Characteristic, Long> getParentIds( Class<?> parentClass, @Nullable Collection<Characteristic> characteristics ) {
        return this.characteristicDao.getParentIds( parentClass, characteristics );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Characteristic, Object> getParents( Collection<Class<?>> classes,
            @Nullable Collection<Characteristic> characteristics ) {
        Map<Characteristic, Object> charToParent = new HashMap<>();
        for ( Class<?> parentClass : classes ) {
            charToParent.putAll( this.characteristicDao.getParents( parentClass, characteristics ) );
        }
        return charToParent;
    }

    @Override
    public Collection<? extends Characteristic> findByCategory( String query ) {
        return this.characteristicDao.findByCategory( query );
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, String value ) throws IllegalArgumentException {
        try {
            return ObjectFilter.parseObjectFilter( CharacteristicDao.OBJECT_ALIAS, property, EntityUtils.getDeclaredFieldType( property, Characteristic.class ), operator, value );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( e );
        }
    }

    @Override
    public ObjectFilter getObjectFilter( String property, ObjectFilter.Operator operator, Collection<String> values ) throws IllegalArgumentException {
        try {
            return ObjectFilter.parseObjectFilter( CharacteristicDao.OBJECT_ALIAS, property, EntityUtils.getDeclaredFieldType( property, Characteristic.class ), operator, values );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( e );
        }
    }

    @Override
    public Sort getSort( String property, Sort.Direction direction ) throws IllegalArgumentException {
        try {
            EntityUtils.getDeclaredFieldType( property, Characteristic.class );
            return Sort.by( CharacteristicDao.OBJECT_ALIAS, property, direction );
        } catch ( NoSuchFieldException e ) {
            throw new IllegalArgumentException( e );
        }
    }
}