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
package ubic.gemma.persistence.persister;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Used to hold information for matching to a new experiment, during persisting.
 *
 * @author paul
 */
public class ArrayDesignsForExperimentCache {
    private static final String DESIGN_ELEMENT_KEY_SEPARATOR = ":::";

    private final Map<String, ArrayDesign> arrayDesignCache = new HashMap<>();

    private final Map<String, CompositeSequence> designElementCache = new HashMap<>();

    private final Map<String, CompositeSequence> designElementSequenceCache = new HashMap<>();

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void add( ArrayDesign arrayDesign ) {
        this.addToDesignElementCache( arrayDesign );

        this.arrayDesignCache.put( arrayDesign.getShortName(), arrayDesign );

    }

    public void add( ArrayDesign arrayDesign, Map<CompositeSequence, BioSequence> sequences ) {
        for ( CompositeSequence cs : sequences.keySet() ) {
            this.addToCache( cs );
        }
        this.arrayDesignCache.put( arrayDesign.getShortName(), arrayDesign );
    }

    public void add( ArrayDesign arrayDesign, Set<CompositeSequence> seqs ) {
        this.addToDesignElementCache( seqs );
        this.arrayDesignCache.put( arrayDesign.getShortName(), arrayDesign );
    }

    public void addToCache( CompositeSequence cs ) {

        String key = this.makeKey( cs );

        designElementCache.put( key, cs );

        String seqName = null;
        if ( cs.getBiologicalCharacteristic() != null ) {
            seqName = cs.getBiologicalCharacteristic().getName();
        }

        if ( StringUtils.isNotBlank( seqName ) ) {
            designElementSequenceCache.put( seqName, cs );

        }
    }

    public Map<String, ArrayDesign> getArrayDesignCache() {
        return arrayDesignCache;
    }

    @Nullable
    public CompositeSequence getFromCache( CompositeSequence cs ) {

        String key = this.makeKey( cs );

        if ( designElementCache.containsKey( key ) ) {
            return designElementCache.get( key );
        }

        String seqName = null;
        if ( cs.getBiologicalCharacteristic() != null ) {
            seqName = cs.getBiologicalCharacteristic().getName();
        }

        if ( StringUtils.isNotBlank( seqName ) && designElementSequenceCache.containsKey( seqName ) ) {
            return designElementSequenceCache.get( seqName );
        }

        return null;
    }

    /**
     * Cache array design design elements (used for associating with ExpressionExperiments)
     * Note that reporters are ignored, as we are not persisting them.
     *
     * @param arrayDesign To add to the cache, must be thawed already.
     */
    private void addToDesignElementCache( final ArrayDesign arrayDesign ) {
        Collection<CompositeSequence> compositeSequences = arrayDesign.getCompositeSequences();
        this.addToDesignElementCache( compositeSequences );
    }

    private void addToDesignElementCache( Collection<CompositeSequence> seqs ) {
        for ( CompositeSequence cs : seqs ) {
            this.addToCache( cs );
        }
    }

    private String makeKey( CompositeSequence cs ) {
        ArrayDesign arrayDesign = cs.getArrayDesign();
        assert arrayDesign != null : cs + " does not have a platform";
        assert StringUtils.isNotBlank( arrayDesign.getShortName() );
        return cs.getName() + ArrayDesignsForExperimentCache.DESIGN_ELEMENT_KEY_SEPARATOR + arrayDesign.getShortName();
    }

}
