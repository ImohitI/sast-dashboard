package dev.sast.dashboard;

import dev.sast.dashboard.Views.BuildView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache-aside for the latest build of a repo.
 *
 *   GET latest:{repoId} -> HIT: return it
 *                       -> MISS: query Postgres, SET latest:{repoId} EX ttl, return it
 *
 * Freshness: the consumer DELs the key after committing a new build; the TTL bounds the race where a reader
 * re-caches the old build just after that DEL. Redis is optional: any Redis error is logged and we read Postgres,
 * so an outage costs speed, never availability (experiment E5).
 */
@Service
public class LatestBuildService {

    private static final Logger log = LoggerFactory.getLogger(LatestBuildService.class);

    private final ScanRepository repository;
    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final Duration ttl;

    public LatestBuildService(ScanRepository repository, StringRedisTemplate redis, JsonMapper json,
                              @Value("${sast.cache.ttl}") Duration ttl) {
        this.repository = repository;
        this.redis = redis;
        this.json = json;
        this.ttl = ttl;
    }

    /** Must match dev.sast.contracts.CacheKeys#latest in sast-platform: the consumer deletes this key. */
    static String key(String repoId) {
        return "latest:" + repoId;
    }

    public Optional<BuildView> latest(String repoId) {
        String key = key(repoId);
        String cached = readCache(key);
        if (cached != null) {
            log.info("cache HIT  {}", key);
            return Optional.of(json.readValue(cached, BuildView.class));
        }
        log.info("cache MISS {}", key);
        Optional<BuildView> build = repository.latestBuildId(repoId).flatMap(id -> repository.build(repoId, id));
        build.ifPresent(view -> writeCache(key, json.writeValueAsString(view)));
        return build;
    }

    private String readCache(String key) {
        try {
            return redis.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.warn("redis unavailable, reading Postgres: {}", e.toString());
            return null;
        }
    }

    private void writeCache(String key, String value) {
        try {
            redis.opsForValue().set(key, value, ttl);
        } catch (RuntimeException e) {
            log.warn("redis unavailable, not caching {}: {}", key, e.toString());
        }
    }
}
