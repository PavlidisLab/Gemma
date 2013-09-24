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
package ubic.gemma.model.association.coexpression;

import java.util.Collection;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.CoexpressionCacheValueObject;
import ubic.gemma.model.genome.Gene;

/**
 * @author Paul
 * @version $Id$
 */
public interface Probe2ProbeCoexpressionCache {

    /**
     * @param coExVOForCache
     */
    public abstract void addToCache( CoexpressionCacheValueObject coExVOForCache );

    /**
     * 
     */
    public abstract void clearCache();

    /**
     * Remove all elements from the cache for the given expression experiment, if the cache exists.
     * 
     * @param e the expression experiment - specific cache to be cleared.
     */
    public abstract void clearCache( Long eeid );

    /**
     * @param ee
     * @param g
     * @return
     */
    public abstract Collection<CoexpressionCacheValueObject> get( BioAssaySet ee, Gene g );

    /**
     * @return the enabled
     */
    public abstract Boolean isEnabled();

    /**
     * @param enabled the enabled to set
     */
    public abstract void setEnabled( Boolean enabled );

}