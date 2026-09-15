package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlValue;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraSubmitterId {
    @XmlAttribute
    private String namespace;
    @XmlValue
    private String id;
}
