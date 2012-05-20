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

import org.apache.commons.lang.StringUtils;

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

    /**
     * @param cs
     */
    public void addToCache( CompositeSequence cs ) {

        ArrayDesign arrayDesign = cs.getArrayDesign();
        assert arrayDesign != null : cs + " does not have an array design";
        String key = cs.getName() + ArrayDesignsForExperimentCache.DESIGN_ELEMENT_KEY_SEPARATOR + arrayDesign.getName();
        String seqName = null;

        if ( cs.getBiologicalCharacteristic() != null ) {
            seqName = cs.getBiologicalCharacteristic().getName();
        }

        designElementCache.put( key, cs );
        if ( StringUtils.isNotBlank( seqName ) ) {
            designElementSequenceCache.put( seqName, cs );

        }
    }

    /**
     * @param cs
     * @return
     */
    public CompositeSequence getFromCache( CompositeSequence cs ) {
        ArrayDesign arrayDesign = cs.getArrayDesign();
        assert arrayDesign != null : cs + " does not have an array design";
        String key = cs.getName() + ArrayDesignsForExperimentCache.DESIGN_ELEMENT_KEY_SEPARATOR + arrayDesign.getName();

        String seqName = null;

        if ( cs.getBiologicalCharacteristic() != null ) {
            seqName = cs.getBiologicalCharacteristic().getName();
        }

        if ( designElementCache.containsKey( key ) ) {
            return designElementCache.get( key );
        } else if ( StringUtils.isNotBlank( seqName ) && designElementSequenceCache.containsKey( seqName ) ) {
            /*
             * Because the names of design elements can change, we should try to go by the _sequence_.
             */
            return designElementSequenceCache.get( seqName );
        }
        return null;
    }

}
