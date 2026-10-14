package com.ahrose.railticket.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 浏览器安全的 JSON 序列化配置。 */
@Configuration
public class JacksonConfig {

    /** 将雪花 Long ID 输出为字符串，避免 JavaScript Number 超过安全整数范围后丢失精度。 */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longIdToStringCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance);
    }
}
