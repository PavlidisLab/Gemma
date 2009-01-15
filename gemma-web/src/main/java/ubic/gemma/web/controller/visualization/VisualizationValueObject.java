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

/**
 * Stores expression profile data from one expression experiment for plotting.
 * 
 * @author kelsey, paul
 * @version $Id$
 */
public class VisualizationValueObject {

    private static String[] colors = new String[] { "red", "black", "blue", "green", "orange" };
    private static Log log = LogFactory.getLog( VisualizationValueObject.class );
    private Map<Long, String> colorMap = new HashMap<Long, String>();

    private ExpressionExperimentValueObject eevo = null;
    private Collection<GeneExpressionProfile> profiles;

    public VisualizationValueObject() {
        super();
        this.profiles = new HashSet<GeneExpressionProfile>();
    }

    /**
     * @param vectors from a single expression experiment.
     * @param genes Is list so that order is gauranteed. Need this so that color's are consistent. Query gene is always
     *        black, coexpressed is always red.
     * @throws IllegalArgumentException if vectors are mixed between EEs.
     */
    public VisualizationValueObject( Collection<DoubleVectorValueObject> vectors, List<Gene> genes ) {
        this( vectors, genes, null, null );
    }

    /**
     * @param Vectors to be plotted (should come from a single expression experiment)
     * @param genes Is list so that order is guaranteed. Need this so that colors are consistent.
     * @param validatedProbeList Probes which are flagged as 'valid' in some sense. For example, in coexpression plots
     *        these are probes that provided the coexpression evidence, to differentiate them from the ones which are
     *        just being displayed because they assay the same gene.
     * @throws IllegalArgumentException if vectors are mixed between EEs.
     */
    public VisualizationValueObject( Collection<DoubleVectorValueObject> vectors, List<Gene> genes,
            Collection<Long> validatedProbeList ) {
        this( vectors, genes, validatedProbeList, null );
    }

    /**
     * @param vectors
     * @param genes
     * @param validatedProbeList
     * @param minPvalue
     */
    public VisualizationValueObject( Collection<DoubleVectorValueObject> vectors, List<Gene> genes,
            Collection<Long> validatedProbeList, Double minPvalue ) {
        this();

        populateColorMap( genes );

        for ( DoubleVectorValueObject vector : vectors ) {
            if ( this.eevo == null ) {
                setEEwithPvalue( vector.getExpressionExperiment(), minPvalue );
            } else if ( !( this.eevo.getId().equals( vector.getExpressionExperiment().getId() ) ) ) {
                throw new IllegalArgumentException( "All vectors have to have the same ee for this constructor. ee1: "
                        + this.eevo.getId() + "  ee2: " + vector.getExpressionExperiment().getId() );
            }

            String color = null;
            for ( Gene g : genes ) {
                if ( !vector.getGenes().contains( g ) ) {
                    continue;
                }
                color = colorMap.get( g.getId() );
            }

            int valid = 1;
            if ( validatedProbeList != null && validatedProbeList.contains( vector.getDesignElement().getId() ) ) {
                valid = 2;
            }

            GeneExpressionProfile profile = new GeneExpressionProfile( vector, color, valid );

            // If points is empty dont add
            if ( profile.getPoints() != null ) profiles.add( profile );

        }
    }

    /**
     * @param dvvo
     */
    public VisualizationValueObject( DoubleVectorValueObject dvvo ) {
        this();
        setEE( dvvo.getExpressionExperiment() );
        GeneExpressionProfile profile = new GeneExpressionProfile( dvvo, null, 0 );
        profiles.add( profile );
    }

    public ExpressionExperimentValueObject getEevo() {
        return eevo;
    }

    // ---------------------------------
    // Getters and Setters
    // ---------------------------------

    public Collection<GeneExpressionProfile> getProfiles() {
        return profiles;
    }

    public void setEE( ExpressionExperiment ee ) {
        this.eevo = new ExpressionExperimentValueObject();
        this.eevo.setId( ee.getId() );
        this.eevo.setName( ee.getName() );
        this.eevo.setShortName( ee.getShortName() );
        this.eevo.setClazz( "ExpressionExperimentValueObject" );
    }

    public void setEevo( ExpressionExperimentValueObject eevo ) {
        this.eevo = eevo;
    }

    public void setEEwithPvalue( ExpressionExperiment ee, Double minP ) {
        setEE( ee );
        this.eevo.setMinPvalue( minP );
    }

    public void setProfiles( Collection<GeneExpressionProfile> profiles ) {
        this.profiles = profiles;
    }

    private void populateColorMap( List<Gene> genes ) {
        int i = 0;
        if ( genes.size() > colors.length ) {
            // / FIXME
        }
        for ( Gene g : genes ) {
            colorMap.put( g.getId(), colors[i] );
            i++;
        }
    }

}
