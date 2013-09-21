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
package ubic.gemma.model.expression.experiment;

import java.util.Collection;

import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * A subset of samples from an ExpressionExperiment
 */
public abstract class ExpressionExperimentSubSet extends BioAssaySet {

    /**
     * Constructs new instances of {@link ExpressionExperimentSubSet}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ExpressionExperimentSubSet}.
         */
        public static ExpressionExperimentSubSet newInstance() {
            return new ExpressionExperimentSubSetImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -1880425342951467283L;
    private ExpressionExperiment sourceExperiment;

    private Collection<BioAssay> bioAssays = new java.util.HashSet<>();

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ExpressionExperimentSubSet() {
    }

    /**
     * 
     */
    @Override
    public Collection<BioAssay> getBioAssays() {
        return this.bioAssays;
    }

    /**
     * 
     */
    public ExpressionExperiment getSourceExperiment() {
        return this.sourceExperiment;
    }

    public void setBioAssays( Collection<BioAssay> bioAssays ) {
        this.bioAssays = bioAssays;
    }

    public void setSourceExperiment( ExpressionExperiment sourceExperiment ) {
        this.sourceExperiment = sourceExperiment;
    }

}