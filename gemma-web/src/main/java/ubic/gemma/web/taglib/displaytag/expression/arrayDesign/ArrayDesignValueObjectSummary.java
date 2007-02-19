/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.web.taglib.displaytag.expression.arrayDesign;

import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;

/**
 * 
 * @author joseph
 * @version $Id$
 */
public class ArrayDesignValueObjectSummary extends ArrayDesignValueObject {

    /**
     * 
     */
    private static final long serialVersionUID = 4317373121919405446L;

    private String lastSequenceUpdate;

    private String lastSequenceAnalysis;
    
    private String lastGeneMapping;

    
    private String summaryTable;

    public ArrayDesignValueObjectSummary() {
        super();
    }

    public ArrayDesignValueObjectSummary( ArrayDesignValueObject otherBean ) {
        super( otherBean );
    }

    public ArrayDesignValueObjectSummary(  ArrayDesignValueObject otherBean ,String summaryTable ) {
        super(otherBean);
        this.summaryTable = summaryTable;
    }
    
    public String getLastGeneMapping() {
        return lastGeneMapping;
    }

    public String getLastSequenceAnalysis() {
        return lastSequenceAnalysis;
    }

    public String getLastSequenceUpdate() {
        return lastSequenceUpdate;
    }

    /**
     * @return the summaryTable
     */
    public String getSummaryTable() {
        return summaryTable;
    }

    public void setLastGeneMapping( String lastGeneMapping ) {
        this.lastGeneMapping = lastGeneMapping;
    }


    public void setLastSequenceAnalysis( String lastSequenceAnalysis ) {
        this.lastSequenceAnalysis = lastSequenceAnalysis;
    }


    public void setLastSequenceUpdate( String lastSequenceUpdate ) {
        this.lastSequenceUpdate = lastSequenceUpdate;
    }


    /**
     * @param summaryTable the summaryTable to set
     */
    public void setSummaryTable( String summaryTable ) {
        this.summaryTable = summaryTable;
    }
}
