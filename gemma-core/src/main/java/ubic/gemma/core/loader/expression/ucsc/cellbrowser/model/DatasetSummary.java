package ubic.gemma.core.loader.expression.ucsc.cellbrowser.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import org.springframework.lang.Nullable;
import java.util.List;

@Data
public class DatasetSummary {
    String name;
    String shortLabel;
    List<String> hasFile;
    @Nullable
    @JsonProperty("body_parts")
    List<String> bodyParts;
    @Nullable
    List<String> diseases;
    @Nullable
    List<String> organisms;
    boolean isCollection;
    int datasetCount;
    String md5;
}
