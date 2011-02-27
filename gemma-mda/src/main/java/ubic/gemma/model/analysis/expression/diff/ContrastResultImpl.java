/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2010 University of British Columbia
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
package ubic.gemma.model.analysis.expression.diff;

/**
 * @author paul
 * @version $Id$
 * @see ubic.gemma.model.analysis.ContrastResult
 */
public class ContrastResultImpl extends ubic.gemma.model.analysis.expression.diff.ContrastResult {
    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -4310735803120153778L;

    /**
     * @see ubic.gemma.model.analysis.ContrastResult#toString()
     */
    @Override
    public java.lang.String toString() {
        return "Contrast for " + this.getFactorValue().toString();
    }

}