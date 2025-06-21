package ubic.gemma.rest.util;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import ubic.gemma.core.util.BuildInfo;

import java.util.Date;

@Value
@Builder
@Jacksonized
public class BuildInfoValueObject {

    public static BuildInfoValueObject from( BuildInfo buildInfo ) {
        return BuildInfoValueObject.builder()
                .version( buildInfo.getVersion() )
                .timestamp( buildInfo.getTimestamp() )
                .gitHash( buildInfo.getGitHash() )
                .build();
    }

    String version;
    Date timestamp;
    String gitHash;
}
