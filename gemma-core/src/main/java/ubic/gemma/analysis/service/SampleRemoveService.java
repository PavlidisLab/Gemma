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
package ubic.gemma.analysis.service;

import java.util.Collection;

import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * @author paul
 * @version $Id$
 */
public interface SampleRemoveService {

    /**
     * This does not actually remove the sample; rather, it sets all values to "missing" in the processed data.
     * 
     * @param expExp
     * @param assaysToRemove
     */
    public abstract void markAsMissing( Collection<BioAssay> assaysToRemove );

    /**
     * Reverts the action of markAsMissing.
     * 
     * @param bioAssay
     */
    public abstract void unmarkAsMissing( Collection<BioAssay> bioAssays );

}