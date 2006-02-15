/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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

package edu.columbia.gemma.expression.designElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import edu.columbia.gemma.loader.loaderutils.BeanPropertyCompleter;

/**
 * @author pavlidis
 * @version $Id$
 */
public class CompositeSequenceDaoImpl extends edu.columbia.gemma.expression.designElement.CompositeSequenceDaoBase {

    private static Log log = LogFactory.getLog( CompositeSequenceDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.designElement.CompositeSequenceDaoBase#find(edu.columbia.gemma.expression.designElement.CompositeSequence)
     */
    @Override
    public CompositeSequence find( CompositeSequence compositeSequence ) {

        if ( compositeSequence.getName() == null ) return null;

        try {

            Criteria queryObject = super.getSession( false ).createCriteria( CompositeSequence.class );

            queryObject.add( Restrictions.eq( "name", compositeSequence.getName() ) );

            // TODO make this use the full arraydesign
            // business key.
            queryObject.createCriteria( "arrayDesign" ).add(
                    Restrictions.eq( "name", compositeSequence.getArrayDesign().getName() ) );

            java.util.List results = queryObject.list();
            Object result = null;
            if ( results != null ) {
                if ( results.size() > 1 ) {
                    throw new org.springframework.dao.InvalidDataAccessResourceUsageException(
                            "More than one instance of '" + CompositeSequence.class.getName()
                                    + "' was found when executing query" );

                } else if ( results.size() == 1 ) {
                    result = ( CompositeSequence ) results.iterator().next();
                }
            }
            return ( CompositeSequence ) result;
        } catch ( org.hibernate.HibernateException ex ) {
            throw super.convertHibernateAccessException( ex );
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.designElement.CompositeSequenceDaoBase#findOrCreate(edu.columbia.gemma.expression.designElement.CompositeSequence)
     */
    @Override
    public CompositeSequence findOrCreate( CompositeSequence compositeSequence ) {
        if ( compositeSequence.getName() == null || compositeSequence.getArrayDesign() == null ) {
            if ( log.isDebugEnabled() ) log.debug( "compositeSequence must have name and arrayDesign." );
            return null;
        }
       
        CompositeSequence newcompositeSequence = this.find( compositeSequence );
        if ( newcompositeSequence != null ) {
            if ( log.isDebugEnabled() ) log.debug( "Found existing compositeSequence: " + newcompositeSequence );
            BeanPropertyCompleter.complete( newcompositeSequence, compositeSequence );
            return newcompositeSequence;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new compositeSequence: " + compositeSequence );
        return ( CompositeSequence ) create( compositeSequence );
    }

   

}