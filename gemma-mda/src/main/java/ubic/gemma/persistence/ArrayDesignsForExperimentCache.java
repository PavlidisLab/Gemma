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
package ubic.gemma.persistence;

import java.util.HashMap;
import java.util.Map;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * Used to hold information for matching to a new experiment, during persisting.
 * 
 * @author paul
 * @version $Id$
 */
public class ArrayDesignsForExperimentCache {
    public static final String DESIGN_ELEMENT_KEY_SEPARATOR = ":::";

    private Map<String, ArrayDesign> arrayDesignCache = new HashMap<String, ArrayDesign>();

    private Map<String, CompositeSequence> designElementCache = new HashMap<String, CompositeSequence>();

    private Map<String, CompositeSequence> designElementSequenceCache = new HashMap<String, CompositeSequence>();

    public Map<String, ArrayDesign> getArrayDesignCache() {
        return arrayDesignCache;
    }

    public Map<String, CompositeSequence> getDesignElementCache() {
        return designElementCache;
    }

    public Map<String, CompositeSequence> getDesignElementSequenceCache() {
        return designElementSequenceCache;
    }
}
