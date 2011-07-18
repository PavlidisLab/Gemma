/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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
package ubic.gemma.tasks.analysis.expression;

import ubic.gemma.job.TaskCommand;

/**
 * @author paul
 * @version $Id$
 */
public class UpdateEEDetailsCommand extends TaskCommand {
 
    private static final long serialVersionUID = 1L;

    String shortName;

    String name;

    String description;
    
    String pubMedId;
    
    boolean removePrimaryPublication;

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the shortName
     */
    public String getShortName() {
        return shortName;
    }

    /**
     * @param description the description to set
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @param name the name to set
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @param shortName the shortName to set
     */
    public void setShortName( String shortName ) {
        this.shortName = shortName;
    }

    public String getPubMedId() {
        return pubMedId;
    }

    public void setPubMedId( String pubMedId ) {
        this.pubMedId = pubMedId;
    }

    public boolean isRemovePrimaryPublication() {
        return removePrimaryPublication;
    }

    public void setRemovePrimaryPublication( boolean removePrimaryPublication ) {
        this.removePrimaryPublication = removePrimaryPublication;
    }

}
