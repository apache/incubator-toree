package system

import org.apache.toree.boot.layer.SparkKernelDeployer
import org.scalatest.{BeforeAndAfterAll, Suites}

class SuiteForSystem extends Suites(
  new KernelCommSpecForSystem,
  new TruncationTests
) with BeforeAndAfterAll {
  override protected def beforeAll(): Unit = {
    // Initialize the kernel for system tests
    println("Initializing kernel for system tests")
    SparkKernelDeployer.noArgKernelBootstrap
  }
}
