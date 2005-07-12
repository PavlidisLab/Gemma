package edu.columbia.gemma.loader.expression.geo.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GeoData {

    Map<String, List<String>> data = new HashMap<String, List<String>>();;

    GeoContact contact = new GeoContact();

    String geoAccesssion;

    public Map getData() {
        return this.data;
    }

    public GeoContact getContact() {
        return this.contact;
    }

    /**
     * @return Returns the geoAccesssion.
     */
    public String getGeoAccesssion() {
        return this.geoAccesssion;
    }

    /**
     * @param geoAccesssion The geoAccesssion to set.
     */
    public void setGeoAccesssion( String geoAccesssion ) {
        this.geoAccesssion = geoAccesssion;
    }

}
