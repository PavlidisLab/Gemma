package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraContact {
    @XmlAttribute
    private String email;
    // the following are manually parsed
    private String street;
    private String city;
    private String country;
    private String firstName;
    private String lastName;
}
