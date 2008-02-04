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
public class ExtCoexpressionDatasetValueObject {
    
    private Long id;
    private String name;
    private String shortName;
    private Boolean probeSpecificForQueryGene;
    private Integer arrayDesignCount;
    private Integer bioAssayCount;
    private String queryGene;
    private Integer rawCoexpressionLinkCount;
    private Integer coexpressionLinkCount;
    
    public Long getId() {
        return id;
    }
    
    public void setId( Long id ) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName( String name ) {
        this.name = name;
    }
    
    public String getShortName() {
        return shortName;
    }
    
    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }
    
    public Boolean getProbeSpecificForQueryGene() {
        return probeSpecificForQueryGene;
    }
    
    public void setProbeSpecificForQueryGene( Boolean probeSpecificForQueryGene ) {
        this.probeSpecificForQueryGene = probeSpecificForQueryGene;
    }
    
    public Integer getArrayDesignCount() {
        return arrayDesignCount;
    }
    
    public void setArrayDesignCount( Integer arrayDesignCount ) {
        this.arrayDesignCount = arrayDesignCount;
    }
    
    public Integer getBioAssayCount() {
        return bioAssayCount;
    }
    
    public void setBioAssayCount( Integer bioAssayCount ) {
        this.bioAssayCount = bioAssayCount;
    }
    
    public Integer getRawCoexpressionLinkCount() {
        return rawCoexpressionLinkCount;
    }
    
    public void setRawCoexpressionLinkCount( Integer rawCoexpressionLinkCount ) {
        this.rawCoexpressionLinkCount = rawCoexpressionLinkCount;
    }
    
    public Integer getCoexpressionLinkCount() {
        return coexpressionLinkCount;
    }
    
    public void setCoexpressionLinkCount( Integer coexpressionLinkCount ) {
        this.coexpressionLinkCount = coexpressionLinkCount;
    }
        
}
