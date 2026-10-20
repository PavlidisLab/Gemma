package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraOrganization {
    @XmlAttribute
    private String type;
    @XmlElement(name = "Name")
    private String name;
    @XmlElement(name = "Contact")
    private SraContact contact;
}
