# 使用JMH对比不同调度器在常见应用场景下的性能表现
## 研究内容
编写JMH测试用例，在常见应用场景下（将mysql的同步操作提交到独立线程池，让协程异步等待独
立线程池执行完毕 ，可以利用CompletableFuture实现），对比不同调度器（FixedThreadPool，
ForkJoinPool）的性能表现。