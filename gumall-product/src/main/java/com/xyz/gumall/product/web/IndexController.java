package com.xyz.gumall.product.web;

import com.xyz.gumall.product.entity.CategoryEntity;
import com.xyz.gumall.product.service.CategoryService;
import com.xyz.gumall.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;
    @Autowired
    RedissonClient redisson;
    @Autowired
    StringRedisTemplate redisTemplate;
    @GetMapping({"/","index.html"})
    public String indexPage(Model model){
        System.out.println(""+Thread.currentThread().getId());
        //1. 查询出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();
    //  classpath:/templates/
        model.addAttribute("categorys",categoryEntities);
        return "index";
    }

//    index/catalog.json
    @ResponseBody
    @GetMapping("/index/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        Map<String, List<Catelog2Vo>> catalogJson = categoryService.getCatalogJson();
        return catalogJson;
    }

    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        //1.获取一把锁，只要名字一样，就是同一把锁
        RLock lock = redisson.getLock("my-lock");
        //2.加锁和解锁
//        lock.lock();//鎖默認30秒
        lock.lock(10, TimeUnit.SECONDS);
        try {
            System.out.println("加鎖成功，執行業務.."+Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("釋放鎖.."+Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }

//    写+读 读等写锁释放
//    写+写 阻塞方式
//    读+写 写等读锁释放
//    读+读 相当无锁，只会在redis中记录好，记录当前的读锁
    @GetMapping("/write")
    @ResponseBody
    public String writeValue(){
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        String s = "";
        //加寫鎖
        RLock rLock = lock.writeLock();
        try {
            rLock.lock();
            System.out.println("寫鎖加鎖成功，執行業務.."+Thread.currentThread().getId());
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue", s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
            System.out.println("寫釋放鎖.."+Thread.currentThread().getId());
        }
        return s;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue(){
        RReadWriteLock lock = redisson.getReadWriteLock("rw-lock");
        String s = "";
        //加讀鎖
        RLock rLock = lock.readLock();
        rLock.lock();
        try {
            System.out.println("讀鎖加鎖成功，執行業務.."+Thread.currentThread().getId());
            s = redisTemplate.opsForValue().get("writeValue");
            Thread.sleep(30000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rLock.unlock();
            System.out.println("讀釋放鎖.."+Thread.currentThread().getId());
        }
        return s;
    }

    @GetMapping("/park")
    @ResponseBody
    public String park() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
//        park.acquire();
        boolean b = park.tryAcquire();
        if (b) {
            //執行業務
        } else {
            return "error";
        }
        return "ok=>"+b;
    }

    @GetMapping("/go")
    @ResponseBody
    public String go() throws InterruptedException {
        RSemaphore park = redisson.getSemaphore("park");
        park.release();
        return "ok";
    }

    @GetMapping("/lockDoor")
    @ResponseBody
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.trySetCount(5);
        door.await();
        return "放假..";
    }

    @GetMapping("/gogogo/{id}")
    @ResponseBody
    public String gogogo(@PathVariable("id") Long id) throws InterruptedException {
        RCountDownLatch door = redisson.getCountDownLatch("door");
        door.countDown();
        return id+"班的人都走了..";
    }
}
