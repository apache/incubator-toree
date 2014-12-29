/*
 * Copyright 2014 IBM Corp.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.ibm.spark.annotations;

// TODO: Investigate using org.scalamacros paradise plugin to provide Scala
//       annotations instead of pursuing Java annotations

import java.lang.annotation.*;

// NOTE: This follows the same pattern as the current Spark annotations and so
//       the same notice was kept below.

/**
 * A stable user-facing API.
 *
 * Stable API's should not change between minor versions of the Spark Kernel.
 * Furthermore, heavy discussion should take place before altering a stable API
 * between major releases. Typically, deprecation warnings should be left if a
 * stable API is being altered.
 *
 * NOTE: If there exists a Scaladoc comment that immediately precedes this
 * annotation, the first line of the comment must be ":: Experimental ::" with
 * no trailing blank line. This is because of the known issue that Scaladoc
 * displays only either the annotation or the comment, whichever comes first.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD,
        ElementType.PARAMETER, ElementType.CONSTRUCTOR,
        ElementType.LOCAL_VARIABLE, ElementType.PACKAGE})
public @interface Stable {}
