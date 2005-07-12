package edu.columbia.gemma.loader.expression.geo.model;

import java.util.Collection;

public class GeoVariable {

    String description;
    Collection<String> variableSampleList;
    Collection<String> repeats;
    Collection<String> repeatsSampleList;

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
    public Collection<String> getRepeats() {
        return this.repeats;
    }

    /**
     * @param repeats The repeats to set.
     */
    public void setRepeats( Collection<String> repeats ) {
        this.repeats = repeats;
    }

    /**
     * @return Returns the repeatsSampleList.
     */
    public Collection<String> getRepeatsSampleList() {
        return this.repeatsSampleList;
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

}