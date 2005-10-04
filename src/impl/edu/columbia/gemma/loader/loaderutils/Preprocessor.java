/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.loaderutils;

import java.io.IOException;
import java.util.List;

import edu.columbia.gemma.expression.bioAssay.BioAssay;
import edu.columbia.gemma.loader.expression.mage.BioAssayDimensions;

/**
 * Put data in a form that can be used in analysis.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public interface Preprocessor {

    /**
     * Creates a matrix of designElements (Y) vs. bioAssays (X) for each quantitation type, for the given bioAssays
     * 
     * @param bioAssays
     */
    public void preprocess( List<BioAssay> bioAssays, BioAssayDimensions dimensions ) throws IOException;

}
