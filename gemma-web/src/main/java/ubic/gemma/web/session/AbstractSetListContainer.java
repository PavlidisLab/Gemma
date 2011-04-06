/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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

package ubic.gemma.web.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;

import ubic.gemma.model.Reference;
import ubic.gemma.persistence.GemmaSessionBackedValueObject;

public abstract class AbstractSetListContainer implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -7207696842986893748L;

    static final int MAX_SIZE = 3;

    Long largestSessionId = 0l;

    ArrayList<GemmaSessionBackedValueObject> setList;


    public AbstractSetListContainer() {
        setList = new ArrayList<GemmaSessionBackedValueObject>();
    }

    public Long incrementAndGetLargestSessionId() {
        largestSessionId = largestSessionId + 1;

        // unique session bound Id for each entry in the user's session(doubt that a user will have over 100000 set session
        // entries
        // so I believe 100000 provides a large enough range to avoid conflicts
        //
        if ( largestSessionId > 100000 ) {
            largestSessionId = 0l;
        }

        return largestSessionId;
    }

    public GemmaSessionBackedValueObject addSet( GemmaSessionBackedValueObject vo ) {

        boolean setExists = false;
        
        // check if the set's reference is already in setList, 
        // if it is, replace the old list with the new one
        if(vo.getReference() != null){
            for ( int i = 0; i < setList.size(); i++ ) {

                Reference setRef = setList.get( i ).getReference();
                
                if ( setRef != null && setRef.equals( vo.getReference() ) ) {
                    setList.remove( i );
                    setList.add( i, vo );
                    setExists = true;
                    break;
                }

            }
        }

        if ( !setExists ) {

            Long newId = incrementAndGetLargestSessionId();
            vo.setReference( new Reference( newId, Reference.SESSION_BOUND_GROUP ) );

            setList.add( vo );
            if ( setList.size() > MAX_SIZE ) {
                setList.remove( 0 );
            }
        }
        return vo;

    }

    public void removeSet( GemmaSessionBackedValueObject vo ) {

        if ( vo.getReference() != null ) {

            for ( int i = 0; i < setList.size(); i++ ) {

                if ( setList.get( i ).getReference().equals( vo.getReference() ) ) {
                    setList.remove( i );
                    break;
                }

            }

        }

    }

    public void updateSet( GemmaSessionBackedValueObject vo ) {

        if ( vo.getReference() != null ) {

            for ( int i = 0; i < setList.size(); i++ ) {

                if ( setList.get( i ).getReference().equals( vo.getReference() ) ) {
                    setList.remove( i );
                    setList.add( i, vo );
                    break;
                }

            }

        }

    }

    public Collection<GemmaSessionBackedValueObject> getRecentSets() {

        return setList;

    }

}
