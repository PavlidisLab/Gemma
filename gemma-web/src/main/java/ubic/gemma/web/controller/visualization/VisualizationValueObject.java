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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDetailsValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.persistence.util.IdentifiableUtils;

import java.util.*;

/**
 * Stores expression profile data from one expression experiment for plotting.
 *
 * @author kelsey, paul
 */
public class VisualizationValueObject {

    private static final String[] colors = new String[] { "red", "black", "blue", "green", "orange" };

    private static final Log log = LogFactory.getLog( VisualizationValueObject.class );

    private final Map<Long, String> colorMap = new HashMap<>();

    private ExpressionExperimentDetailsValueObject eevo = null;

    private LinkedHashMap<String, LinkedHashMap<String, String>> factorNames; // map of factor name to value-colour map

    private Collection<FactorProfile> factorProfiles;

    private List<LinkedHashMap<String, String[]>> factorValueMaps; // list of factor name to value-colour maps (for
    // colouring column headers)

    /**
     * used for displaying factor info in heatmap
     */
    private List<List<String>> factorValues;

    private Collection<GeneExpressionProfile> profiles;

    private List<String> sampleNames;

    public VisualizationValueObject() {
        super();
        this.profiles = new HashSet<>();
    }

    public VisualizationValueObject( Collection<DoubleVectorValueObject> vectors, Collection<GeneValueObject> genes,
            Double minPvalue, Collection<DifferentialExpressionValueObject> validatedProbes ) {
        this();

        Map<Long, GeneValueObject> idMap = IdentifiableUtils.getIdMap( genes );
        this.populateColorMap( new ArrayList<>( idMap.keySet() ) );

        Collection<Long> validatedProbeIdList = new ArrayList<>();
        if ( validatedProbes != null && !validatedProbes.isEmpty() ) {
            for ( DifferentialExpressionValueObject devo : validatedProbes ) {
                validatedProbeIdList.add( devo.getProbeId() );
            }
        }

        for ( DoubleVectorValueObject vector : vectors ) {
            if ( this.eevo == null ) {
                this.setEEwithPvalue( new ExpressionExperimentDetailsValueObject( vector.getExpressionExperiment() ),
                        minPvalue );
            } else if ( !( this.eevo.getId().equals( vector.getExpressionExperiment().getId() ) ) ) {
                throw new IllegalArgumentException(
                        "All vectors have to have the same ee for this constructor. ee1: " + this.eevo.getId()
                                + "  ee2: " + vector.getExpressionExperiment().getId() );
            }

            String color = null;
            Collection<Long> vectorGeneids = vector.getGenes();
            Collection<GeneValueObject> vectorGenes = new HashSet<>();
            for ( Long g : idMap.keySet() ) {
                if ( !vectorGeneids.contains( g ) ) {
                    continue;
                }
                vectorGenes.add( idMap.get( g ) );
                color = colorMap.get( g );
            }

            int valid = 1;
            Double pValue = vector.getPvalue();

            if ( validatedProbes != null ) {
                for ( DifferentialExpressionValueObject devo : validatedProbes ) {
                    if ( devo.getProbeId().equals( vector.getDesignElement().getId() ) ) {
                        // pValue = devo.getP();
                        valid = 2;
                        break;
                    }
                }
            }
            GeneExpressionProfile profile = new GeneExpressionProfile( vector, vectorGenes, color, valid, pValue );

            if ( !profile.isAllMissing() )
                profiles.add( profile );

        }
    }

    /**
     * @param vectors            to be plotted (should come from a single expression experiment)
     * @param genes              Is list so that order is guaranteed. Need this so that colors are consistent.
     * @param validatedProbeList Probes which are flagged as 'valid' in some sense. For example, in coexpression plots
     *                           these are probes that provided the coexpression evidence, to differentiate them from the ones which are
     *                           just being displayed because they assay the same gene.
     * @throws IllegalArgumentException if vectors are mixed between EEs.
     */
    public VisualizationValueObject( Collection<DoubleVectorValueObject> vectors, List<GeneValueObject> genes,
            Collection<Long> validatedProbeList ) {
        this( vectors, genes, validatedProbeList, null );
    }

    public VisualizationValueObject( Collection<DoubleVectorValueObject> vectors, List<GeneValueObject> genes,
            Collection<Long> validatedProbeIdList, Double minPvalue ) {
        this();

        Map<Long, GeneValueObject> idMap = IdentifiableUtils.getIdMap( genes );
        this.populateColorMap( new ArrayList<>( idMap.keySet() ) );

        for ( DoubleVectorValueObject vector : vectors ) {
            if ( this.eevo == null ) {
                this.setEEwithPvalue( new ExpressionExperimentDetailsValueObject( vector.getExpressionExperiment() ),
                        minPvalue );
            } else if ( !( this.eevo.getId().equals( vector.getExpressionExperiment().getId() ) ) ) {
                throw new IllegalArgumentException(
                        "All vectors have to have the same ee for this constructor. ee1: " + this.eevo.getId()
                                + "  ee2: " + vector.getExpressionExperiment().getId() );
            }

            Collection<Long> vectorGeneids = vector.getGenes();
            Collection<GeneValueObject> vectorGenes = new HashSet<>();

            String color = "black";
            if ( genes != null && vectorGeneids != null ) {
                for ( GeneValueObject g : genes ) {
                    // This seems inefficient. We should just pass in the genes for this vector.
                    if ( !vectorGeneids.contains( g.getId() ) ) {
                        continue;
                    }
                    vectorGenes.add( g );
                    color = colorMap.get( g.getId() );
                }
            }

            int valid = 2; // default. // FIXME Actually, this might not work.
            if ( validatedProbeIdList != null && !validatedProbeIdList.contains( vector.getDesignElement().getId() ) ) {
                valid = 1;
            }

            GeneExpressionProfile profile = new GeneExpressionProfile( vector, vectorGenes, color, valid,
                    vector.getPvalue() );

            //  if ( !profile.isAllMissing() ) // this might not be a desirable side-effect.
            profiles.add( profile );

        }
    }

    public VisualizationValueObject( DoubleVectorValueObject dvvo ) {
        this();
        this.setEevo( new ExpressionExperimentDetailsValueObject( dvvo.getExpressionExperiment() ) );
        GeneExpressionProfile profile = new GeneExpressionProfile( dvvo );
        profiles.add( profile );
    }

    @Override
    public String toString() {
        final int maxLen = 5;
        return "VisualizationValueObject [" + ( eevo != null ? "eevo=" + eevo + "\n " : "" ) + (
                factorProfiles != null ?
                        factorProfiles.size() + " factorProfiles=\n" + this.toString( factorProfiles, 20 ) :
                        "" ) + ( profiles != null ?
                profiles.size() + " exp. profiles (show up to " + maxLen + "):\n" + this.toString( profiles, maxLen ) :
                "" ) + "]";
    }

    /**
     * Initialize the factor profiles.
     */
    public void setUpFactorProfiles(
            LinkedHashMap<BioAssayValueObject, LinkedHashMap<ExperimentalFactor, Double>> layout ) {
        if ( layout == null ) {
            VisualizationValueObject.log.warn( "Null layout, ignoring" );
            return;
        }

        Collection<ExperimentalFactor> efs = new HashSet<>();
        for ( LinkedHashMap<ExperimentalFactor, Double> maps : layout.values() ) {
            efs.addAll( maps.keySet() );
        }

        this.factorProfiles = new ArrayList<>();
        for ( ExperimentalFactor experimentalFactor : efs ) {
            this.factorProfiles.add( new FactorProfile( experimentalFactor, layout ) );
        }
    }

    public ExpressionExperimentValueObject getEevo() {
        return eevo;
    }

    public void setEevo( ExpressionExperimentDetailsValueObject eevo ) {
        this.eevo = eevo;
    }

    public LinkedHashMap<String, LinkedHashMap<String, String>> getFactorNames() {
        return factorNames;
    }

    public void setFactorNames( LinkedHashMap<String, LinkedHashMap<String, String>> factorNames2 ) {
        this.factorNames = factorNames2;
    }

    public Collection<FactorProfile> getFactorProfiles() {
        return factorProfiles;
    }

    public void setFactorProfiles( Collection<FactorProfile> factorProfiles ) {
        this.factorProfiles = factorProfiles;
    }

    public List<List<String>> getFactorValues() {
        return factorValues;
    }

    public void setFactorValues( List<List<String>> factorValues ) {
        this.factorValues = factorValues;
    }

    public List<LinkedHashMap<String, String[]>> getFactorValuesToNames() {
        return factorValueMaps;
    }

    public void setFactorValuesToNames( List<LinkedHashMap<String, String[]>> factorValueMaps2 ) {
        this.factorValueMaps = factorValueMaps2;
    }

    public Collection<GeneExpressionProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles( Collection<GeneExpressionProfile> profiles ) {
        this.profiles = profiles;
    }

    public List<String> getSampleNames() {
        return sampleNames;
    }

    public void setSampleNames( List<String> sampleNames ) {
        this.sampleNames = sampleNames;
    }

    public void setEEwithPvalue( ExpressionExperimentDetailsValueObject ee, Double minP ) {
        this.setEevo( ee );
        this.eevo.setMinPvalue( minP );
    }

    private void populateColorMap( List<Long> genes ) {
        int i = 0;
        //if ( genes.size() > colors.length ) {
        // / FIXME -- we just cycle through for now.
        //}
        for ( Long g : genes ) {
            colorMap.put( g, VisualizationValueObject.colors[i % VisualizationValueObject.colors.length] );
            i++;
        }
    }

    private String toString( Collection<?> collection, int maxLen ) {
        StringBuilder builder = new StringBuilder();
        builder.append( "[" );
        int i = 0;
        for ( Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++ ) {
            if ( i > 0 )
                builder.append( ", " );
            builder.append( iterator.next() );
        }
        builder.append( "]\n" );
        return builder.toString();
    }

}
