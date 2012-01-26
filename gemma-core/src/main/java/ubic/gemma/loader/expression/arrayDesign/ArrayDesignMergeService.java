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

/**
 * <ul>
 * <li>Make new array design based on others
 * <li>Keep map of relation between new design elements and old ones
 * <li>Store relationship with mergees
 * </ul>
 * 
 * @author paul
 * @version $Id$
 */
public interface ArrayDesignMergeService {

    /**
     * Merge array designs based on their sequence content.
     * <p>
     * Array designs that are already merged cannot be merged, but new array designs can be added into an existing
     * merged design. Also array designs can only be merged once: a given array design cannot be merged twice.
     * 
     * @param arrayDesign, used as a "top level" design when 'add' is true; otherwise just treated as one of the designs
     *        to be merged into a new design.
     * @param otherArrayDesigns array designs to merge with the arrayDesign
     * @param nameOfNewDesign can be null if "add" is true (ignored)
     * @param shortNameOfNewDesign can be null if "add" is true (ignored)
     * @param add if "arrayDesign" is already merged, add the "otherArrayDesign"s to it. Otherwise force the creation of
     *        a new design.
     * @return the merged design. If add=true, then this will be "arrayDesign". Otherwise it will be a new array design.
     */
    public abstract ArrayDesign merge( ArrayDesign arrayDesign, Collection<ArrayDesign> otherArrayDesigns,
            String nameOfNewDesign, String shortNameOfNewDesign, boolean add );

}