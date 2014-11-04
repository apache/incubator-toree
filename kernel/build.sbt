import sbtdocker.{ImageName, Dockerfile}
import DockerKeys._

import Common._

fork := true // http://www.scala-sbt.org/0.13/docs/Running-Project-Code.html#Deserialization+and+class+loading

pack <<= pack dependsOn (rebuildIvyXml dependsOn deliverLocal)

packArchive <<= packArchive dependsOn (rebuildIvyXml dependsOn deliverLocal)

//
// AKKA DEPENDENCIES (from Spark project)
//
libraryDependencies +=
  "org.spark-project.akka" %% "akka-zeromq" % "2.2.3-shaded-protobuf" // Apache v2

//
// MAGIC DEPENDENCIES
//
libraryDependencies ++= Seq(
  "org.jfree" % "jfreechart" % "1.0.19" // LGPL
)

//
// TEST DEPENDENCIES
//
libraryDependencies +=
  "org.spark-project.akka" %% "akka-testkit" % "2.2.3-shaded-protobuf" % "test" // MIT

//
// CUSTOM TASKS
//

lazy val kill = taskKey[Unit]("Executing the shell script.")

kill := {
  "sh scripts/terminate_spark_kernels.sh".!
}


//
// Docker config
//

dockerSettings

// Make docker depend on the package task, which generates a jar file of the application code
docker <<= docker.dependsOn(Keys.`package`.in(Compile, packageBin))

// Define a Dockerfile
dockerfile in docker := {
  new Dockerfile {
    // Base image
    from("ubuntu:14.04")
    // Copy all dependencies to 'libs' in stage dir
    //    runShell("apt-key", "adv", "--keyserver", "keyserver.ubuntu.com", "--recv", "E56151BF")
    runShell("gpg", "--keyserver", "hkp://keyserver.ubuntu.com:80", "--recv-keys", "E56151BF")
    runShell("echo", "deb http://repos.mesosphere.io/$(lsb_release -is | tr '[:upper:]' '[:lower:]') $(lsb_release -cs) main", ">>", "/etc/apt/sources.list.d/mesosphere.list")
    runShell("cat", "/etc/apt/sources.list.d/mesosphere.list")
    runShell("apt-get", "update")
    runShell("apt-get", "--no-install-recommends","-y" ,"--force-yes", "install", "openjdk-7-jre", "mesos=0.20.1-1.0.ubuntu1404", "make", "libzmq-dev")
    env("MESOS_NATIVE_LIBRARY","/usr/local/lib/libmesos.so")
    //  Install the pack elements
    stageFile(baseDirectory.value / "target" / "pack" / "Makefile", "/app/Makefile")
    stageFile(baseDirectory.value / "target" / "pack" / "VERSION", "/app/VERSION")
    stageFile(baseDirectory.value / "target" / "pack" / "lib", "/app/lib")
    stageFile(baseDirectory.value / "target" / "pack" / "bin", "/app/bin")
    add("/app", "/app")
    workDir("/app")
    run("chmod" , "+x", "/app/bin/sparkkernel")
    // On launch run Java with the classpath and the main class
    entryPoint("/app/bin/sparkkernel")
  }
}

// Set a custom image name
imageName in docker := {
  //  TODO Temporary fix for Mesos because it does not like : in the image name
  val kernelBuildId = if(System.getenv("KERNEL_BUILD_ID") != null)  Option(System.getenv("KERNEL_BUILD_ID")) else Option( "-v" + version.value)
  val kernelImageId = if(System.getenv("KERNEL_IMAGE") != null)  System.getenv("KERNEL_IMAGE") else name.value + kernelBuildId.get
  val dockerRegistry= if(System.getenv("DOCKER_REGISTRY") != null)  Option(System.getenv("DOCKER_REGISTRY")) else Option(organization.value + ":5000")
  ImageName(
    namespace = dockerRegistry,
    repository = kernelImageId,
    tag = kernelBuildId)
}
