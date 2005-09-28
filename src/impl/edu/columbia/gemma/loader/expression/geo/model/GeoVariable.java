/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.loader.expression.geo.model;

import java.util.Collection;

/**
 * A GeoVariable represents variables which were investigated.
 * <p>
 * According to the GEO web site, the valid values for the "name" are: dose, time, tissue, strain, gender, cell line,
 * development stage, age, agent, cell type, infection, isolate, metabolism, shock, stress, temperature, specimen,
 * disease state, protocol, growth protocol, genotype/genetic variation, species, individual, or other.
 * </p>
 * <p>
 * These map to ExperimentalFactors in Gemma.
 * </p>
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class GeoVariable {

    String name;
    String description = "";
    Collection<String> variableSampleList;
    String repeats;
    Collection<String> repeatsSampleList;

    /**
     * @param d
     */
    public void addToDescription( String d ) {
        this.description += d;
    }

    /**
     * @return Returns the description.
     */
    public String getDescription() {
        return this.description;
    }

    /**
     * @param description The description to set.
     */
    public void setDescription( String description ) {
        this.description = description;
    }

    /**
     * @return Returns the repeats.
     */
    public String getRepeats() {
        return this.repeats;
    }

    /**
     * @param repeats The repeats to set.
     */
    public void setRepeats( String repeats ) {
        this.repeats = repeats;
    }

    /**
     * @return Returns the repeatsSampleList.
     */
    public Collection<String> getRepeatsSampleList() {
        return this.repeatsSampleList;
    }

    public void addToVariableSampleList( String sample ) {
        this.variableSampleList.add( sample );
    }

    public void addToRepeatsSampleList( String sample ) {
        this.repeatsSampleList.add( sample );
    }

    /**
     * @param repeatsSampleList The repeatsSampleList to set.
     */
    public void setRepeatsSampleList( Collection<String> repeatsSampleList ) {
        this.repeatsSampleList = repeatsSampleList;
    }

    /**
     * @return Returns the variableSampleList.
     */
    public Collection<String> getVariableSampleList() {
        return this.variableSampleList;
    }

    /**
     * @param variableSampleList The variableSampleList to set.
     */
    public void setVariableSampleList( Collection<String> variableSampleList ) {
        this.variableSampleList = variableSampleList;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name The name to set.
     */
    public void setName( String name ) {
        this.name = name;
    }

}