package org.terrakube.executor.service.logs;

import lombok.Builder;
import lombok.Setter;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@RedisHash("logs")
@Getter
@Setter
@Builder
public class Logs {
    
    @Id
    private String id;
    @Indexed
    private Integer jobId;
    private String output;
    @TimeToLive
    private Long ttl;
}
