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

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.bioAssay.BioAssay;

/**
 * Represents a set of BioAssays. This is not associated with any actual data, and soley represents a logical grouping
 * of "samples" that can be used for any purpose. These could be a published grouping, or a subset of samples from a
 * published study.
 */
public abstract class BioAssaySet extends Investigation {

    public abstract Collection<BioAssay> getBioAssays();

}