package dev.sast.dashboard;

import dev.sast.dashboard.Views.BuildView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LatestBuildServiceTest {

    private final ScanRepository repository = mock(ScanRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final JsonMapper json = JsonMapper.builder().build();
    private final LatestBuildService service = new LatestBuildService(repository, redis, json, Duration.ofSeconds(60));

    private final BuildView build = new BuildView("samples", 7, "abc", Instant.parse("2026-10-26T18:00:00Z"), 0,
            Map.of("HIGH", 0), List.of());

    @BeforeEach
    void redisValues() {
        when(redis.opsForValue()).thenReturn(values);
    }

    @Test
    void hitIsServedFromRedisWithoutTouchingPostgres() {
        when(values.get("latest:samples")).thenReturn(json.writeValueAsString(build));

        assertThat(service.latest("samples")).contains(build);
        verifyNoInteractions(repository);
    }

    @Test
    void missReadsPostgresAndCachesWithTtl() {
        when(values.get("latest:samples")).thenReturn(null);
        when(repository.latestBuildId("samples")).thenReturn(Optional.of(7L));
        when(repository.build("samples", 7)).thenReturn(Optional.of(build));

        assertThat(service.latest("samples")).contains(build);
        verify(values).set("latest:samples", json.writeValueAsString(build), Duration.ofSeconds(60));
    }

    @Test
    void redisOutageFallsBackToPostgres() {
        when(values.get(anyString())).thenThrow(new RedisConnectionFailureException("redis is down"));
        when(repository.latestBuildId("samples")).thenReturn(Optional.of(7L));
        when(repository.build("samples", 7)).thenReturn(Optional.of(build));

        assertThat(service.latest("samples")).contains(build);
    }
}
