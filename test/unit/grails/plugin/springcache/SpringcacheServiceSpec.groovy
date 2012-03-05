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
package grails.plugin.springcache

import grails.spring.BeanBuilder
import grails.test.mixin.TestFor
import net.sf.ehcache.config.CacheConfiguration
import org.springframework.cache.ehcache.EhCacheFactoryBean
import net.sf.ehcache.*
import static net.sf.ehcache.Status.STATUS_UNINITIALISED
import net.sf.ehcache.constructs.blocking.*
import spock.lang.*

@TestFor(SpringcacheService)
@Unroll
class SpringcacheServiceSpec extends Specification {

	CacheManager manager = new CacheManager()
	Ehcache cache1, cache2

	void setup() {
		manager.addCache "cache1"
		manager.addCache "cache2"

		cache1 = manager.getEhcache("cache1")
		cache1.statisticsEnabled = true

		cache2 = manager.getEhcache("cache2")
		cache2.statisticsEnabled = true

        service.grailsApplication = grailsApplication
		service.springcacheCacheManager = manager
	}

	void "calling flush with #flushArgument flushes the correct cache or caches"() {
		given:
		cache1.put(new Element("key", "value"))
		cache2.put(new Element("key", "value"))

		when:
		service.flush(flushArgument)

		then:
		cache1.size == cache1Size
		cache2.size == cache2Size

		where:
		flushArgument        | cache1Size | cache2Size
		"cache1"             | 0          | 1
		["cache1", "cache2"] | 0          | 0
		/cache[\d]/          | 0          | 0
		"cacheX"             | 1          | 1
	}

	void "exceptions on flush are handled"() {
		given:
		cache1.put(new Element("key", "value"))

		and:
		def cache3 = Mock(Ehcache)
		cache3.name >> "cache3"
		cache3.status >> STATUS_UNINITIALISED
		cache3.cacheConfiguration >> new CacheConfiguration(name: 'cache3')
		cache3.flush() >> { new IllegalStateException("this exception would be thrown if cache is not in $Status.STATUS_ALIVE") }
		manager.addCache cache3

		when:
		service.flush(["cache3", "cache1"])

		then:
		cache1.size == 0
	}

	void "flush all flushes everything"() {
		given:
		cache1.put(new Element("key", "value"))
		cache2.put(new Element("key", "value"))

		when:
		service.flushAll()

		then:
		cache1.size == 0
		cache2.size == 0
	}

	void clearStatistics() {
		given:
		for (cache in [cache1, cache2]) {
			cache.get("key") // triggers a miss
			cache.put(new Element("key", "value"))
			cache.get("key") // triggers a hit
		}

		when:
		service.clearStatistics()

		then:
		old(cache1.statistics.cacheHits) == 1L
		old(cache1.statistics.cacheMisses) == 1L
		old(cache2.statistics.cacheHits) == 1L
		old(cache2.statistics.cacheMisses) == 1L

		and:
		cache1.statistics.cacheHits == 0L
		cache1.statistics.cacheMisses == 0L
		cache2.statistics.cacheHits == 0L
		cache2.statistics.cacheMisses == 0L
	}

	void "doWithCache retrieves #value from cache"() {
		given:
		cache1.put(new Element("key", value))

		when:
		def result = service.doWithCache("cache1", "key") {
			fail "Closure should not have been invoked"
		}

		then:
		result == value

		where:
		value << ["value", null]
	}

	void "doWithCache stores value returned by closure if not found in cache"() {
		when:
		def result = service.doWithCache("cache1", "key") {
			return "value"
		}

		then:
		result == "value"

		and:
		cache1.get("key").objectValue == "value"
	}

	void "doWithCache stores value returned by closure if cache element expired"() {
		given:
		def element = new Element("key", "value")
		element.timeToLive = 1
		cache1.put(element)
		while (!element.expired) {
			Thread.sleep 250
		}

		when:
		def result = service.doWithCache("cache1", "key") {
			return "value"
		}

		then:
		result == "value"

		and:
		cache1.get("key").objectValue == "value"
		!cache1.get("key").expired
	}

	void "doWithCache stores null if closure returns null"() {
		when:
		def result = service.doWithCache("cache1", "key") {
			return null
		}

		then:
		result == null

		and:
		cache1.isKeyInCache("key")
		cache1.get("key").objectValue == null
	}

	void "doWithCache throws exception if cache not found and autoCreateCaches is false"() {
		given:
		service.autoCreateCaches = false

		when:
		service.doWithCache("cacheA", "key") {
			fail "Closure should not have been invoked"
		}

		then:
		thrown NoSuchCacheException
	}

	void "doWithCache creates a new cache if cache not found and autoCreateCaches is true"() {
		given:
		def beanBuilder = new BeanBuilder()
		beanBuilder.beans {
			springcacheDefaultCache(EhCacheFactoryBean) { bean ->
				bean."abstract" = true
				cacheManager = manager
			}
		}
		service.applicationContext = beanBuilder.createApplicationContext()
		service.autoCreateCaches = true

		when:
		service.doWithCache("cacheA", "key") {
			return "value"
		}

		then:
		"cacheA" in manager.cacheNames
		manager.getEhcache("cacheA").get("key").objectValue == "value"
	}

	void "doWithBlockingCache does not decorate cache if it is a blocking cache already"() {
		given:
		def blockingCache = new BlockingCache(new Cache("blockingCache", 1, false, false, 1, 0))
		manager.addCache(blockingCache)

		when:
		service.doWithBlockingCache("blockingCache", "key") {
			return "value"
		}

		then:
		manager.getEhcache("blockingCache").is blockingCache
		blockingCache.get("key").objectValue == "value"
	}

	void "doWithBlockingCache decorates cache before using if it is non-blocking"() {
		given:
		manager.addCache("nonBlockingCache")

		when:
		service.doWithBlockingCache("nonBlockingCache", "key") {
			return "value"
		}

		then:
		manager.getEhcache("nonBlockingCache") instanceof BlockingCache
		manager.getEhcache("nonBlockingCache").get("key").objectValue == "value"
	}

	void "doWithBlockingCache clears lock if exception is thrown from closure"() {
		given:
		def blockingCache = new BlockingCache(new Cache("blockingCache", 1, false, false, 1, 0))
		blockingCache.timeoutMillis = 10
		manager.addCache(blockingCache)

		when:
		service.doWithBlockingCache("blockingCache", "key") {
			throw new DeliberateException()
		}

		then:
		thrown DeliberateException

		and:
		// this will time out if service call did not clear the lock
		blockingCache.get("key") == null
	}

	void "doWithBlockingCache does not try to clear lock if it never acquires it"() {
		given:
		def blockingCache = Mock(BlockingCache)
		// simulate lock held on cache by another thread
		blockingCache.name >> "blockingCache"
		blockingCache.status >> STATUS_UNINITIALISED
		blockingCache.cacheConfiguration >> new CacheConfiguration(name: 'cache3')
		blockingCache.get("key") >> { throw new LockTimeoutException() }
		manager.addCache(blockingCache)

		when:
		service.doWithBlockingCache("blockingCache", "key") {
			return "value"
		}

		then:
		thrown LockTimeoutException
	}

	void "doWithCache delegates to doWithBlockingCache if it finds a blocking cache"() {
		given:
		def blockingCache = new BlockingCache(new Cache("blockingCache", 1, false, false, 1, 0))
		blockingCache.timeoutMillis = 10
		manager.addCache(blockingCache)

		when:
		service.doWithCache("blockingCache", "key") {
			throw new DeliberateException()
		}

		then:
		thrown DeliberateException

		and:
		// this will time out if service call did not clear the lock
		blockingCache.get("key") == null
	}

	void "the #methodName method passes through when the plugin is disabled"() {
		given:
		grailsApplication.config.springcache.enabled = false
		service.springcacheCacheManager = null

		when:
		def result = service."$methodName"("cache1", "key") { "value" }

		then:
		result == "value"

		and:
		cache1.statistics.objectCount == 0L
		cache1.statistics.cacheHits == 0L
		cache1.statistics.cacheMisses == 0L

		where:
		methodName << ["doWithCache", "doWithBlockingCache"]
	}

	void "the #methodName method is a no-op when the plugin is disabled"() {
		given:
		grailsApplication.config.springcache.enabled = false
		service.springcacheCacheManager = null

		when:
		service."$methodName"(*arguments)

		then:
		notThrown(Throwable)

		where:
		methodName                 | arguments
		"flushAll"                 | []
		"clearStatistics"          | []
		"flush"                    | ["cache1"]
		"getOrCreateCache"         | ["cache1"]
		"getOrCreateBlockingCache" | ["cache1"]
	}
}

class DeliberateException extends RuntimeException {
	DeliberateException() {
		super("thrown to test exception handling")
	}
}
