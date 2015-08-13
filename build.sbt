name := """GeoPublisher-viewer"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

resolvers += "idgis-public" at "http://nexus.idgis.eu/content/groups/public/"

resolvers += "idgis-restricted" at "http://nexus.idgis.eu/content/groups/restricted/"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs,
  "org.webjars" %% "webjars-play" % "2.4.0-1",
  "org.webjars" % "openlayers" % "3.5.0",
  "org.webjars" % "dojo" % "1.10.4",
  "org.webjars" % "bootstrap" % "3.3.5",
  "org.webjars" % "jquery" % "2.1.4",
  "nl.idgis.geoide" % "geoide-ogc-client" % "0.1.8-SNAPSHOT" exclude ("java3d", "vecmath"),
  "java3d" % "vecmath" % "1.5.2" classifier "",
  "org.jsoup" % "jsoup" % "1.8.3"
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
routesGenerator := InjectedRoutesGenerator

// Compile the project before generating Eclipse files, so that generated .scala or .class files for views and routes are present
EclipseKeys.preTasks := Seq(compile in Compile)