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
package ubic.gemma.annotation.geommtx;

import java.util.Collection;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author paul
 * @version $Id$
 */
public interface ExpressionExperimentAnnotator {

    public static final String MMTX_ACTIVATION_PROPERTY_KEY = "mmtxOn";

    /*
     */
    public abstract Collection<Characteristic> annotate( ExpressionExperiment e, boolean force );

    /**
     * Force initialization. No need to call this if you have configured this via the MMTX_ACTIVATION_PROPERTY_KEY
     */
    public abstract void init();
}