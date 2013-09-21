/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.analysis.expression.coexpression;

import ubic.gemma.model.analysis.MultiExperimentAnalysisImpl;

/**
 * A coexpression analysis done at the level of genes, usually for all the genes associated with a taxon
 */
public abstract class GeneCoexpressionAnalysis extends MultiExperimentAnalysisImpl {

    /**
     * Constructs new instances of {@link GeneCoexpressionAnalysis}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link GeneCoexpressionAnalysis}.
         */
        public static GeneCoexpressionAnalysis newInstance() {
            return new GeneCoexpressionAnalysisImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 7815753999347292175L;
    private Integer stringency;

    private Boolean enabled;

    private ubic.gemma.model.genome.Taxon taxon;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public GeneCoexpressionAnalysis() {
    }

    /**
     * Indicator of whether results from this analysis should be used. This should be set to 'false' while the analysis
     * is being performed (and incomplete) and then 'true' when it is finished. Only one analysis per taxon should be
     * enabled at any given time.
     */
    public Boolean getEnabled() {
        return this.enabled;
    }

    /**
     * The minimum stringency used.
     */
    public Integer getStringency() {
        return this.stringency;
    }

    /**
     * The taxon the experiments used came from. This is a denomalization but saves us from looking it up through the
     * experimentsAnalyzed.
     */
    public ubic.gemma.model.genome.Taxon getTaxon() {
        return this.taxon;
    }

    public void setEnabled( Boolean enabled ) {
        this.enabled = enabled;
    }

    public void setStringency( Integer stringency ) {
        this.stringency = stringency;
    }

    public void setTaxon( ubic.gemma.model.genome.Taxon taxon ) {
        this.taxon = taxon;
    }

}