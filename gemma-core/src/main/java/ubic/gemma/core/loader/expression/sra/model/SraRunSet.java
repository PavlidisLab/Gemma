package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraRunSet {
    @XmlAttribute
    private long bases;
    @XmlAttribute
    private long spots;
    @XmlAttribute
    private long bytes;
    @XmlElement(name = "RUN")
    private List<SraRun> runs;
}
