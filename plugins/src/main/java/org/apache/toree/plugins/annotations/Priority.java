package org.apache.toree.plugins.annotations;

import java.lang.annotation.*;

/**
 * Represents an indicator of priority for plugins and plugin methods.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface Priority {
    long level();
}
