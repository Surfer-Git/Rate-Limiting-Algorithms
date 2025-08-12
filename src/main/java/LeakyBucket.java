import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.*;

public class LeakyBucket {

    private final int capacity = 3;
    private final int numTasksPerUnitTime = 2;

    private final Queue<Runnable> bucket = new ArrayBlockingQueue<>(capacity);
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(numTasksPerUnitTime);

    public boolean addTask(Runnable task){
        return bucket.offer(task);
    }

    public LeakyBucket(){
        // Leaking rate: 2 req in 10 seconds
        scheduler.scheduleAtFixedRate(this::processTask, 5, 10, TimeUnit.SECONDS);
    }

    private void processTask(){
        int cntTask = 0;
        while(!bucket.isEmpty() && cntTask < numTasksPerUnitTime){
            executor.execute(bucket.poll());
            cntTask += 1;
        }
    }

    public static void main(String[] args) {
        LeakyBucket userBucket = new LeakyBucket();
        for (int i=1; i <= 10; i++){
            int taskId = i;
            int time = 2 * (i-1);
            boolean isSuccess = userBucket.addTask(() -> System.out.printf("process-i: %d, time: %d \n", taskId, time));
            if(!isSuccess) {
                System.out.printf("Fail-i: %d \n", taskId);
            }

            try{
                Thread.sleep(Duration.ofSeconds(2));
            }
            catch (Exception e){}
        }
    }
}