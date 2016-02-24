package org.apache.toree.plugins.annotations;

import java.lang.annotation.*;

/**
 * Represents a marker for a plugin shutdown method.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Destroy {}
