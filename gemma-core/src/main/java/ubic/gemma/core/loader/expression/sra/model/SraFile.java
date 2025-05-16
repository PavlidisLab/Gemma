package ubic.gemma.core.loader.expression.sra.model;

import lombok.Data;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Date;

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
    @XmlJavaTypeAdapter(IntToBool.class)
    private Boolean sratoolkit;

    private static class IntToBool extends XmlAdapter<String, Boolean> {
        @Override
        public Boolean unmarshal( String v ) {
            return !v.equals( "0" );
        }

        @Override
        public String marshal( Boolean v ) {
            return v ? "1" : "0";
        }
    }
}
