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
package ubic.gemma.core.analysis.expression.coexpression;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author luke
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class CoexpressionSearchCommand implements Serializable {

    /**
     * Set to true to signal that the eeSet has been modified from its stored version.
     */
    private boolean dirty = false;

    /**
     * if set to true will do a quick coexpression search (without filling in various details)
     */
    private boolean quick = false;

    private Collection<Long> eeIds;

    /**
     * we're storing the actual ee ids in the command object; the query string is only here so we can use this object to
     * store the state of the search form between visits...
     */
    private String eeQuery;

    private Long eeSetId;

    private String eeSetName;

    private Long geneSetId;
    private Collection<Long> geneIds;
    private boolean queryGenesOnly;
    private Integer stringency;
    private boolean useMyDatasets;
    /**
     * as eeQuery above, the taxon is only here so we can use this object to store the entire state of the form...
     */
    private Long taxonId;

    public Long getGeneSetId() {
        return geneSetId;
    }

    public void setGeneSetId( Long geneSetId ) {
        this.geneSetId = geneSetId;
    }

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

    public boolean getQueryGenesOnly() {
        return queryGenesOnly;
    }

    public void setQueryGenesOnly( boolean queryGenesOnly ) {
        this.queryGenesOnly = queryGenesOnly;
    }

    public Integer getStringency() {
        return stringency;
    }

    public void setStringency( Integer stringency ) {
        this.stringency = stringency;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty( boolean dirty ) {
        this.dirty = dirty;
    }

    public boolean isQuick() {
        return quick;
    }

    public void setQuick( boolean quick ) {
        this.quick = quick;
    }

    public boolean isUseMyDatasets() {
        return this.useMyDatasets;
    }

    public void setUseMyDatasets( boolean useMyDatasets ) {
        this.useMyDatasets = useMyDatasets;
    }

    @Override
    public String toString() {
        return "Genes=" + StringUtils.abbreviate( StringUtils.join( this.getGeneIds(), "," ), 100 ) + " EESet=" + this
                .getEeSetId() + " QueryGenesOnly=" + this.getQueryGenesOnly() + " tax=" + this.getTaxonId() + (
                stringency > 1 ?
                        " Stringency=" + stringency :
                        "" ) + " ees=" + StringUtils.abbreviate( StringUtils.join( this.getEeIds(), "," ), 100 );
    }

}
