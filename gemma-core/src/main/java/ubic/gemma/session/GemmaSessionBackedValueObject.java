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
package ubic.gemma.session;

import java.util.Collection;

/**
 * TODO Document M
 * 
 * @author ?
 * @version $Id$
 */
public interface GemmaSessionBackedValueObject {

    /**
     * @return
     */
    public Collection<Long> getMemberIds();

    /**
     * @return
     */
    public Long getId();

    /**
     * @param id
     */
    public void setId( Long id );

    /**
     * FIXME should this not override Object.equals()? HashCode?
     * 
     * @param ervo
     * @return
     */
    public boolean equals( GemmaSessionBackedValueObject ervo );

    /**
     * @return
     */
    public boolean isModified();

    /**
     * @param modified
     */
    public void setModified( boolean modified );

}
