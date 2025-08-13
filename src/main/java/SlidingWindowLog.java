import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SlidingWindowLog {
    private final int numConcurrentTask = 2;
    private final Duration slidingWindowDuration = Duration.ofSeconds(2);
    private final int numAllowedReqPerWindow = 2;
    private final Queue<LocalDateTime> requestTimeLogs = new LinkedBlockingQueue<>();
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numConcurrentTask);
    private final Lock lock = new ReentrantLock();

    public boolean processRequest(Runnable task){
        lock.lock(); // critical section
        try {
            // log request time anyway
            LocalDateTime reqStartTime = LocalDateTime.now();
            requestTimeLogs.offer(reqStartTime);
            boolean isAllowed = canProcessRequest(reqStartTime);
            if (isAllowed) {
                executor.execute(task);
            }
            return isAllowed;
        } finally {
            lock.unlock();
        }
    }

    private boolean canProcessRequest(LocalDateTime reqStartTime){
        // remove old timestamps
        final LocalDateTime thresholdTime = reqStartTime.minus(slidingWindowDuration);
        requestTimeLogs.removeIf(logTime -> logTime.isBefore(thresholdTime));

        // return true if within window bounds
        return requestTimeLogs.size() <= numAllowedReqPerWindow;
    }

    public static void main(String[] args) throws InterruptedException {
        SlidingWindowLog slidingWindowLog = new SlidingWindowLog();
        int numberOfTasks = 10;
        CountDownLatch latch = new CountDownLatch(numberOfTasks);
        Random random = new Random();

        for (int i = 1; i <= numberOfTasks; i++) {
            final int taskId = i;

            new Thread(() -> {
                boolean res = slidingWindowLog.processRequest(() -> System.out.printf("success-i: %d\n", taskId));
                if (!res) {
                    System.out.printf("failure-i: %d\n", taskId);
                }
                latch.countDown();

            }).start();

            // Calculate a random sleep (as i increase -> increase range for req simulation)
            long sleepDurationMillis = random.nextInt(i * 401);
            try {
                System.out.printf("----- sleep: %d ms -----\n", sleepDurationMillis);
                Thread.sleep(sleepDurationMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        latch.await();
        System.out.println("All threads have finished their tasks.");
    }
}