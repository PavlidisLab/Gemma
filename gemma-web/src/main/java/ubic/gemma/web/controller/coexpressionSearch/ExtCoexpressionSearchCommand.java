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

import java.util.Collection;

/**
 * @author luke
 */
public class ExtCoexpressionSearchCommand {
    
    private Collection<Long> geneIds;
    
    private Collection<Long> eeIds;
    
    private Long cannedAnalysisId;

    private Integer stringency;

    public Collection<Long> getGeneIds() {
        return geneIds;
    }

    public void setGeneIds( Collection<Long> geneIds ) {
        this.geneIds = geneIds;
    }

    public Collection<Long> getEeIds() {
        return eeIds;
    }

    public void setEeIds( Collection<Long> eeIds ) {
        this.eeIds = eeIds;
    }

    public Long getCannedAnalysisId() {
        return cannedAnalysisId;
    }

    public void setCannedAnalysisId( Long cannedAnalysisId ) {
        this.cannedAnalysisId = cannedAnalysisId;
    }

    public Integer getStringency() {
        return stringency;
    }

    public void setStringency( Integer stringency ) {
        this.stringency = stringency;
    }
}
