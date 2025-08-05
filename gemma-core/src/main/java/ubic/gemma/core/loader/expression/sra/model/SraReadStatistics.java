package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraReadStatistics {
    @XmlAttribute
    private int index;
    @XmlAttribute
    private int count;
    @XmlAttribute
    private double average;
    @XmlAttribute
    private double stdev;
}
