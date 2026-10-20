package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraExperiment {
    @XmlAttribute
    private String alias;
    @XmlAttribute
    private String accession;
    @XmlElement(name = "IDENTIFIERS")
    private SraIdentifiers identifiers;
    @XmlElement(name = "TITLE")
    private String title;
    @XmlElement(name = "DESIGN")
    private SraExperimentDesign design;
    @XmlElement(name = "PLATFORM")
    private SraPlatform platform;
}
