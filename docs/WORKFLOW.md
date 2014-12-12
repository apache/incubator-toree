Spark Kernel Development Workflow
=================================

While it is not necessary to follow this guide for development, it is being
documented to encourage some form of standard practice for this project.

Tooling
-------

Most of the developers for the Spark Kernel thus far have chosen to use
_IntelliJ_ as their means of development. Because of this, a plugin for _sbt_
is included in our project to allow easy construction of an IntelliJ project
that contains all of the modules.

Obviously, _git_ is used as the source control for the project.

Finally, we use _sbt_ for our build and test runner. You can find more
information about compiling/testing in the main RAEDME.

Building IntelliJ Project
-------------------------

To build the IntelliJ project using _sbt_, you can trigger the plugin by
executing the following from the root of the Spark Kernel project:

    sbt gen-idea

This should create *.idea/* and *.idea\_modules/*  directories.

From there, you should be able to open (not import) the project using IntelliJ.

Using Branches for Development
------------------------------

When we tackle defects or features in the Spark Kernel, we typically break the
problems up into the smallest pieces possible. Once we have something simple
like "I need the kernel to print out hello world when it starts," we create a
branch from our development branch (in the case of this project, it is
typically master). For this example, let's call the branch 
"AddHelloWorldDuringBoot" and use it for our feature.

Once development has finished, it is good practice to ensure that all tests
are still passing. To do this, run `sbt test` from the root of the Spark Kernel
project.

If everything passes, we want to ensure that our branch is up-to-date with the
latest code in the kernel. So, move back to the development branch (master in
our case) and pull the latest changes. If there are changes, we want to rebase
our branch on top of those new changes. From the _AddHelloWorldDuringBoot_
branch, run `git rebase master` to bring the branch up to speed with master.

The advantage of using rebase on a _local_ branch is that it makes merging back
with _master_ much cleaner for the maintainers. If your branch has been pushed
remotely, you want to avoid rebasing in case someone else has branched off of
your branch. Tricky stuff!

After rebasing on top of master, it is a good idea to rerun the tests for your
branch to ensure that nothing has broken from the changes: `sbt test`

Finally, if the tests pass, switch back to the development branch (master) and
merge the changes: `git merge AddHelloWorldDuringBoot`. As a last check,
rerun the tests to ensure that the merge went well (`sbt test` in master). If
those tests still pass, the changes can be pushed!

Writing proper unit tests
-------------------------

The goal of our unit tests was to be isolated. This means that absolutely _no_
external logic is needed to run the tests. This includes fixtures and any
possible dependencies referenced in the code. We use _Mockito_ to provide
mocking facilities for our dependencies and try our best to isolate dependency
creation.

    class MyClass {
        // Bad design
        val someDependency = new SomeDependency()

        // ...
    }

instead move it to the constructor

    class MyClass(someDependency: SomeDependency) {
        // ...
    }

or use traits to mix in dependencies

    trait MyDependency {
        val someDependency = new SomeDependency()
    }

    class MyClass extends MyDependency {

    }

For testing, we use _ScalaTest_ with the _FunSpec_ to provide the basic
structure of our tests (in a BDD manner). Typically, _Matchers_ from
_ScalaTest_ are also included to provide a better flow.

    class MyClassSpec extends FunSpec with Matchers {
        describe("MyClass") {
            describe("#someMethod") {
                it("should indicate success by default") {
                    val myClass = new MyClass(new SomeDependency())
                    val expected = true
                    val actual = myClass.someMethod()

                    actual should be (expected)
                }
            }
        }
    }

The above structure is to use a _describe_ block to represent the name of the
class being tested. We nest a second layer of _describe_ blocks to indicate
tests for individual public methods. Finally, _it_ blocks are structured to
test single cases (such as different logical routes to be encountered).

We have attempted to keep the majority of our tests clear and concise.
Typically, we avoid helper functions because they can obfuscate the tests.

