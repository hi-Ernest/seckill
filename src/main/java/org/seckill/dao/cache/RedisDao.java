package org.seckill.dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.seckill.entity.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisDao {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JedisPool jedisPool;

    public RedisDao(String ip, int port) {
        this.jedisPool = new JedisPool(ip, port);
    }

    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);

    public Seckill getSeckill(long seckillId) {
        //redis操作
        try {
            //数据库连接池
            Jedis jedis = jedisPool.getResource();

            try {
                String key = "seckill:"+seckillId;
                //并没有实现内部序列化操作
                //get——> btye[]——> 反序列化——> object(Seckill)
                //采用自定义的序列化
                //protostuff:pojo
                byte[] bytes = jedis.get(key.getBytes());
                //缓存中获取
                if (bytes != null) {
                    //空对象
                    Seckill seckill = schema.newMessage();
                    ProtobufIOUtil.mergeFrom(bytes, seckill, schema);

                    //seckill 反序列化
                    return seckill;
                }
            }finally {
                jedis.close();
            }

        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }


    public String putSeckill(Seckill seckill) {
        //set object(Seckill)——> 序列化——> btye[]
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = "seckill:"+seckill.getSeckillId();
                byte[] bytes = ProtobufIOUtil.toByteArray(seckill, schema,
                        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));

                //超时缓存
                int timeout = 60 * 60;

                //if success return ok
                String result = jedis.setex(key.getBytes(), timeout, bytes);
                return result;

            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return null;
    }
}
