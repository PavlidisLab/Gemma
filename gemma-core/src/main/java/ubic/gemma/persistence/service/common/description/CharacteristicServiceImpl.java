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
import ubic.gemma.model.association.Gene2GOAssociationImpl;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Luke
 * @see CharacteristicService
 */
@Service
public class CharacteristicServiceImpl extends AbstractVoEnabledService<Characteristic, CharacteristicValueObject>
        implements CharacteristicService {

    /**
     * Classes examined when getting the "parents" of characteristics.
     */
    private static final Class<?>[] CLASSES_WITH_CHARACTERISTICS = new Class[] { ExpressionExperiment.class,
            BioMaterial.class, FactorValue.class, ExperimentalFactor.class, Gene2GOAssociationImpl.class,
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
    public Collection<Characteristic> findByUri( Collection<Class<?>> classesToFilterOn, String uriString ) {
        return this.characteristicDao.findByUri( classesToFilterOn, uriString );
    }

    @Override
    public Collection<Characteristic> findByUri( Collection<Class<?>> classes, Collection<String> characteristicUris ) {
        return this.characteristicDao.findByUri( classes, characteristicUris );
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
    public Map<Characteristic, Object> getParents( Collection<Characteristic> characteristics ) {
        Map<Characteristic, Object> charToParent = new HashMap<>();
        for ( Class<?> parentClass : CharacteristicServiceImpl.CLASSES_WITH_CHARACTERISTICS ) {
            charToParent.putAll( this.characteristicDao.getParents( parentClass, characteristics ) );
        }
        return charToParent;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Characteristic, Object> getParents( Collection<Class<?>> classes,
            Collection<Characteristic> characteristics ) {
        Map<Characteristic, Object> charToParent = new HashMap<>();
        for ( Class<?> parentClass : classes ) {
            charToParent.putAll( this.characteristicDao.getParents( parentClass, characteristics ) );
        }
        return charToParent;
    }

    @Override
    public Collection<Characteristic> findByValue( Collection<Class<?>> classes, String string ) {
        return this.characteristicDao.findByValue( classes, string );
    }

    @Override
    public Collection<? extends Characteristic> findByCategory( String query ) {
        return this.characteristicDao.findByCategory( query );
    }

}