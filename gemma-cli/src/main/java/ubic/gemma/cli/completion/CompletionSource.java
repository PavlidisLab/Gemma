package ubic.gemma.cli.completion;

import lombok.Value;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Defines a source of completions.
 * @author poirigui
 */
public interface CompletionSource {

    List<Completion> getCompletions();

    @Value
    class Completion {
        String name;
        @Nullable
        String description;
    }
}
