/*
 * Copyright 2010 Rob Fletcher
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.plugin.springcache.web

import grails.test.mixin.web.ControllerUnitTestMixin
import grails.plugin.springcache.*
import grails.plugin.springcache.annotations.*
import grails.plugin.springcache.web.key.*
import grails.test.mixin.*
import static org.hamcrest.CoreMatchers.instanceOf
import spock.lang.*
import static spock.util.matcher.HamcrestSupport.that

@TestMixin(ControllerUnitTestMixin)
@Mock([CachedTestController, UncachedTestController, RestfulTestController, FlushingTestController])
class FilterContextSpec extends Specification {

	void setup() {
		// set up a spring context with a cacheResolver
		grailsApplication.mainContext.registerMockBean("springcacheDefaultCacheResolver", new DefaultCacheResolver())
	}

	@Unroll
	void "shouldCache returns #shouldCache when controller is '#controllerName' and action is '#actionName'"() {
		given: "there is a request context"
		webRequest.controllerName = controllerName
		webRequest.actionName = actionName
		def context = new FilterContext()

		expect:
		context.shouldCache() == shouldCache

		where:
		controllerName | actionName | shouldCache
		null           | null       | false
		"uncachedTest" | null       | false
		"uncachedTest" | "index"    | false
		"cachedTest"   | "list1"    | true
		"cachedTest"   | "list2"    | true
		"cachedTest"   | "list3"    | true
		"cachedTest"   | null       | true
	    "cachedTest"   | "blah"     | true
	}

	@Unroll
	void "shouldFlush returns #shouldFlush when controller is '#controllerName' and action is '#actionName'"() {
		given: "there is a request context"
		webRequest.controllerName = controllerName
		webRequest.actionName = actionName
		def context = new FilterContext()

		expect:
		context.shouldFlush() == shouldFlush

		where:
		controllerName | actionName | shouldFlush
		null           | null       | false
		"cachedTest"   | null       | false
		"cachedTest"   | "index"    | false
		"flushingTest" | null       | true
		"flushingTest" | "update1"  | true
		"flushingTest" | "update2"  | true
	}

	@Unroll
	void "cache name is '#expectedCacheName' when controller is '#controllerName' and action is '#actionName'"() {
		given: "there is a request context"
		webRequest.controllerName = controllerName
		webRequest.actionName = actionName
		def context = new FilterContext()

		expect:
		context.cacheName == expectedCacheName

		where:
		controllerName | actionName | expectedCacheName
		"cachedTest"   | "index"    | "testControllerCache"
		"cachedTest"   | "list1"    | "listActionCache"
		"cachedTest"   | "list2"    | "listActionCache"
		"cachedTest"   | "list3"    | "listActionCache"
		"cachedTest"   | null       | "testControllerCache"
	    "cachedTest"   | "blah"     | "testControllerCache"
	}

	@Unroll
	void "cannot get cache name when controller is '#controllerName' and action is '#actionName'"() {
		given: "a request for a non-caching action"
		webRequest.controllerName = controllerName
		webRequest.actionName = actionName
		def context = new FilterContext()

		when:
		context.getCacheName()

		then:
		thrown(IllegalStateException)

		where:
		controllerName | actionName
		null           | null
		"uncachedTest" | null
		"uncachedTest" | "index"
		"flushingTest" | null       
		"flushingTest" | "update1"
		"flushingTest" | "update2"       
	}

	@Unroll
	void "cache names are #expectedCacheNames when controller is '#controllerName' and action is '#actionName'"() {
		given: "there is a request context"
		webRequest.controllerName = controllerName
		webRequest.actionName = actionName
		def context = new FilterContext()

		expect:
		context.cacheNames == expectedCacheNames

		where:
		controllerName | actionName | expectedCacheNames
		"flushingTest" | null       | ["testControllerCache"]
		"flushingTest" | "update1"  | ["testControllerCache"]
		"flushingTest" | "update2"  | ["testControllerCache", "listActionCache"]
		"flushingTest" | "update3"  | ["testControllerCache", "listActionCache"]
	}

	@Unroll
	void "cannot get cache names when controller is '#controllerName' and action is '#actionName'"() {
		given: "a request for a non-flushing action"
		webRequest.controllerName = controllerName
		webRequest.actionName = actionName
		def context = new FilterContext()

		when:
		context.getCacheNames()

		then:
		thrown(IllegalStateException)

		where:
		controllerName | actionName
		null           | null
		"uncachedTest" | null
		"uncachedTest" | "index"
		"cachedTest"   | null
		"cachedTest"   | "index"
		"cachedTest"   | "list1"
	}

	void "the cache name is identified via the cache resolver specified by the annotation"() {
		given: "a cache resolver bean"
		def mockCacheResolver = Mock(CacheResolver)
		grailsApplication.mainContext.registerMockBean("mockCacheResolver", mockCacheResolver)
		mockCacheResolver.resolveCacheName("listActionCache") >> { String name -> name.reverse() }

		and: "a request context"
		webRequest.controllerName = "cachedTest"
		webRequest.actionName = "list4"
		def context = new FilterContext()

		expect:
		context.cacheName == "ehcaCnoitcAtsil"
	}

	@Unroll
	void "key generator is #keyGeneratorMatcher when controller is '#controllerName' and action is '#actionName'"() {
		given: "there is a request context"
		webRequest.controllerName = controllerName
		webRequest.actionName = actionName
		def context = new FilterContext()
		
		and: "a key generator bean registered in the spring context"
		grailsApplication.mainContext.registerMockBean("springcacheDefaultKeyGenerator", new DefaultKeyGenerator())
		grailsApplication.mainContext.registerMockBean("alternateKeyGenerator", new WebContentKeyGenerator())

		expect:
		that context.keyGenerator, keyGeneratorMatcher

		where:
		controllerName | actionName | keyGeneratorMatcher
		"cachedTest"   | "index"    | instanceOf(DefaultKeyGenerator)
		"cachedTest"   | "list1"    | instanceOf(DefaultKeyGenerator)
		"cachedTest"   | "list2"    | instanceOf(DefaultKeyGenerator)
		"cachedTest"   | "list3"    | instanceOf(WebContentKeyGenerator)
		"cachedTest"   | null       | instanceOf(DefaultKeyGenerator)
	    "cachedTest"   | "blah"     | instanceOf(DefaultKeyGenerator)
		"restfulTest"  | "list"     | instanceOf(WebContentKeyGenerator)
	}
	
	@Unroll
	void "cannot get key generator when controller is '#controllerName' and action is '#actionName'"() {
		given: "a request for a non-flushing action"
		webRequest.controllerName = controllerName
		webRequest.actionName = actionName
		def context = new FilterContext()

		when:
		context.getKeyGenerator()

		then:
		thrown(IllegalStateException)

		where:
		controllerName | actionName
		null           | null
		"uncachedTest" | null
		"uncachedTest" | "index"
		"flushingTest" | null
		"flushingTest" | "update1"
		"flushingTest" | "update2"
	}

}

@Cacheable("testControllerCache")
class CachedTestController {

	def index = {}

	@Cacheable("listActionCache")
	def list1 = {}

	@Cacheable(cache = "listActionCache")
	def list2 = {}

	@Cacheable(cache = "listActionCache", keyGenerator = "alternateKeyGenerator")
	def list3 = {}

	@Cacheable(cache = "listActionCache", cacheResolver = "mockCacheResolver")
	def list4 = {}
}

class UncachedTestController {

	def index = {}

}

@Cacheable(cache = "testControllerCache", keyGenerator = "alternateKeyGenerator")
class RestfulTestController {

	def list = {}

}

@CacheFlush("testControllerCache")
class FlushingTestController {

	def update1 = {}

	@CacheFlush(["testControllerCache", "listActionCache"])
	def update2 = {}

	@CacheFlush(caches = ["testControllerCache", "listActionCache"])
	def update3 = {}
}