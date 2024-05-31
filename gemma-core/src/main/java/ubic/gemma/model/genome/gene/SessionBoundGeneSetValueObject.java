/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.model.genome.gene;

import ubic.gemma.model.common.GemmaSessionBackedValueObject;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author tvrossum
 */
public class SessionBoundGeneSetValueObject extends GeneSetValueObject implements GemmaSessionBackedValueObject {

    private static final long serialVersionUID = 5073203626044664184L;

    /**
     * Counter to assign unique IDs to session-bounds gene sets.
     */
    private static final AtomicLong counter = new AtomicLong( 1L );

    private boolean modified = false;

    /**
     * default constructor to satisfy java bean contract
     */
    @SuppressWarnings("WeakerAccess") // Frontend use
    public SessionBoundGeneSetValueObject() {
        super( counter.getAndIncrement() );
        this.setModified( false );
    }

    /**
     * @return the modified
     */
    @Override
    public boolean isModified() {
        return modified;
    }

    /**
     * @param modified the modified to set
     */
    @Override
    public void setModified( boolean modified ) {
        this.modified = modified;
    }

}
