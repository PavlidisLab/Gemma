package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SraRun {
    @XmlElement(name = "IDENTIFIERS")
    private SraIdentifiers identifiers;
    @XmlElement(name = "Pool")
    private SraPool pool;
    @XmlAttribute
    private String alias;
    @XmlAttribute
    private String accession;
    @XmlAttribute(name = "total_spots")
    private long totalSpots;
    @XmlAttribute(name = "total_bases")
    private long totalBases;
    @XmlAttribute
    private long size;
    @XmlAttribute(name = "load_done")
    private boolean loadDone;
    @XmlAttribute
    @XmlJavaTypeAdapter(SraDateParser.class)
    private Date published;
    @XmlAttribute(name = "is_public")
    private boolean isPublic;
    @XmlAttribute(name = "cluster_name")
    private String clusterName;
    @XmlElement(name = "SRAFiles")
    private SraFiles files;
    @XmlElement(name = "Statistics")
    private SraRunStatistics statistics;
    @XmlElement(name = "Bases")
    private SraBases bases;
}
