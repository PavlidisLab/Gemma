package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraLibraryDescriptor {
    @XmlElement(name = "LIBRARY_NAME")
    private String name;
    @XmlElement(name = "LIBRARY_STRATEGY")
    private String strategy;
    @XmlElement(name = "LIBRARY_SOURCE")
    private String source;
    @XmlElement(name = "LIBRARY_SELECTION")
    private String selection;
    @XmlElement(name = "LIBRARY_LAYOUT")
    private SraLibraryLayout layout;
    @XmlElement(name = "LIBRARY_CONSTRUCTION_PROTOCOL")
    private String constructionProtocol;
}
