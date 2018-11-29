import sbt.Keys._
import sbt._


name := "SparkJob"

version := "1.0"

parallelExecution := false

scalaVersion := "2.11.8"

val SPARK_VERSION = "2.2.0"

val SPRAY_VERSION = "1.3.4"

val HADOOP_VERISON = "2.7.0"

organization := "com.dataValidation"

libraryDependencies +=  "org.apache.spark"  %% "spark-core" % SPARK_VERSION  % "provided"

libraryDependencies += "org.apache.spark"  %% "spark-sql"  % SPARK_VERSION  % "provided"

libraryDependencies += "org.apache.spark" %% "spark-hive" % SPARK_VERSION % "provided"

libraryDependencies +=  "org.apache.spark"  %% "spark-core" % SPARK_VERSION  % "test"

libraryDependencies += "org.apache.spark"  %% "spark-sql"  % SPARK_VERSION  % "test"

libraryDependencies += "org.apache.spark" %% "spark-hive" % SPARK_VERSION % "test"

libraryDependencies += "com.typesafe" % "config" % "1.2.1"

libraryDependencies ++= Seq(
  "io.spray"          %% "spray-json"     % "1.3.4",
  "io.spray"          %% "spray-testkit" % "1.3.1" % "test" excludeAll (ExclusionRule(organization = "javax.servlet"),ExclusionRule(organization = "org.eclipse.jetty.orbit"))
)

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
  case PathList("javax", "pom.properties", xs@_*) => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".properties" => MergeStrategy.concat
  case PathList(ps@_*) if ps.last endsWith ".class" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".RSA" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith "mailcap" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".xml" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".html" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".dtd" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".default" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".xsd" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith ".thrift" => MergeStrategy.first
  case PathList(ps@_*) if ps.last endsWith "public-suffix-list.txt" => MergeStrategy.first

  case "pom.properties" => MergeStrategy.first
  case "unwanted.txt" => MergeStrategy.discard
  case "META-INF/DISCLAIMER" => MergeStrategy.discard
  case x => old(x)
}
}

parallelExecution in Test := false

test in assembly := {}


assemblyJarName in assembly := "data_wf.jar"
