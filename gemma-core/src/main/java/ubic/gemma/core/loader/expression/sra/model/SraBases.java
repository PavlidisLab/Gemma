package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
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
