package org.apache.toree.plugins.annotations;

import java.lang.annotation.*;

/**
 * Represents a marker for a plugin initialization method.
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface Init {}
