package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraExperiment {
    @XmlAttribute
    private String alias;
    @XmlAttribute
    private String accession;
    @XmlElement(name = "IDENTIFIERS")
    private SraIdentifiers identifiers;
}
