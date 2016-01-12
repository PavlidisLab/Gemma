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

package ubic.gemma.web.persistence;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import ubic.gemma.session.GemmaSessionBackedValueObject;

/**
 * TODO Document Me
 * 
 * @author thea
 * @version $Id$
 */
public abstract class AbstractSetListContainer implements Serializable {

    private static final int MAX_MODIFIED_GROUPS = 3;

    private static final int MAX_TOTAL = 1000;

    private static final long serialVersionUID = -7207696842986893748L;

    private List<GemmaSessionBackedValueObject> allSessionBoundGroups;

    private Long largestSessionId = 0l;

    private List<GemmaSessionBackedValueObject> sessionBoundModifiedGroups;

    public AbstractSetListContainer() {
        allSessionBoundGroups = new ArrayList<GemmaSessionBackedValueObject>();
        sessionBoundModifiedGroups = new ArrayList<GemmaSessionBackedValueObject>();
    }

    /**
     * Sets the reference (generates an id and assumes this group was made as a result of a modification for the type
     * value) for the group then adds it to the session-bound list(s) for session-bound groups
     * 
     * @param vo
     * @return
     */
    public GemmaSessionBackedValueObject addSet( GemmaSessionBackedValueObject vo, boolean modified ) {

        // check if the set's reference is already in setList,
        // if it is, replace the old list with the new one
        if ( vo.getId() != null ) {
            for ( int i = 0; i < allSessionBoundGroups.size(); i++ ) {

                if ( allSessionBoundGroups.get( i ).equals( vo ) ) {
                    allSessionBoundGroups.remove( i );
                    allSessionBoundGroups.add( i, vo );
                    return vo;
                }

            }
        }

        Long newId = incrementAndGetLargestSessionId();
        vo.setModified( modified );
        vo.setId( newId );

        // add it to the special list of groups the user has modified
        if ( modified ) {
            sessionBoundModifiedGroups.add( vo );
            if ( sessionBoundModifiedGroups.size() > MAX_MODIFIED_GROUPS ) {
                sessionBoundModifiedGroups.remove( 0 );
            }
        }
        allSessionBoundGroups.add( vo );
        if ( allSessionBoundGroups.size() > MAX_TOTAL ) {
            allSessionBoundGroups.remove( 0 );
        }

        return vo;
    }

    public List<? extends GemmaSessionBackedValueObject> getAllSessionBoundGroups() {
        return allSessionBoundGroups;
    }

    public List<? extends GemmaSessionBackedValueObject> getSessionBoundModifiedGroups() {
        return sessionBoundModifiedGroups;
    }

    public Long incrementAndGetLargestSessionId() {
        largestSessionId = largestSessionId + 1;

        // unique session bound Id for each entry in the user's session(doubt that a user will have over 100000 set
        // session
        // entries
        // so I believe 100000 provides a large enough range to avoid conflicts
        //
        if ( largestSessionId > 100000 ) {
            largestSessionId = 0l;
        }

        return largestSessionId;
    }

    /**
     * @param vo
     */
    public void removeSet( GemmaSessionBackedValueObject vo ) {

        if ( vo != null ) {

            for ( int i = 0; i < allSessionBoundGroups.size(); i++ ) {

                if ( allSessionBoundGroups.get( i ).equals( vo ) ) {
                    allSessionBoundGroups.remove( i );
                    break;
                }
            }
        }
        if ( vo != null ) {

            for ( int i = 0; i < sessionBoundModifiedGroups.size(); i++ ) {

                if ( sessionBoundModifiedGroups.get( i ).equals( vo ) ) {
                    sessionBoundModifiedGroups.remove( i );
                    break;
                }
            }
        }
    }

    /**
     * @param vo
     */
    public void updateSet( GemmaSessionBackedValueObject vo ) {

        if ( vo != null ) {

            for ( int i = 0; i < allSessionBoundGroups.size(); i++ ) {

                if ( allSessionBoundGroups.get( i ).equals( vo ) ) {
                    allSessionBoundGroups.remove( i );
                    allSessionBoundGroups.add( i, vo );
                    break;
                }
            }
        }
        if ( vo != null ) {

            for ( int i = 0; i < sessionBoundModifiedGroups.size(); i++ ) {

                if ( sessionBoundModifiedGroups.get( i ).equals( vo ) ) {
                    sessionBoundModifiedGroups.remove( i );
                    sessionBoundModifiedGroups.add( i, vo );
                    break;
                }
            }
        }
    }

}
