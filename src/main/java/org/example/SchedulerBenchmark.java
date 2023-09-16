package org.example;

import org.openjdk.jmh.annotations.*;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Fork(1)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class SchedulerBenchmark {
    @Param({"1000", "5000", "10000"})
    private static int threadCount;
    @Param({"15000", "25000", "36000"})
    private static int requestCount;
    @Param({"1", "2"})
    private static int testOption;
    private static final int useFixedThreadPool = 0;
    private static final int useForkJoinPool = 1;
    private static Thread.Builder builder;
    private static ExecutorService db_executor;

    @Setup(Level.Invocation)
    public void init() {
        if (testOption == useFixedThreadPool) {
            ThreadFactory factory = Thread.ofVirtual().factory();
            builder = Thread.ofVirtual().scheduler(Executors.newFixedThreadPool(threadCount, factory));
        } else {
            builder = Thread.ofVirtual().scheduler(new ForkJoinPool(threadCount));
        }
        db_executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        ConnectionPool.initConnectionPool();
    }

    @TearDown(Level.Invocation)
    public void tearDown() {
        ConnectionPool.closeConnection();
    }

    public static String execQuery(String sql) throws InterruptedException, ExecutionException {
        String queryResult = "";
        try {
            ConnectionNode node;
            do {
                node = ConnectionPool.getConnection();
            } while (node == null);
            ResultSet rs = node.stm.executeQuery(sql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String hello = rs.getString("hello");
                String response = rs.getString("response");

                queryResult += "id: " + id + " hello:" + hello + " response: " + response + "\n";
            }

            rs.close();
            ConnectionPool.releaseConnection(node);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return queryResult;
    }

    public static String submitQuery(String sql) throws InterruptedException, ExecutionException {
        CompletableFuture<String> future = new CompletableFuture<>();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    future.complete(execQuery(sql));
                } catch (Exception e) {

                }
            }
        };
        db_executor.execute(r);

        return future.get();
    }

    @Benchmark
    public void testScheduler() throws Exception {
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(requestCount);
        AtomicLong statsTimes = new AtomicLong();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    startSignal.await();
                    String sql = "select * from hello";
                    String result = submitQuery(sql);
                    doneSignal.countDown();
                } catch (Exception e) {

                }
            }
        };
        ArrayList<Thread> temp = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            Thread now = builder.start(r);
        }

        long before = System.currentTimeMillis();
        statsTimes.set(before);
        startSignal.countDown();
        doneSignal.await();

        db_executor.shutdown();
    }

}
