/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.analysis.expression.diff;

/**
 * Helper object, not for general use.
 * 
 * @author Paul
 * @version $Id$
 */
public class ContrastVO {

    private Long factorValueId;

    private Long id;

    private Double logFoldChange = null;

    private Double pvalue = null;

    public ContrastVO( Long id, Long factorValueId, Double logFoldchange, Double pvalue ) {
        super();
        this.id = id;
        this.factorValueId = factorValueId;
        this.logFoldChange = logFoldchange;
        this.pvalue = pvalue;
    }

    public Long getFactorValueId() {
        return factorValueId;
    }

    public Long getId() {
        return id;
    }

    public Double getLogFoldChange() {
        return logFoldChange;
    }

    public Double getPvalue() {
        return pvalue;
    }

}