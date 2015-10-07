#
# Copyright 2015 IBM Corp.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

#
# Reduces a collection of character vectors to a single character vector of
# length 1
#
# obj: The object representing the character vector to flatten
# shouldTrim: If true, will trim each individual element
# sepCharacter: Used as the separator between combined strings
#
flatten <- function(obj, shouldTrim = TRUE, sepCharacter = "\n") {
  Reduce(function(x, y) {
    flattenedX <- if (length(x) > 1) flatten(x) else x
    flattenedY <- if (length(y) > 1) flatten(y) else y

    finalX <- if (shouldTrim) trimws(flattenedX) else flattenedX
    finalY <- if (shouldTrim) trimws(flattenedY) else flattenedY

    paste(finalX, finalY, sep = sepCharacter)
  }, obj)
}
