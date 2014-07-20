import AssemblyKeys._

assemblySettings

//
// NOTE: IntelliJ reports errors about type mismatch, but this still works
//       when running with sbt in a terminal.
//
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
      case PathList("META-INF", "ECLIPSEF.RSA") => MergeStrategy.discard
      case PathList("META-INF", "mailcap") => MergeStrategy.discard
      case PathList("META-INF", "native", "osx", "libjansi.jnilib") => MergeStrategy.first
      case PathList("META-INF", "native", "windows32", "jansi.dll") => MergeStrategy.first
      case PathList("META-INF", "native", "windows64", "jansi.dll") => MergeStrategy.first
      case x => old(x)
    }
  }

