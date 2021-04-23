package com.kaiy.cloud.alibaba.oauth;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.security.jackson2.CoreJackson2Module;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.provider.token.store.redis.StandardStringSerializationStrategy;
import org.springframework.security.web.jackson2.WebJackson2Module;

/**
 * @author kaiy
 */

public class JsonSerializationStrategy extends StandardStringSerializationStrategy {

    private static final Jackson2JsonRedisSerializer<Object> JACKSON2_JSON_REDIS_SERIALIZER = new Jackson2JsonRedisSerializer<>(Object.class);

    static {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.WRAPPER_ARRAY);
//        mapper.registerModule(new CoreJackson2Module());
//        mapper.registerModule(new WebJackson2Module());
//        mapper.registerModule(new OAuth2ClientJackson2Module());

        JACKSON2_JSON_REDIS_SERIALIZER.setObjectMapper(mapper);
    }

    @Override
    protected <T> T deserializeInternal(byte[] bytes, Class<T> clazz) {
        return (T) JACKSON2_JSON_REDIS_SERIALIZER.deserialize(bytes);
    }

    @Override
    protected byte[] serializeInternal(Object object) {
        return JACKSON2_JSON_REDIS_SERIALIZER.serialize(object);
    }
}
