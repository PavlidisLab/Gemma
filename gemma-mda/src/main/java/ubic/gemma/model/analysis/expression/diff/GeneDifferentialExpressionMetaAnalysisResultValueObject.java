/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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

import java.io.Serializable;

/**
 * @author frances
 * @version $Id$
 */
public class GeneDifferentialExpressionMetaAnalysisResultValueObject implements Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 6099286095885830140L;

    private String geneSymbol;
    private String geneName;
    private Double metaPvalue;
    private Double metaQvalue;
    public Boolean upperTail;

    public String getGeneSymbol() {
        return this.geneSymbol;
    }

    public void setGeneSymbol( String geneSymbol ) {
        this.geneSymbol = geneSymbol;
    }

    public String getGeneName() {
        return this.geneName;
    }

    public void setGeneName( String geneName ) {
        this.geneName = geneName;
    }

    public Double getMetaPvalue() {
        return this.metaPvalue;
    }

    public void setMetaPvalue( Double metaPvalue ) {
        this.metaPvalue = metaPvalue;
    }

    public Double getMetaQvalue() {
        return this.metaQvalue;
    }

    public void setMetaQvalue( Double metaQvalue ) {
        this.metaQvalue = metaQvalue;
    }

    public Boolean getUpperTail() {
        return this.upperTail;
    }

    public void setUpperTail( Boolean upperTail ) {
        this.upperTail = upperTail;
    }
}
