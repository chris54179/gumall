package com.xyz.gumall.search.thread;

import java.util.concurrent.*;

public class ThreadTest {
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main...start");
//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            System.out.println("當前線程:" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("結果:" + i);
//        }, executor);

//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("當前線程:" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("結果:" + i);
//            return i;
//        }, executor);
//        System.out.println("main...end"+future.get());

//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("當前線程:" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("結果:" + i);
//            return i;
//        }, executor).whenComplete((res,exception)->{
//            System.out.println("result="+res+"  exception="+exception);
//        }).exceptionally(throwable -> {
//            return 10;
//        });
//        Integer integer = future.get();
//        System.out.println("main...end"+integer);

//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("當前線程:" + Thread.currentThread().getId());
//            int i = 10 / 4;
//            System.out.println("結果:" + i);
//            return i;
//        }, executor).handle((res, thr)->{
//            if (res != null) {
//                return res*2;
//            }
//            if (thr != null) {
//                return 0;
//            }
//            return 0;
//        });
//        Integer integer = future.get();
//        System.out.println("main...end"+integer);

//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("當前線程:" + Thread.currentThread().getId());
//            int i = 10 / 4;
//            System.out.println("結果:" + i);
//            return i;
//        }, executor).thenRunAsync(()->{
//            System.out.println("任務2啟動...");
//        });
//        System.out.println("main...end");

//        CompletableFuture.supplyAsync(() -> {
//            System.out.println("當前線程:" + Thread.currentThread().getId());
//            int i = 10 / 4;
//            System.out.println("結果:" + i);
//            return i;
//        }, executor).thenAcceptAsync(res->{
//            System.out.println("任務2啟動..."+res);
//        });
//        System.out.println("main...end");

//        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("當前線程:" + Thread.currentThread().getId());
//            int i = 10 / 4;
//            System.out.println("結果:" + i);
//            return i;
//        }, executor).thenApplyAsync(res -> {
//            System.out.println("任務2啟動..." + res);
//            return "" + res;
//        });
//        System.out.println("main...end"+future.get());

//        CompletableFuture<Object> future01 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("future01線程:" + Thread.currentThread().getId());
//            int i = 10 / 4;
//            System.out.println("future01結果:" + i);
//            return i;
//        }, executor);
//
//        CompletableFuture<Object> future02 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("future02線程:" + Thread.currentThread().getId());
//            try {
//                Thread.sleep(3000);
//                System.out.println("future02結果:");
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            return "Hello";
//        }, executor);

//        future01.runAfterBothAsync(future02, ()->{
//            System.out.println("future03 start");
//        },executor);

//        future01.thenAcceptBothAsync(future02, (f1, f2)->{
//            System.out.println("future03 start f1=" + f1 + " f2=" + f2);
//        } ,executor);

//        CompletableFuture<String> future = future01.thenCombineAsync(future02, (f1, f2) -> {
//            return "f1=" + f1 + " f2=" + f2;
//        }, executor);

//        future01.runAfterEitherAsync(future02, ()->{
//            System.out.println("future03 start");
//        },executor);
//        future01.acceptEitherAsync(future02, (res)->{
//            System.out.println("future03 start");
//        },executor);
//        CompletableFuture<String> future = future01.applyToEitherAsync(future02, res -> {
//            System.out.println("future03 start");
//            return res.toString();
//        }, executor);

        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查詢圖片信息");
            return "hello.jpg";
        },executor);

        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查詢圖片attr");
            return "black+256g";
        },executor);

        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(3000);
                System.out.println("查詢圖片desc");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "huawei";
        },executor);

        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);
//        CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
//        allOf.get();
//        System.out.println("main...end ");
//        System.out.println("main...end "+futureImg.get()+"=>"+futureAttr.get()+"=>"+futureDesc.get());
        System.out.println("main...end "+anyOf.get());
    }

    public static void thread(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main...start");
//        1.Thread
//        Thread01 thread01 = new Thread01();
//        thread01.start();
//        System.out.println("main...end");

//        2.Runnable
//        Runable01 runable01 = new Runable01();
//        new Thread(runable01).start();

//        3.Callable
//        FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
//        new Thread(futureTask).start();
//        Integer integer = futureTask.get();
//        System.out.println("main...end");

//        4.Executors
//        service.execute(new Runable01());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());
        System.out.println("main...end");

    }

    public static class Thread01 extends Thread{
        @Override
        public void run() {
            System.out.println("當前線程:"+Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("結果:"+i);
        }
    }

    public static class Runable01 implements Runnable{
        @Override
        public void run() {
            System.out.println("當前線程:"+Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("結果:"+i);
        }
    }

    public static class Callable01 implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("當前線程:"+Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("結果:"+i);
            return i;
        }
    }
}
