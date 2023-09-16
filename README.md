# 使用JMH对比不同调度器在常见应用场景下的性能表现
## 研究内容
编写JMH测试用例，在常见应用场景下（将mysql的同步操作提交到独立线程池，让协程异步等待独
立线程池执行完毕 ，可以利用CompletableFuture实现），对比不同调度器（FixedThreadPool，
ForkJoinPool）的性能表现。

## 对调度器的简单分析

**FixedThreadPool:** 拥有固定的线程/协程数量，当所需线程/协程数量超过所拥有的数目时，就会把任务存入队列中，当出现空闲时则会从该队列中取新的任务，从而实现调度。

**ForkJoinPool:** 该调度器的核心思想是分治法，并通过采取工作窃取算法来进行实现。对于池中的每一个线程/协程都有独立的队列，当队列为空时就会去其它的队列中去寻找任务，从而避免了工作线程由于拆分了任务之后的join等待过程，以提高效率。

## 火焰图
使用AsyncProfiler实现了对JVM运行时函数调用的追踪，并初步尝试对样例代码`mysql_sync_stress_demo`使用协程时进行了监测
[mysql-demo-flameGraph](./flameGraph/fl0.html)
