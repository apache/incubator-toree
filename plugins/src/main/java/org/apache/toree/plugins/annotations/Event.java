package org.apache.toree.plugins.annotations;

import java.lang.annotation.*;

/**
 * Represents a marker for a generic plugin event.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Event {
    String name();
}
