package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraPoolMember {
    @XmlAttribute(name = "member_name")
    private String name;
    @XmlAttribute
    private String accession;
    @XmlAttribute(name = "sample_name")
    private String sampleName;
    @XmlAttribute(name = "sample_title")
    private String sampleTitle;
    @XmlAttribute
    private long spots;
    @XmlAttribute
    private long bases;
    @XmlAttribute(name = "tax_id")
    private int taxonId;
    @XmlAttribute
    private String organism;
    @XmlElement(name = "IDENTIFIER")
    private SraIdentifiers identifiers;
}
