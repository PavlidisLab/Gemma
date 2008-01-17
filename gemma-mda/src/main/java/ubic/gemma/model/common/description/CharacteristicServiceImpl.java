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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.common.description;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import ubic.gemma.model.expression.biomaterial.BioMaterialImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperimentImpl;
import ubic.gemma.model.expression.experiment.FactorValueImpl;

/**
 * @see ubic.gemma.model.common.description.CharacteristicService
 * @author Luke
 * @version $Id$
 */
public class CharacteristicServiceImpl extends ubic.gemma.model.common.description.CharacteristicServiceBase {

    private static final Class[] CLASSES_WITH_CHARACTERISTICS = new Class[] { ExpressionExperimentImpl.class,
            BioMaterialImpl.class, FactorValueImpl.class };

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByValue(java.lang.String)
     */
    @Override
    protected java.util.Collection handleFindByValue( java.lang.String search ) throws java.lang.Exception {
        return this.getCharacteristicDao().findByValue( search + '%' );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicServiceBase#handleFindByParentClass(java.lang.Object)
     */
    @Override
    protected Map handleFindByParentClass( java.lang.Class parentClass ) {
        return this.getCharacteristicDao().findByParentClass( parentClass );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicServiceBase#handleGetParent(ubic.gemma.model.common.description.Characteristic)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Object handleGetParent( Characteristic characteristic ) throws Exception {
        Collection chars = Arrays.asList( new Characteristic[] { characteristic } );
        for ( Class parentClass : CLASSES_WITH_CHARACTERISTICS ) {
            Map<Characteristic, Object> charToParent = this.getCharacteristicDao().getParents( parentClass, chars );
            if ( charToParent.containsKey( characteristic ) ) return charToParent.get( characteristic );
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicServiceBase#handleGetParents(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Map handleGetParents( Collection characteristics ) throws Exception {
        Map charToParent = new HashMap<Characteristic, Object>();
        for ( Class parentClass : CLASSES_WITH_CHARACTERISTICS ) {
            charToParent.putAll( this.getCharacteristicDao().getParents( parentClass, characteristics ) );
        }
        return charToParent;
    }

    @Override
    protected Collection handleFindByUri( String searchString ) throws Exception {
        return this.getCharacteristicDao().findByUri( searchString );
    }

    @Override
    protected Collection handleFindByUri( Collection uris ) throws Exception {
        return this.getCharacteristicDao().findByUri( uris );
    }

    @Override
    protected Characteristic handleLoad( Long id ) throws Exception {
        return ( Characteristic ) this.getCharacteristicDao().load( id );
    }

    @Override
    protected void handleDelete( Characteristic c ) throws Exception {
        this.getCharacteristicDao().remove( c );
    }

    @Override
    protected void handleUpdate( Characteristic c ) throws Exception {
        this.getCharacteristicDao().update( c );
    }

    @Override
    protected void handleDelete( Long id ) throws Exception {
        this.getCharacteristicDao().remove( id );
    }

    @Override
    protected Characteristic handleCreate( Characteristic c ) throws Exception {
        return ( Characteristic ) this.getCharacteristicDao().create( c );
    }

}