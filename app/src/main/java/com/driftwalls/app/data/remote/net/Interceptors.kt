package com.driftwalls.app.data.remote.net

import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/** Adds the identifying header the Art Institute of Chicago asks API clients to send. */
class AicUserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!request.url.host.endsWith("artic.edu")) return chain.proceed(request)
        return chain.proceed(request.newBuilder().header("AIC-User-Agent", userAgent).build())
    }
}

/**
 * Network interceptor, so it only throttles requests that actually reach the network
 * (cache hits are free). Hosts without a limiter pass straight through.
 */
class RateLimitInterceptor(private val limiters: Map<String, RateLimiter>) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        limiters[chain.request().url.host]?.acquire()
        return chain.proceed(chain.request())
    }
}

/**
 * Network interceptor that lets OkHttp cache API responses for [maxAgeSeconds] on the listed hosts.
 * Both museum APIs reply with `no-cache`, which would otherwise mean a request every time.
 */
class ApiCacheInterceptor(private val hosts: Set<String>, private val maxAgeSeconds: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (request.method != "GET" || request.url.host !in hosts || !response.isSuccessful) return response
        return response.newBuilder()
            .removeHeader("Pragma")
            .header("Cache-Control", "public, max-age=$maxAgeSeconds")
            .build()
    }
}

/** When the network fails, serves any cached copy (however stale) before giving up. */
class OfflineFallbackInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (request.method != "GET") throw e
            val cached = chain.proceed(request.newBuilder().cacheControl(CacheControl.FORCE_CACHE).build())
            if (cached.isSuccessful) return cached
            cached.close()
            throw e
        }
    }
}
