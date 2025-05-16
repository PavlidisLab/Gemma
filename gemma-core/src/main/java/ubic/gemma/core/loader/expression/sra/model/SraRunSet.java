package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
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
