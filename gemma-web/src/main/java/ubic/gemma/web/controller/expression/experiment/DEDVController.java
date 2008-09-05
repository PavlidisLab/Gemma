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

package ubic.gemma.web.controller.expression.experiment;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * @author kelsey Exposes methods for accessing underlying Design Element Data Vectors. eg: ajax methods for
 *         visulization
 * @version $Id: DEDVController.java
 * @spring.bean id="dedvController"
 * @spring.property name = "expressionExperimentService" ref="expressionExperimentService"
 * @spring.property name = "designElementDataVectorService" ref="designElementDataVectorService"
 * @spring.property name = "geneService" ref="geneService"
 */

public class DEDVController {

    private static Log log = LogFactory.getLog( DEDVController.class );

    // ------------------------------
    // Service Members
    private DesignElementDataVectorService designElementDataVectorService;
    private GeneService geneService;
    private ExpressionExperimentService expressionExperimentService;

    // -----------------------
    // Exposed Ajax Methods

    /**
     * Given a collection of expression experiment Ids and a geneId returns a map of DEDV value objects to a collection
     * of genes. The EE info is in the value object.
     */

    public Map<Long, Collection<DoubleVectorValueObject>>  getDEDV( Collection<Long> eeIds, Collection<Long> geneIds )
            throws Exception {
        StopWatch watch = new StopWatch();
        watch.start();

        // Get and thaw the experiments.
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadMultiple( eeIds );
        if (ees == null || ees.isEmpty())
            return null;
        
        for ( ExpressionExperiment ee : ees ) {
            expressionExperimentService.thawLite( ee );
        }

        // Get and thaw gene
        Collection<Gene> genes = geneService.loadMultiple( geneIds );
        
        if (genes == null || genes.isEmpty())
            return null;
        
        // Get dedv's
        Map<DoubleVectorValueObject, Collection<Gene>> dedvMap = designElementDataVectorService.getMaskedPreferredDataArrays( ees, genes );

        
        watch.stop();
        Long time = watch.getTime();

        log.info( "Retrieved " + dedvMap.keySet().size() + " DEDVs for eeIDs: " + eeIds + " and GeneIds: " + geneIds + " in : " + time + " ms." );

        return mapInvert(dedvMap);

    }

    // Private method used for inverting the DEDV map. Having DEDV's as the key is not always useful and DWR does not
    // like non-string key values for the map. During the inverting process Gemma Gene Ids are used instead of the GEne
    // object themselves.
    private Map<Long, Collection<DoubleVectorValueObject>> mapInvert(Map<DoubleVectorValueObject, Collection<Gene>> mapToInvert)
    {   
        Map<Long, Collection<DoubleVectorValueObject>> convertedMap = new HashMap<Long, Collection<DoubleVectorValueObject>>();
        
        for(DoubleVectorValueObject dvvo : mapToInvert.keySet()){
            
            for(Gene g : mapToInvert.get( dvvo )){
                if (convertedMap.containsKey( g.getId() ))
                    convertedMap.get( g.getId() ).add( dvvo );            
                else{//If the gene is not already in the map we need to create the collection to hold the dedv.
                    Collection<DoubleVectorValueObject> dvvos = new HashSet<DoubleVectorValueObject>();
                    dvvos.add( dvvo );
                    convertedMap.put( g.getId(), dvvos );
                }                
            }
            
        }
        
        return convertedMap;
    }
    // --------------------------------
    // Dependency injection setters
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}
