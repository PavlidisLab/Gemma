package ubic.gemma.core.search;

import lombok.Value;

import org.springframework.lang.Nullable;
import java.util.function.Consumer;

@Value
public class SearchContext {
    @Nullable
    Highlighter highlighter;
    @Nullable
    Consumer<Throwable> issueReporter;
}
