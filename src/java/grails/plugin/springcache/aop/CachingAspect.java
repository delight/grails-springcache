/*
 * Copyright 2009 Rob Fletcher
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
package grails.plugin.springcache.aop;

import grails.plugin.springcache.CacheKey;
import grails.plugin.springcache.annotations.Cacheable;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.ObjectUtils.NULL;

@Aspect
public class CachingAspect {

	private final Logger log = LoggerFactory.getLogger(CachingAspect.class);
	
	private CacheManager cacheManager;

	@Around("@annotation(cacheable)")
	public Object invokeCachedMethod(ProceedingJoinPoint pjp, Cacheable cacheable) throws Throwable {
		if (log.isDebugEnabled()) log.debug(String.format("Intercepted %s", pjp.toLongString()));
		Ehcache cache = cacheManager.getEhcache(cacheable.value());
		CacheKey key = CacheKey.generate(pjp);
		return getFromCacheOrInvoke(pjp, cache, key);
	}

	Object getFromCacheOrInvoke(ProceedingJoinPoint pjp, Ehcache cache, CacheKey key) throws Throwable {
		Object value = null;
		Element element = cache.get(key);
		if (element == null || element.isExpired()) {
			if (log.isDebugEnabled()) log.debug(String.format("Cache miss for %s", key.toString()));
			value = pjp.proceed();
			cache.put(new Element(key, value == null ? NULL : value));
		} else {
			if (log.isDebugEnabled()) log.debug(String.format("Cache hit for %s", key.toString()));
			value = element.getValue();
		}
		return value == NULL ? null : value;
	}

	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}
}