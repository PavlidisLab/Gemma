package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraSubmission {
    @XmlAttribute
    private String alias;
    @XmlAttribute(name = "broker_name")
    private String brokerName;
    @XmlAttribute(name = "center_name")
    private String centerName;
    @XmlAttribute(name = "submission_comment")
    private String submissionComment;
    @XmlAttribute(name = "lab_name")
    private String labName;
    @XmlAttribute
    private String accession;
    @XmlElement(name = "IDENTIFIERS")
    private SraIdentifiers identifiers;
    @XmlElement(name = "Organization")
    private SraOrganization organization;
}
