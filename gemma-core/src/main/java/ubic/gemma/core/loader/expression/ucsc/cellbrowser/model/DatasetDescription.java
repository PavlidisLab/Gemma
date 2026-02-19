package ubic.gemma.core.loader.expression.ucsc.cellbrowser.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class DatasetDescription {
    String title;
    @JsonProperty("abstract")
    String abstract_;
    @JsonProperty("paper_url")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    List<String> paperUrl;
    @JsonProperty("geo_series")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    List<String> geoSeries;
    String downloads;
    List<String> coordFiles;
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    List<String> pmid;
    String matrixFile;
    String methods;
}
