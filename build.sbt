name := "gnib"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies += "com.softwaremill.sttp"        %% "core"              % "1.5.11"
libraryDependencies += "com.softwaremill.sttp"        %% "akka-http-backend" % "1.5.12"
libraryDependencies += "com.typesafe.akka"            %% "akka-stream"       % "2.5.19"
libraryDependencies += "io.circe"                     %% "circe-optics"      % "0.11.0"
libraryDependencies += "io.circe"                     %% "circe-parser"      % "0.11.0"
libraryDependencies += "com.typesafe.scala-logging"   %% "scala-logging"     % "3.9.0"
libraryDependencies += "ch.qos.logback"                % "logback-classic"   % "1.2.3"

enablePlugins(JavaAppPackaging)

import com.typesafe.sbt.SbtNativePackager.packageArchetype
// we specify the name for our fat jar
assemblyJarName in assembly := "assembly-project.jar"

// using the java server for this application. java_application is fine, too
packageArchetype.java_server

// removes all jar mappings in universal and appends the fat jar
mappings in Universal := {
  // universalMappings: Seq[(File,String)]
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  // removing means filtering
  val filtered = universalMappings filter {
    case (file, name) =>  ! name.endsWith(".jar")
  }
  // add the fat jar
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}


// the bash scripts classpath only needs the fat jar
scriptClasspath := Seq( (assemblyJarName in assembly).value )