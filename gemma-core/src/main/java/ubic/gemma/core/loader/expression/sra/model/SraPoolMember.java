package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

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
