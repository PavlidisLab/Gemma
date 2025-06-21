/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.analysis.expression.diff;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.analysis.expression.diff.DiffExpressionSelectedFactorCommand;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author keshav
 */
public class DiffExpressionSearchCommand implements Serializable {

    private static final long serialVersionUID = 1L;

    /* analysis */
    private Collection<Long> geneIds;

    private Double threshold;

    private Long taxonId;

    /* meta analysis */
    private Collection<DiffExpressionSelectedFactorCommand> selectedFactors;

    private String eeQuery;

    private Long eeSetId;

    private String eeSetName;

    private Collection<Long> eeIds;

    private boolean dirty;

    public Collection<Long> getEeIds() {
        return eeIds;
    }

    public void setEeIds( Collection<Long> eeIds ) {
        this.eeIds = eeIds;
    }

    public String getEeQuery() {
        return eeQuery;
    }

    public void setEeQuery( String eeQuery ) {
        this.eeQuery = eeQuery;
    }

    public Long getEeSetId() {
        return eeSetId;
    }

    public void setEeSetId( Long eeSetId ) {
        this.eeSetId = eeSetId;
    }

    public String getEeSetName() {
        return eeSetName;
    }

    public void setEeSetName( String eeSetName ) {
        this.eeSetName = eeSetName;
    }

    public Collection<Long> getGeneIds() {
        return geneIds;
    }

    public void setGeneIds( Collection<Long> geneIds ) {
        this.geneIds = geneIds;
    }

    public Collection<DiffExpressionSelectedFactorCommand> getSelectedFactors() {
        return selectedFactors;
    }

    public void setSelectedFactors( Collection<DiffExpressionSelectedFactorCommand> selectedFactors ) {
        this.selectedFactors = selectedFactors;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold( Double threshold ) {
        this.threshold = threshold;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty( boolean dirty ) {
        this.dirty = dirty;
    }

    @Override
    public String toString() {
        return "GeneIds=" + StringUtils.join( this.getGeneIds(), "," ) + " taxon=" + this.getTaxonId() + " Threshold="
                + threshold;
    }
}
