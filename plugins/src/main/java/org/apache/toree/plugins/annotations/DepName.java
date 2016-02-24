package org.apache.toree.plugins.annotations;

import java.lang.annotation.*;

/**
 * Represents a marker for loading a dependency for a specific name.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
public @interface DepName {
    String name();
}
