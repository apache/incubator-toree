package org.apache.toree.plugins.annotations;

import java.lang.annotation.*;

/**
 * Represents a marker for multiple generic plugin events.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Events {
    String[] names();
}
