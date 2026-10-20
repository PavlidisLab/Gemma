package ubic.gemma.core.loader.expression.ucsc.cellbrowser.model;

import lombok.Data;

@Data
public class Coordinate {
    String name;
    String shortLabel;
    double minX;
    double maxX;
    double minY;
    double maxY;
    String type;
    String textFname;
    String md5;
    String labelMd5;
}
