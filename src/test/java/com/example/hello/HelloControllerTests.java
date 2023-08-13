package com.example.hello;

import com.google.common.util.concurrent.RateLimiter;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SpringBootTest
public class HelloControllerTests {

    @Mock
    private RateLimiter rateLimiter;

    @InjectMocks
    private HelloController helloController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testHelloWithTokenAcquired() throws JSONException {
        // Mock the rateLimiter.tryAcquire() method to return true
        when(rateLimiter.tryAcquire(500, TimeUnit.MILLISECONDS)).thenReturn(true);

        // Call the hello() method
        helloController.SetRateLimiter(rateLimiter);
        String response = helloController.hello();

        // Verify the response
        JSONObject jsonObject = new JSONObject(response);
        assertEquals(200, jsonObject.getInt("code"));
        assertEquals("hello cloud_native!", jsonObject.getString("msg"));
    }

    @Test
    public void testHelloWithTokenNotAcquired() throws JSONException {
        // Mock the rateLimiter.tryAcquire() method to return false
        when(rateLimiter.tryAcquire(500, TimeUnit.MILLISECONDS)).thenReturn(false);

        // Call the hello() method
        helloController.SetRateLimiter(rateLimiter);
        String response = helloController.hello();

        // Verify the response
        JSONObject jsonObject = new JSONObject(response);
        assertEquals(429, jsonObject.getInt("code"));
        assertEquals("too many requests", jsonObject.getString("msg"));
    }
}