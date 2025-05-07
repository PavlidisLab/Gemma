package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraFile {
    @XmlAttribute
    private String cluster;
    @XmlAttribute
    private String filename;
    @Nullable
    @XmlAttribute
    private String url;
    @XmlAttribute
    private long size;
    @XmlAttribute
    @XmlJavaTypeAdapter(SraDateParser.class)
    private Date date;
    @XmlAttribute
    private String md5;
    @XmlAttribute
    private String version;
    @XmlAttribute(name = "semantic_name")
    private String semanticName;
    @XmlAttribute
    private String supertype;
    @XmlAttribute
    private int sratoolkit;
}
