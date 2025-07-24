package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraRunStatistics {
    /**
     * Usually a number, but can take the value "variable".
     */
    @XmlAttribute(name = "nreads")
    private String numberOfReads;
    /**
     * Might be missing if {@link #numberOfReads} is "variable".
     */
    @Nullable
    @XmlAttribute(name = "nspots")
    private Long numberOfSpots;
    /**
     * Might be missing if {@link #numberOfReads} is "variable".
     */
    @Nullable
    @XmlElement(name = "Read")
    private List<SraReadStatistics> readStatistics;
}
