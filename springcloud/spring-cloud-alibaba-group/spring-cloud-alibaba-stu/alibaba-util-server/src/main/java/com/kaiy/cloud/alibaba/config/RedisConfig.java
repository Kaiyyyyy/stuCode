package com.kaiy.cloud.alibaba.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * @author kaiy
 */
//@Configuration
public class RedisConfig {

//    @Bean
//    @DependsOn("redisConnectionFactory")
    public RedisTemplate<Object,Object> redisTemplate( RedisConnectionFactory connectionFactory){
        RedisTemplate<Object,Object> rt = new RedisTemplate<Object,Object>();
        rt.setConnectionFactory(connectionFactory);

        //序列化和反序列化redis的key和value值
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<Object>(Object.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(mapper);
        rt.setValueSerializer(serializer);
        //使用StringRedisSerializer来序列化和反序列化redis的key值
        rt.setKeySerializer(new StringRedisSerializer());
        rt.setHashKeySerializer(serializer);
        rt.setHashValueSerializer(serializer);
        rt.afterPropertiesSet();
        return rt;
    }
}
