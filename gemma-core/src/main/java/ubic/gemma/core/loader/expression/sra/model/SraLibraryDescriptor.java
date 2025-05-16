package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

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
