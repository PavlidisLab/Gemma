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
package ubic.gemma.analysis.expression.coexpression;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

/**
 * @author luke
 * @version $Id$
 */
public class CoexpressionSearchCommand {

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

    public Long getGeneSetId() {
        return geneSetId;
    }

    public void setGeneSetId( Long geneSetId ) {
        this.geneSetId = geneSetId;
    }

    private Collection<Long> geneIds;

    private boolean queryGenesOnly;

    private Integer stringency;

    private boolean useMyDatasets;

    /**
     * as eeQuery above, the taxon is only here so we can use this object to store the entire state of the form...
     */
    private Long taxonId;

    public Collection<Long> getEeIds() {
        return eeIds;
    }

    public String getEeQuery() {
        return eeQuery;
    }

    public Long getEeSetId() {
        return eeSetId;
    }

    public String getEeSetName() {
        return eeSetName;
    }

    public Collection<Long> getGeneIds() {
        return geneIds;
    }

    public boolean getQueryGenesOnly() {
        return queryGenesOnly;
    }

    public Integer getStringency() {
        return stringency;
    }

    public Long getTaxonId() {
        return taxonId;
    }

    public boolean isDirty() {
        return dirty;
    }

    public boolean isQuick() {
        return quick;
    }

    public boolean isUseMyDatasets() {
        return this.useMyDatasets;
    }

    public void setDirty( boolean dirty ) {
        this.dirty = dirty;
    }

    public void setEeIds( Collection<Long> eeIds ) {
        this.eeIds = eeIds;
    }

    public void setEeQuery( String eeQuery ) {
        this.eeQuery = eeQuery;
    }

    public void setEeSetId( Long eeSetId ) {
        this.eeSetId = eeSetId;
    }

    public void setEeSetName( String eeSetName ) {
        this.eeSetName = eeSetName;
    }

    public void setGeneIds( Collection<Long> geneIds ) {
        this.geneIds = geneIds;
    }

    public void setQueryGenesOnly( boolean queryGenesOnly ) {
        this.queryGenesOnly = queryGenesOnly;
    }

    public void setQuick( boolean quick ) {
        this.quick = quick;
    }

    public void setStringency( Integer stringency ) {
        this.stringency = stringency;
    }

    public void setTaxonId( Long taxonId ) {
        this.taxonId = taxonId;
    }

    public void setUseMyDatasets( boolean useMyDatasets ) {
        this.useMyDatasets = useMyDatasets;
    }

    @Override
    public String toString() {
        return "Genes=" + StringUtils.abbreviate( StringUtils.join( getGeneIds(), "," ), 100 ) + " EESet="
                + this.getEeSetId() + " QueryGenesOnly=" + this.getQueryGenesOnly() + " tax=" + getTaxonId()
                + " Stringency=" + stringency + " ees="
                + StringUtils.abbreviate( StringUtils.join( getEeIds(), "," ), 100 );
    }

}
