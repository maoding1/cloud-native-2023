package com.example.hello;

import com.alibaba.fastjson.JSONObject;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
public class HelloController {
    /* 为了方便测试，限流策略设为1秒钟2个令牌,正式提交时应改为每秒100个令牌**/
    private final RateLimiter limiter = RateLimiter.create(2.0);
    @RequestMapping("/hello")
    public String hello() {
        JSONObject jsonObject = new JSONObject();
        //令牌桶算法实现流量控制 五百毫秒内每拿到令牌则失败
        boolean tryAcquire = limiter.tryAcquire(500, TimeUnit.MILLISECONDS);
        if (tryAcquire) {
            jsonObject.put("code", 200);
            jsonObject.put("msg", "hello cloud_native!");
        } else {
            jsonObject.put("code", 429);
            jsonObject.put("msg", "too many requests");
        }

        return jsonObject.toJSONString();
    }
}
