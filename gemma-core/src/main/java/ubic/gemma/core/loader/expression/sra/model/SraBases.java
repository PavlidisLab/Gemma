package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraBases {
    @XmlAttribute(name = "cs_native")
    private boolean csNative;
    @XmlAttribute
    private long count;
    @XmlElement(name = "Base")
    private List<SraBase> bases;
}
