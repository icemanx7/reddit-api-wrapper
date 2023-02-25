ThisBuild / scalaVersion := "2.13.8"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.rthomas"
ThisBuild / organizationName := "rthomas"

lazy val root = (project in file("."))
  .settings(
    name := "reddit-api-wrapper",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.typelevel" %% "cats-core" % "2.9.0",
      "org.typelevel" %% "cats-effect" % "3.4.5",
      "org.http4s" %% "http4s-circe" % "0.23.18",
      "io.circe" %% "circe-generic" % "0.14.3",
      "io.circe" %% "circe-literal" % "0.14.3",
      "org.http4s" %% "http4s-dsl" % "0.23.18",
      "org.http4s" %% "http4s-netty-server" % "0.5.4",
      "org.http4s" %% "http4s-netty-client" % "0.5.4",
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "io.circe" %% "circe-literal" % "0.14.3",
      "org.scalactic" %% "scalactic" % "3.2.15",
      "org.scalatest" %% "scalatest" % "3.2.15" % "test",
      "org.http4s" %% "http4s-client-testkit" % "0.23.18" % Test,
      "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % "test",
      "org.mockito" % "mockito-scala-scalatest_2.13" % "1.17.12",
      "org.scalamock" %% "scalamock" % "5.1.0" % Test,
      "org.scalatest" %% "scalatest" % "3.1.0" % Test,
      "com.github.tomakehurst" % "wiremock-jre8" % "2.35.0" % Test
    ),
    scalacOptions ++= Seq( // use ++= to add to existing options
      "-feature", // then put the next option on a new line for easy editing
      "-language:implicitConversions",
      "-Ywarn-unused:imports",
      "-Wunused"
    ), // for "trailing comma", the closing paren must be on the next line
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
