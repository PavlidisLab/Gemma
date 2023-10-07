package ubic.gemma.core.util;

import ubic.gemma.core.apps.GemmaCLI;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    String name();

    String description();

    GemmaCLI.CommandGroup group() default GemmaCLI.CommandGroup.MISC;
}
