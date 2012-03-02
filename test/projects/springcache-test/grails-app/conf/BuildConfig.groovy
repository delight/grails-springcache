grails.project.target.dir = "target"
grails.project.work.dir = "target"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
grails.project.dependency.resolution = {
	inherits "global"
	log "warn"
	repositories {
		grailsPlugins()
		grailsHome()
		grailsCentral()
		mavenLocal()
		mavenCentral()
		mavenRepo "http://repository.codehaus.org/"
	}

	def gebVersion = '0.6.2'
	def seleniumVersion = '2.17.0'

	dependencies {
		test "org.seleniumhq.selenium:selenium-firefox-driver:$seleniumVersion"
		test("org.codehaus.groovy.modules.http-builder:http-builder:0.5.1") {
			excludes "groovy", "xml-apis", "commons-logging"
		}
		test "org.codehaus.geb:geb-spock:0.6.0"
	}
	plugins {
		compile ":bean-fields:0.5"
		test ":build-test-data:1.1.2"
		compile ":cache-headers:1.1.5"
		test ":geb:$gebVersion"
		compile ":hibernate:$grailsVersion"
		compile ":rateable:0.7.1"
		compile ":shiro:1.1.3"
		test ":spock:0.6-SNAPSHOT"
		build ":tomcat:$grailsVersion"
		compile ":yui:2.8.2.1"
		compile ":resources:1.1.6"
		runtime ":jquery:1.7.1"
	}
}
grails.plugin.location.springcache = "../../.."
