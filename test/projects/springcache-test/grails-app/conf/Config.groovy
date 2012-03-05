import auth.User
import org.apache.shiro.SecurityUtils

// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if(System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }
grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = true
grails.mime.types = [html: ['text/html', 'application/xhtml+xml'],
		xml: ['text/xml', 'application/xml'],
		text: 'text/plain',
		js: 'text/javascript',
		rss: 'application/rss+xml',
		atom: 'application/atom+xml',
		css: 'text/css',
		csv: 'text/csv',
		all: '*/*',
		json: ['application/json', 'text/json'],
		form: 'application/x-www-form-urlencoded',
		multipartForm: 'multipart/form-data'
]
// The default codec used to encode data with ${}
grails.views.default.codec = "none" // none, html, base64
grails.views.gsp.encoding = "UTF-8"
grails.converters.encoding = "UTF-8"
// enable Sitemesh preprocessing of GSP pages
grails.views.gsp.sitemesh.preprocess = true
// scaffolding templates configuration
grails.scaffolding.templates.domainSuffix = 'Instance'

// Set to false to use the new Grails 1.2 JSONBuilder in the render method
grails.json.legacy.builder = false
// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true
// whether to install the java.util.logging bridge for sl4j. Disable fo AppEngine!
grails.logging.jul.usebridge = true
// packages to include in Spring bean scanning
grails.spring.bean.packages = []

grails.app.context = "/"
grails.sitemesh.default.layout = "application"

// set per-environment serverURL stem for creating absolute links
environments {
	production {
		grails.serverURL = "http://www.changeme.com"
	}
}

// log4j configuration
log4j = {
	// Example of changing the log pattern for the default console
	// appender:
	//
	//appenders {
	//    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
	//}

	error 'org.codehaus.groovy.grails.web.servlet',  //  controllers
			'org.codehaus.groovy.grails.web.pages', //  GSP
			'org.codehaus.groovy.grails.web.sitemesh', //  layouts
			'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
			'org.codehaus.groovy.grails.web.mapping', // URL mapping
			'org.codehaus.groovy.grails.commons', // core / classloading
			'org.codehaus.groovy.grails.plugins', // plugins
			'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
			'org.springframework',
			'org.hibernate',
			'net.sf.ehcache.hibernate'

	warn 'org.mortbay.log'

	info 'net.sf.ehcache.constructs.web.filter'

	debug 'grails.plugin.springcache'

}
//log4j.logger.org.springframework.security='off,stdout'
//log4j.logger.org.springframework.security='off,stdout'

springcache {
	defaults {
//		statisticsEnabled = true
		blocking = true
		eternal = true
		memoryStoreEvictionPolicy = "LFU"
		timeToLive = 86400
	}
	caches {
		albumControllerCache
		artistControllerCache
		userControllerCache
		latestControllerCache
		popularControllerCache
		layoutsCache
		configuredCache {
			timeToLive = 86400
		}
		eternalCache
		resourcesControllerCache
	}
}

grails.rateable.rater.evaluator = {
	def principal = SecurityUtils.subject?.principal
	if (principal) {
		User.findByUsername(principal)
	} else {
		null
	}
}
