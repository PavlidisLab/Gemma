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
package ubic.gemma.web.controller.expression.experiment;

import java.io.Serializable;

/**
 * Expression experiment command object that wraps expression experiment search preferences.
 * 
 * @author keshav
 * @version $Id$
 */
public class ExpressionExperimentSearchCommand implements Serializable {

    private static final long serialVersionUID = 2166768356457316142L;

    private int stringency;

    private String searchCriteria = null;

    private String name = null;

    private String description = null;

    private Long id = null;

    private String searchString = null;

    private boolean suppressVisualizations;

    private String filename = null;

    private int imageWidth = 0;

    private int imageHeight = 0;

    /**
     * @return Returns the filename.
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename The filename to set.
     */
    public void setFilename( String filename ) {
        this.filename = filename;
    }

    /**
     * @return boolean
     */
    public boolean isSuppressVisualizations() {
        return suppressVisualizations;
    }

    /**
     * @param suppressVisualizations
     */
    public void setSuppressVisualizations( boolean suppressVisualizations ) {
        this.suppressVisualizations = suppressVisualizations;
    }

    /**
     * @return String
     */
    public String getSearchString() {
        return searchString;
    }

    /**
     * @param searchString
     */
    public void setSearchString( String searchString ) {
        this.searchString = searchString;
    }

    /**
     * @return int
     */
    public int getStringency() {
        return stringency;
    }

    /**
     * @param stringency
     */
    public void setStringency( int stringency ) {
        this.stringency = stringency;
    }

    /**
     * @return String
     */
    public String getSearchCriteria() {
        return searchCriteria;
    }

    /**
     * @param searchCriteria
     */
    public void setSearchCriteria( String searchCriteria ) {
        this.searchCriteria = searchCriteria;
    }

    /**
     * @return String
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * @return Long
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId( Long id ) {
        this.id = id;
    }

    /**
     * @return int
     */
    public int getImageHeight() {
        return imageHeight;
    }

    /**
     * @param imageHeight
     */
    public void setImageHeight( int imageHeight ) {
        this.imageHeight = imageHeight;
    }

    /**
     * @return int
     */
    public int getImageWidth() {
        return imageWidth;
    }

    /**
     * @param imageWidth
     */
    public void setImageWidth( int imageWidth ) {
        this.imageWidth = imageWidth;
    }
}
