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