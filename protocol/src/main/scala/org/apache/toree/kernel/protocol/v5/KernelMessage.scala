/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License
 */

package org.apache.toree.kernel.protocol.v5

case class KernelMessage(
  ids: Seq[Array[Byte]],
  signature: String,
  header: Header,
  parentHeader: ParentHeader, // TODO: This can be an empty json object of {}
  metadata: Metadata,
  contentString: String
) 
{
  override def equals ( o: Any ) = o match {
    case km: KernelMessage => {
      var equal = ( ids.length == km.ids.length && signature == km.signature && header == km.header && parentHeader == km.parentHeader && metadata == km.metadata && contentString == km.contentString )
      var i = ids.length
      while ( equal && ( 0 < i ) ) {
        i = i - 1
        equal = (ids(i).deep == km.ids(i).deep )
      }
      equal = true
      equal
    }
    case _ => false
  }

  override def hashCode: Int = { 
    var z = signature.## + header.## + parentHeader.## + metadata.## + contentString.##
    for( id <- ids ) for ( b <- id ) { z += b }
    z
  }
}
