import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TokenBucket {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int capacity = 3;
    private final AtomicInteger bucket = new AtomicInteger(capacity);
    private static final int refillTokenCnt = 2;
    private static final int refillInitialDelayInSec = 30;
    private static final int refillPeriodInSec = 30;

    // refill 2 tokens every 30 sec
    private void refill(){
        int currentCount;
        int newCount;

        do {
            currentCount = bucket.get();
            if ( currentCount == capacity ) {
                break;
            }
            newCount = Math.min(currentCount + refillTokenCnt, TokenBucket.capacity);

        } while (!bucket.compareAndSet(currentCount, newCount));
    }

    public TokenBucket(){
        scheduler.scheduleAtFixedRate(this::refill, refillInitialDelayInSec, refillPeriodInSec, TimeUnit.SECONDS);
    }

    public boolean processRequest(Runnable reqTask){
        int currentCount;
        int newCount;

        do {
            currentCount = bucket.get();
            if (currentCount <= 0) {
                // The bucket is empty, so we can't process the request.
                return false;
            }
            newCount = currentCount - 1;
        } while (!bucket.compareAndSet(currentCount, newCount));

        reqTask.run();
        return true;
    }

    public static void main(String[] args) {
        TokenBucket tb = new TokenBucket();
        for(int i=1; i<=10; i += 1){
            final String msg = String.format("success, i=%d", i);
            boolean res = tb.processRequest(() -> System.out.println(msg));
            System.out.printf("res=%b, time=%d%n", res, (i-1)*5);

            try{
                Thread.sleep(Duration.ofSeconds(5));
            }
            catch (Exception e){
                System.out.println("Exception caught");
            }
        }

        System.out.println("Exiting main thread!");
    }
}
