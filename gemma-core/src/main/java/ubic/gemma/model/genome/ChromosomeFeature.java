/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.genome;

import ubic.gemma.model.common.AbstractDescribable;

/**
 * Some part of a chromosome
 */
public abstract class ChromosomeFeature extends AbstractDescribable {

    private String previousNcbiId;

    private PhysicalLocation physicalLocation;

    public PhysicalLocation getPhysicalLocation() {
        return this.physicalLocation;
    }

    public void setPhysicalLocation( PhysicalLocation physicalLocation ) {
        this.physicalLocation = physicalLocation;
    }

    /**
     * @return The last-used NCBI id for this feature, according to the history information provided by NCBI. This may be empty.
     */
    public String getPreviousNcbiId() {
        return this.previousNcbiId;
    }

    public void setPreviousNcbiId( String previousNcbiId ) {
        this.previousNcbiId = previousNcbiId;
    }
}