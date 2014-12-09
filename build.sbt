name := "require-dependency-analyser"

version := "0.0.1"

scalaVersion := "2.10.0"

libraryDependencies += "org.jgrapht" % "jgrapht-core" % "0.9.0"

libraryDependencies += "org.jgrapht" % "jgrapht-ext" % "0.9.0"


mainClass in (Compile, run) := Some("Search")