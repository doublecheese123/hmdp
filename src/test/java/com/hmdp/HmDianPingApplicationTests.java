package com.hmdp;

import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
class HmDianPingApplicationTests {

    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private RedisIdWorker redisIdWorker;

    private static final ExecutorService es = Executors.newFixedThreadPool(500);

    @Test
    void testSaveShop() throws InterruptedException {
//        shopService.saveShop2Redis(1L, 10L);
    }

    @Test
    void testId() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = () -> {
            for (int i = 0; i < 100; i++) {
                long id = redisIdWorker.nextId("order"); // 生成ID
                System.out.println("id = " + id);
            }
            latch.countDown(); // 任务完成，计数-1
        };
        long begin = System.currentTimeMillis(); // 记录任务开始时间
        for (int i = 0; i < 300; i++) {
            es.submit(task); // 把任务提交到线程池（es是ExecutorService线程池对象）
        }
        latch.await(); // 主线程阻塞，直到300个任务都执行完（latch计数减到0）
        long end = System.currentTimeMillis(); // 记录任务结束时间
        System.out.println("time = " + (end - begin)); // 打印总耗时（毫秒）
    }

}
