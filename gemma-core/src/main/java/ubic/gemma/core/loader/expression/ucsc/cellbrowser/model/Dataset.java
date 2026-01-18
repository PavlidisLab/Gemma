package ubic.gemma.core.loader.expression.ucsc.cellbrowser.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Data
public class Dataset {
    int sampleCount;
    String[][] parents;
    String name;
    boolean matrixWasFiltered;
    List<MetaField> metaFields;
    String labelField;
    List<Coordinate> coords;
    String shortLabel;
    String clusterField;
    @JsonProperty("body_parts")
    List<String> bodyParts;
    List<String> organisms;
    List<String> diseases;
    List<String> hasFiles;
    Map<String, List<String>> topMarkers;
    List<Marker> markers;
    List<List<String>> quickGenes;
    Date firstBuildTipe;
    Date lastBuildTime;
    String md5;
}


