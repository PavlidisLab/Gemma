package edu.columbia.gemma.loader.expression.geo.model;

public class GeoSubset extends GeoData {

    private String description;
    private GeoSample sample;
    private String type;

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
     * @return Returns the sample.
     */
    public GeoSample getSample() {
        return this.sample;
    }

    /**
     * @param sample The sample to set.
     */
    public void setSample( GeoSample sample ) {
        this.sample = sample;
    }

    /**
     * @return Returns the type.
     */
    public String getType() {
        return this.type;
    }

    /**
     * @param type The type to set.
     */
    public void setType( String type ) {
        this.type = type;
    }

}
