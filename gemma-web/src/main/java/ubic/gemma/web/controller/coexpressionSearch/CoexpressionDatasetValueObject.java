/*
 * The Gemma project
 * 
 * Copyright (c) 2006 Columbia University
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
package ubic.gemma.web.controller.coexpressionSearch;


/**
 * @author luke
 */
public class CoexpressionDatasetValueObject {
    
    private Long id;
    private String queryGene;
    private Boolean probeSpecificForQueryGene;
    private Long arrayDesignCount;
    private Long bioAssayCount;
    private Long rawCoexpressionLinkCount;
    private Long coexpressionLinkCount;
    
    public Long getId() {
        return id;
    }
    
    public void setId( Long id ) {
        this.id = id;
    }

    public String getQueryGene() {
        return queryGene;
    }

    public void setQueryGene( String queryGene ) {
        this.queryGene = queryGene;
    }
    
    public Boolean getProbeSpecificForQueryGene() {
        return probeSpecificForQueryGene;
    }
    
    public void setProbeSpecificForQueryGene( Boolean probeSpecificForQueryGene ) {
        this.probeSpecificForQueryGene = probeSpecificForQueryGene;
    }
    
    public Long getArrayDesignCount() {
        return arrayDesignCount;
    }
    
    public void setArrayDesignCount( Long arrayDesignCount ) {
        this.arrayDesignCount = arrayDesignCount;
    }
    
    public Long getBioAssayCount() {
        return bioAssayCount;
    }
    
    public void setBioAssayCount( Long bioAssayCount ) {
        this.bioAssayCount = bioAssayCount;
    }
    
    public Long getRawCoexpressionLinkCount() {
        return rawCoexpressionLinkCount;
    }
    
    public void setRawCoexpressionLinkCount( Long rawCoexpressionLinkCount ) {
        this.rawCoexpressionLinkCount = rawCoexpressionLinkCount;
    }
    
    public Long getCoexpressionLinkCount() {
        return coexpressionLinkCount;
    }
    
    public void setCoexpressionLinkCount( Long coexpressionLinkCount ) {
        this.coexpressionLinkCount = coexpressionLinkCount;
    }
        
}
