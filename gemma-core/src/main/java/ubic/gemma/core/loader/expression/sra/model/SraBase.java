package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraBase {
    @XmlAttribute
    private String value;
    @XmlAttribute
    private long count;
}
