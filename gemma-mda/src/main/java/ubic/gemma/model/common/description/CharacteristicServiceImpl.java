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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import ubic.gemma.model.association.Gene2GOAssociationImpl;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationImpl;
import ubic.gemma.model.expression.biomaterial.BioMaterialImpl;
import ubic.gemma.model.expression.experiment.ExperimentalFactorImpl;
import ubic.gemma.model.expression.experiment.ExpressionExperimentImpl;
import ubic.gemma.model.expression.experiment.FactorValueImpl;

/**
 * @see ubic.gemma.model.common.description.CharacteristicService
 * @author Luke
 * @version $Id$
 */
@Service
public class CharacteristicServiceImpl extends ubic.gemma.model.common.description.CharacteristicServiceBase {

    /**
     * Classes examined when getting the "parents" of characteristics.
     */
    private static final Class[] CLASSES_WITH_CHARACTERISTICS = new Class[] { ExpressionExperimentImpl.class,
            BioMaterialImpl.class, FactorValueImpl.class, ExperimentalFactorImpl.class, Gene2GOAssociationImpl.class, PhenotypeAssociationImpl.class };

    @Override
    protected Characteristic handleCreate( Characteristic c ) throws Exception {
        return this.getCharacteristicDao().create( c );
    }

    @Override
    protected void handleDelete( Characteristic c ) throws Exception {
        this.getCharacteristicDao().remove( c );
    }

    @Override
    protected void handleDelete( Long id ) throws Exception {
        this.getCharacteristicDao().remove( id );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicServiceBase#handleFindByParentClass(java.lang.Object)
     */
    @Override
    protected Map handleFindByParentClass( java.lang.Class parentClass ) {
        return this.getCharacteristicDao().findByParentClass( parentClass );
    }

    @Override
    protected Collection handleFindByUri( Collection uris ) throws Exception {
        return this.getCharacteristicDao().findByUri( uris );
    }

    @Override
    protected Collection handleFindByUri( String searchString ) throws Exception {
        return this.getCharacteristicDao().findByUri( searchString );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByValue(java.lang.String)
     */
    @Override
    protected java.util.Collection handleFindByValue( java.lang.String search ) throws java.lang.Exception {
        return this.getCharacteristicDao().findByValue( search + '%' );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.common.description.CharacteristicServiceBase#handleGetParent(ubic.gemma.model.common.description
     * .Characteristic)
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
    protected Characteristic handleLoad( Long id ) throws Exception {
        return this.getCharacteristicDao().load( id );
    }

    @Override
    protected void handleUpdate( Characteristic c ) throws Exception {
        this.getCharacteristicDao().update( c );
    }

    @Override
    public List<Characteristic> browse( Integer start, Integer limit ) {
        return this.getCharacteristicDao().browse( start, limit );
    }

    @Override
    public List<Characteristic> browse( Integer start, Integer limit, String sortField, boolean descending ) {
        return this.getCharacteristicDao().browse( start, limit, sortField, descending );
    }

    @Override
    public Integer count() {
        return this.getCharacteristicDao().count();
    }

}