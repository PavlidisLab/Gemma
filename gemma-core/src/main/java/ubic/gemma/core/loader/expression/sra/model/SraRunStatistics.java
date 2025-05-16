package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraRunStatistics {
    @XmlAttribute(name = "nreads")
    private long numberOfReads;
    @XmlAttribute(name = "nspots")
    private long numberOfSpots;
    @XmlElement(name = "Read")
    private List<SraReadStatistics> readStatistics;
}
