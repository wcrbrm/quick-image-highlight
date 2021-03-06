// Name of the project
name := "Quick Image Highlight"

// Project version
version := "0.1.1"

// Version of Scala used by the project
scalaVersion := "2.12.4"

// Add dependency on ScalaFX library
libraryDependencies += "org.scalafx" %% "scalafx" % "8.0.144-R12"
libraryDependencies += "fr.janalyse" %% "janalyse-ssh" % "0.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xcheckinit", "-encoding", "utf8", "-feature")

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true
