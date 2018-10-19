import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "1.2.71"
}

group = "io.mfj"
version = project.properties["ver"] ?: "9.1-SNAPSHOT" // gradle -Pver= to override

tasks.create("get-version") { println( project.version ) }

val copyrightYear = "2018"
val sourceLocation = "https://github.com/measuresforjustice/textricator"

dependencies {
	compile(kotlin("stdlib-jdk8"))
	testCompile(kotlin("test-junit"))

	compile("io.mfj:expr:4.0.13") // apache 2.0

  // pdfbox
  compile("org.apache.pdfbox:pdfbox:2.0.12") // Apache 2.0
  compile("org.apache.pdfbox:pdfbox-tools:2.0.12") // Apache 2.0

	// itext5
  compile("com.itextpdf:itextpdf:5.5.13") // AGPL
	compile("org.bouncycastle:bcprov-jdk15on:1.50") // MIT

	// itext7
  compile("com.itextpdf:kernel:7.1.4") // AGPL
  compile("com.itextpdf:layout:7.1.4") // AGPL

  // jackson
  compile("com.fasterxml.jackson.core:jackson-databind:2.9.7") // Apache 2.0
  compile("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.9.7") // Apache 2.0
  compile("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.7") // Apache 2.0

  // slf4j
  compile("org.slf4j:slf4j-api:1.7.25") // MIT
  compile("org.slf4j:slf4j-simple:1.7.25") // MIT

  compile("org.apache.commons:commons-csv:1.4") // Apache 2.0

	compile("com.offbytwo:docopt:0.6.0.20150202") // MIT

}

// inject values into version.properties
tasks.withType<ProcessResources> {
	filesMatching("/io/mfj/textricator/version.properties") {
		expand( project.properties
				.plus( "copyrightYear" to copyrightYear )
				.plus( "sourceLocation" to sourceLocation ) )
	}
}

tasks.withType<KotlinCompile>().all {
	kotlinOptions {
		jvmTarget = "1.8"
	}
}

val sourceJar = tasks.create<Jar>("sourcesJar") {
	dependsOn(JavaPlugin.CLASSES_TASK_NAME)
	classifier = "sources"
	from(kotlin.sourceSets["main"].kotlin)
}

artifacts {
	add("archives",sourceJar)
}

tasks.create<Tar>("tgz") {
	baseName = "assembly"
	extension = "tgz"
	compression = Compression.GZIP
	from( project.rootDir ) {
		include( "/README.md", "/NOTICE", "/COPYING" )
	}
	from( "src/scripts" )
	// jar
	from( tasks["jar"].outputs.files ) {
		into( "lib" )
	}
	// dependencies
	from( configurations.runtimeClasspath ) {
		into( "lib" )
	}
}

// do not allow snapshot dependencies when building a release
if ( ! project.version.toString().endsWith("-SNAPSHOT") ) {
	configurations.compile.dependencies.forEach { dep ->
		if ( dep?.version?.endsWith("-SNAPSHOT") == true ) {
			throw Exception( "${dep.group}:${dep.name}:${dep.version} is a snapshot" )
		}
	}
}

// TODO maven local deploy
// TODO maven remote deploy
