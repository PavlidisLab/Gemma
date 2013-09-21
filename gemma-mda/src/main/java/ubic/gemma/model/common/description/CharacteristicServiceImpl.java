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
package ubic.gemma.model.common.description;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.association.Gene2GOAssociationImpl;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationImpl;
import ubic.gemma.model.expression.biomaterial.BioMaterialImpl;
import ubic.gemma.model.expression.biomaterial.TreatmentImpl;
import ubic.gemma.model.expression.experiment.ExperimentalFactorImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperimentImpl;
import ubic.gemma.model.expression.experiment.FactorValueImpl;

/**
 * @see ubic.gemma.model.common.description.CharacteristicService
 * @author Luke
 * @version $Id$
 */
@Service
public class CharacteristicServiceImpl extends CharacteristicServiceBase {

    /**
     * Classes examined when getting the "parents" of characteristics.
     */
    private static final Class<?>[] CLASSES_WITH_CHARACTERISTICS = new Class[] { ExpressionExperimentImpl.class,
            BioMaterialImpl.class, FactorValueImpl.class, ExperimentalFactorImpl.class, Gene2GOAssociationImpl.class,
            PhenotypeAssociationImpl.class, TreatmentImpl.class };

    @Override
    @Transactional(readOnly = true)
    public List<Characteristic> browse( Integer start, Integer limit ) {
        return this.getCharacteristicDao().browse( start, limit );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Characteristic> browse( Integer start, Integer limit, String sortField, boolean descending ) {
        return this.getCharacteristicDao().browse( start, limit, sortField, descending );
    }

    @Override
    @Transactional(readOnly = true)
    public Integer count() {
        return this.getCharacteristicDao().count();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Characteristic, Object> getParents( Collection<Class<?>> classes,
            Collection<Characteristic> characteristics ) {
        Map<Characteristic, Object> charToParent = new HashMap<Characteristic, Object>();
        for ( Class<?> parentClass : classes ) {
            charToParent.putAll( this.getCharacteristicDao().getParents( parentClass, characteristics ) );
        }
        return charToParent;
    }

    @Override
    protected Characteristic handleCreate( Characteristic c ) {
        return this.getCharacteristicDao().create( c );
    }

    @Override
    protected void handleDelete( Characteristic c ) {
        this.getCharacteristicDao().remove( c );
    }

    @Override
    protected void handleDelete( Long id ) {
        this.getCharacteristicDao().remove( id );
    }

    @Override
    protected Collection<Characteristic> handleFindByUri( Collection<String> uris ) {
        return this.getCharacteristicDao().findByUri( uris );
    }

    @Override
    protected Collection<Characteristic> handleFindByUri( String searchString ) {
        return this.getCharacteristicDao().findByUri( searchString );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByValue(java.lang.String)
     */
    @Override
    protected java.util.Collection<Characteristic> handleFindByValue( java.lang.String search ) {
        return this.getCharacteristicDao().findByValue( search + '%' );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicServiceBase#handleGetParents(java.util.Collection)
     */
    @Override
    protected Map<Characteristic, Object> handleGetParents( Collection<Characteristic> characteristics ) {
        Map<Characteristic, Object> charToParent = new HashMap<Characteristic, Object>();
        for ( Class<?> parentClass : CLASSES_WITH_CHARACTERISTICS ) {
            charToParent.putAll( this.getCharacteristicDao().getParents( parentClass, characteristics ) );
        }
        return charToParent;
    }

    @Override
    protected Characteristic handleLoad( Long id ) {
        return this.getCharacteristicDao().load( id );
    }

    @Override
    protected void handleUpdate( Characteristic c ) {
        this.getCharacteristicDao().update( c );
    }

    @Override
    public Collection<Characteristic> findByUri( Collection<Class<?>> classesToFilterOn, String uriString ) {
        return this.getCharacteristicDao().findByUri( classesToFilterOn, uriString );
    }

    @Override
    public Collection<Characteristic> findByUri( Collection<Class<?>> classes, Collection<String> characteristicUris ) {
        return this.getCharacteristicDao().findByUri( classes, characteristicUris );
    }

    @Override
    public Collection<Characteristic> findByValue( Collection<Class<?>> classes, String string ) {
        return this.getCharacteristicDao().findByValue( classes, string );
    }

    @Override
    public Collection<? extends Characteristic> findByCategory( String query ) {
        return this.getCharacteristicDao().findByCategory( query );
    }

    @Override
    public Collection<String> getUsedCategories() {
        return this.getCharacteristicDao().getUsedCategories();
    }

}