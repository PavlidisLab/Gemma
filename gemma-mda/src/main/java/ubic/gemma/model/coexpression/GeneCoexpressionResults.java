/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.model.coexpression;

import java.util.HashMap;
import java.util.Map;

/**
 * Used to hold coexpression results
 * 
 * @author paul
 * @version $Id$
 * @see CoexpressionValueObject
 * @see ProbeLinkCoexpressionAnalyzer
 */
public class GeneCoexpressionResults {

    private Map<Long, CoexpressionValueObject> geneImplMap;
    private Map<Long, CoexpressionValueObject> predictedMap;
    private Map<Long, CoexpressionValueObject> probeAlignedMap;

    public GeneCoexpressionResults() {
        super();
        geneImplMap = new HashMap<Long, CoexpressionValueObject>();
        predictedMap = new HashMap<Long, CoexpressionValueObject>();
        probeAlignedMap = new HashMap<Long, CoexpressionValueObject>();
    }

    /**
     * @param id
     * @return
     */
    public boolean contains( Long id ) {
        return geneImplMap.containsKey( id ) || predictedMap.containsKey( id ) || probeAlignedMap.containsKey( id );
    }

    /**
     * @param id
     * @return
     */
    public CoexpressionValueObject get( Long id ) {
        if ( geneImplMap.containsKey( id ) ) return geneImplMap.get( id );
        if ( predictedMap.containsKey( id ) ) return predictedMap.get( id );
        if ( probeAlignedMap.containsKey( id ) ) return probeAlignedMap.get( id );
        return null;
    }

    /**
     * @return
     */
    public Map<Long, CoexpressionValueObject> getGeneImplMap() {
        return geneImplMap;
    }

    /**
     * @return
     */
    public Map<Long, CoexpressionValueObject> getPredictedGeneMap() {
        return predictedMap;
    }

    /**
     * @return
     */
    public Map<Long, CoexpressionValueObject> getProbeAlignedRegionMap() {
        return probeAlignedMap;
    }

    /**
     * @param cvo
     */
    public void put( CoexpressionValueObject cvo ) {
        if ( cvo.getGeneType().equalsIgnoreCase( "GeneImpl" ) ) {
            addGeneImpl( cvo );
        } else if ( cvo.getGeneType().equalsIgnoreCase( "PredictedGeneImpl" ) ) {
            addPredictedGene( cvo );
        } else if ( cvo.getGeneType().equalsIgnoreCase( "ProbeAlignedRegionImpl" ) ) {
            addProbeAlignedGene( cvo );
        }
        return;
    }

    private void addGeneImpl( CoexpressionValueObject cvo ) {
        geneImplMap.put( cvo.getGeneId(), cvo );
    }

    private void addPredictedGene( CoexpressionValueObject cvo ) {
        predictedMap.put( cvo.getGeneId(), cvo );
    }

    private void addProbeAlignedGene( CoexpressionValueObject cvo ) {
        probeAlignedMap.put( cvo.getGeneId(), cvo );
    }

}