package ubic.gemma.cli.completion;

import ubic.gemma.cli.logging.LoggingConfigurer;

import java.util.List;
import java.util.stream.Collectors;

public class LoggerCompletionSource implements CompletionSource {

    private final LoggingConfigurer loggingConfigurer;

    public LoggerCompletionSource( LoggingConfigurer loggingConfigurer ) {
        this.loggingConfigurer = loggingConfigurer;
    }

    @Override
    public List<Completion> getCompletions() {
        return loggingConfigurer.getAllLoggerNames().stream()
                .map( x -> new Completion( x, x.isEmpty() ? "Root logger" : "" ) )
                .collect( Collectors.toList() );
    }
}
