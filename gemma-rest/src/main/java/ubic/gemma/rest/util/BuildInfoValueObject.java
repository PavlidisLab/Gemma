package ubic.gemma.rest.util;

import lombok.Value;
import ubic.gemma.core.util.BuildInfo;

import java.util.Date;

@Value
public class BuildInfoValueObject {
    String version;
    Date timestamp;
    String gitHash;

    public BuildInfoValueObject( BuildInfo buildInfo ) {
        this.version = buildInfo.getVersion();
        this.timestamp = buildInfo.getTimestamp();
        this.gitHash = buildInfo.getGitHash();
    }
}
