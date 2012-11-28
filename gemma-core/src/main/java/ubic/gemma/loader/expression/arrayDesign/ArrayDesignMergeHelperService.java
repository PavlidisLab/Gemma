/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.loader.expression.arrayDesign;

import java.util.Collection;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * @author Paul
 * @version $Id$
 */
public interface ArrayDesignMergeHelperService {

    /**
     * Finalize the assembly and persistence of the merged array design.
     * 
     * @param result the final merged design
     * @param arrayDesign
     * @param otherArrayDesigns
     * @param mergeWithExisting don't make a new array design, merge it into the one given as the first argument
     * @param newProbes Probes that have to be added to make up the merged design. In the case of "mergeWithExisting",
     *        this might even be empty.
     * @return the final persistent merged design
     */
    public ArrayDesign persistMerging( ArrayDesign result, ArrayDesign arrayDesign,
            Collection<ArrayDesign> otherArrayDesigns, boolean mergeWithExisting,
            Collection<CompositeSequence> newProbes );
}
