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

package ubic.gemma.web.controller.visualization;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

public class VisualizationValueObject {

    private Collection<GeneExpressionProfile> profiles;
    private ExpressionExperimentValueObject eevo = null;
    private Map<Long, String> colorMap = new HashMap<Long, String>();

    private static String[] colors = new String[] { "black", "red", "blue", "green" };
    private static Log log = LogFactory.getLog( VisualizationValueObject.class );

    
    public VisualizationValueObject() {
        super();
        this.profiles = new HashSet<GeneExpressionProfile>();
    }

    /**
     * @param vectors
     * @param genes  Is list so that order is gauranteed.  Need this so that color's are consistent.  Query gene is always black, coexpressed is always red. 
     */
    public VisualizationValueObject( Collection<DoubleVectorValueObject> vectors, List<Gene> genes ) {
        this();

        int i = 0;
        if ( genes.size() > colors.length ) {
            // / FIXME
        }
        for ( Gene g : genes ) {
            colorMap.put( g.getId(), colors[i] );
            i++;
        }

        for ( DoubleVectorValueObject vector : vectors ) {
            if ( this.eevo == null ) {
                setEE(vector.getExpressionExperiment());
            } else if ( !( this.eevo.getId().equals( vector.getExpressionExperiment().getId() ) ) ) {
                throw new IllegalArgumentException( "All vectors have to have the same ee for this constructor. ee1: "
                        + this.eevo.getId() + "  ee2: " + vector.getExpressionExperiment().getId() );
            }

            String color = null;
            for ( Gene g : genes ) {
                if ( vector.getGenes().contains( g ) ) {
                    if (color != null){
                        log.warn("Probe: " +vector.getDesignElement().getName() + " contains both the query gene and the expressed gene");
                    }
                    color = colorMap.get( g.getId() );
                }
            }
            GeneExpressionProfile profile = new GeneExpressionProfile( vector, color );
            profiles.add( profile );

        }
    }

    /**
     * @param dvvo
     */
    public VisualizationValueObject( DoubleVectorValueObject dvvo ) {
        this();
        setEE( dvvo.getExpressionExperiment() );
        GeneExpressionProfile profile = new GeneExpressionProfile( dvvo, null );
        profiles.add( profile );
    }

    // ---------------------------------
    // Getters and Setters
    // ---------------------------------

    public ExpressionExperimentValueObject getEevo() {
        return eevo;
    }

    public void setEE( ExpressionExperiment ee ) {
        this.eevo = new ExpressionExperimentValueObject();
        this.eevo = new ExpressionExperimentValueObject();
        this.eevo.setId(  ee.getId());
        this.eevo.setName( ee.getName());
        this.eevo.setShortName( ee.getShortName() );
        this.eevo.setClazz( "ExpressionExperimentValueObject" );
    }

    public void setEevo(ExpressionExperimentValueObject eevo){
        this.eevo = eevo;
    }
    
    public Collection<GeneExpressionProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles( Collection<GeneExpressionProfile> profiles ) {
        this.profiles = profiles;
    }

}
