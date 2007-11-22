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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @see ubic.gemma.model.common.description.Characteristic
 */
public class CharacteristicDaoImpl
    extends ubic.gemma.model.common.description.CharacteristicDaoBase
{

    /* (non-Javadoc)
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByvalue(java.lang.String)
     */
    @Override
    protected Collection handleFindByValue( String search ) throws Exception {
        final String queryString = "select distinct char from CharacteristicImpl as char where lower(char.value) like :search";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setString( "search", search.toLowerCase() );
            return queryObject.list();
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByParentClass(java.lang.Class)
     */
    @Override
    protected Map handleFindByParentClass( Class parentClass ) throws Exception {
        final String queryString =
            "select parent, char from " + parentClass.getSimpleName() + " as parent " +
                "inner join parent.characteristics as char";
//            "from " + parentClass.getSimpleName();
        
        try {
            Map charToParent = new HashMap<Characteristic, Object>();
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            for ( Object o : queryObject.list() ) {
                Object[] row = (Object[])o;
                charToParent.put( (Characteristic)row[1], row[0] );
            }
            return charToParent;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }

    /* (non-Javadoc)
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindParents(java.lang.Class, java.util.Collection)
     */
    @Override
    protected Map handleGetParents( Class parentClass, Collection characteristics ) throws Exception {
        if ( characteristics.isEmpty() )
            return new HashMap();
        
        final String queryString =
            "select parent, char from " + parentClass.getSimpleName() + " as parent " +
                "inner join parent.characteristics as char " +
                "where char in (:chars)";
        
        try {
            Map charToParent = new HashMap<Characteristic, Object>();
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "chars", characteristics );
            for ( Object o : queryObject.list() ) {
                Object[] row = (Object[])o;
                charToParent.put( (Characteristic)row[1], row[0] );
            }
            return charToParent;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
    }
    
    @Override
    protected Collection<Characteristic> handleFindByUri( String searchString ) throws Exception {
        final String queryString = "select char from VocabCharacteristicImpl as char where lower(char.valueUri) = :search";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setString( "search", searchString.toLowerCase() );
            return queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
	}
    
    @Override
    protected Collection<Characteristic> handleFindByUri( Collection uris ) throws Exception {
        final String queryString = "from VocabCharacteristicImpl where valueUri in (:uris)";

        try {
            org.hibernate.Query queryObject = super.getSession( false ).createQuery( queryString );
            queryObject.setParameterList( "uris", uris);
            return queryObject.list();

        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }
	}
	
}