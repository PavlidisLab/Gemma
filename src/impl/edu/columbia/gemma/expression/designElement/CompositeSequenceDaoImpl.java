/*
 * The Gemma project.
 * 
 * Copyright (c) 2005 Columbia University
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

/**
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class CompositeSequenceDaoImpl extends edu.columbia.gemma.expression.designElement.CompositeSequenceDaoBase {

    private static Log log = LogFactory.getLog( CompositeSequenceDaoImpl.class.getName() );

    /*
     * (non-Javadoc)
     * 
     * @see edu.columbia.gemma.expression.designElement.CompositeSequenceDaoBase#findOrCreate(edu.columbia.gemma.expression.designElement.CompositeSequence)
     */
    @Override
    public CompositeSequence findOrCreate( CompositeSequence compositeSequence ) {
        if ( compositeSequence.getName() == null || compositeSequence.getArrayDesign() == null ) {
            log.debug( "compositeSequence must name and arrayDesign." );
            return null;
        }
        CompositeSequence newcompositeSequence = ( CompositeSequence ) this.find( compositeSequence );
        if ( newcompositeSequence != null ) {
            return newcompositeSequence;
        }
        if ( log.isDebugEnabled() ) log.debug( "Creating new compositeSequence: " + compositeSequence.getName() );
        return ( CompositeSequence ) create( compositeSequence );
    }

}