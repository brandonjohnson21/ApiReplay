package bjohnson.apireplay;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static java.time.LocalDateTime.now;

// TODO: Can replace the data list + index with a queue if reverse operation/rewind is not being considered.

public class Replay {
    private Replay() {

    }
    public static Replay getInstance() {
        if (_instance == null)
            _instance = new Replay();
        return _instance;
    }


    private static Replay _instance = new Replay();
    private Status status  = Status.STOPPED;
    private int lastSentIdx=-1;
    private long skipNanoOffset = 0;
    private LocalDateTime localStartTime;
    private LocalDateTime lastTickTime;
    private int speed = 1;
    private List<DataPoint> data;
    private Semaphore ticking = new Semaphore(1);
    public void setData(List<DataPoint> data) {
        this.data = data;
    }
    public List<DataPoint> getData() {
        return this.data;
    }
    public DataPoint getNext() {
        if (lastSentIdx < this.data.size()-1)
            return this.data.get(lastSentIdx+1);
        return null;
    }
    public void playPause() {
        switch (this.status) {
            case COMPLETE:
                break;
            case PAUSED:
                this.resume();
                break;
            case PLAYING:
                this.pause();
                break;
            case STOPPED:
            default:
                this.start();
                break;

        }
    }
    private void resume() {
        ticking.acquireUninterruptibly();
        this.restartAt(lastTickTime);
        this.play();
        ticking.release();
    }
    public void start(int index) {
        ticking.acquireUninterruptibly();
        this.reset();
        this.lastSentIdx=index-1; // start at this index+1
        this.skipNanoOffset=this.data.get(lastSentIdx+1).createdAt; // set the skip offset to be whatever the start time is.
        this.localStartTime = now(ZoneId.of("UTC"));
        this.play();
        ticking.release();
    }
    public void start() {
        this.start(0);
    }
    private void play() {
        if (Main.replayThread != null)
            Main.replayThread.interrupt();
        this.status=Status.PLAYING;
    }
    public void pause() {
        this.status=Status.PAUSED;
        ticking.acquireUninterruptibly();
        this.lastTickTime=now(ZoneId.of("UTC"));
        if (Main.replayThread != null)
            Main.replayThread.interrupt();
        ticking.release();
    }
    private void restartAt(LocalDateTime newStartTime) {
        if (localStartTime != null && newStartTime!=null && !localStartTime.isEqual(newStartTime)) {
            this.skipNanoOffset += ChronoUnit.NANOS.between(localStartTime, newStartTime)*this.speed;
            this.localStartTime = now(ZoneId.of("UTC"));
            this.lastTickTime = null;
        }
    }
    public void stop() {
        this.status=Status.STOPPED;
    }
    private void reset() {
        this.lastSentIdx=-1;
        this.lastTickTime = null;
        this.skipNanoOffset=0;
    }
    public void setSpeed(int multiplier) {
        ticking.acquireUninterruptibly();
        if (multiplier>0) {
            this.speed = multiplier;
            System.out.println("Replay speed set to: " + multiplier);
            if (lastTickTime == null) {
                lastTickTime = localStartTime;
            }
            restartAt(lastTickTime);
            if (Main.replayThread != null)
                Main.replayThread.interrupt();
        }
        ticking.release();
    }
    public Status getStatus() {
        return this.status;
    }
    public void setStatus(Status status) { this.status = status;}
    public void printStatus() {
        System.out.println("Replay status: "+getStatus());
        if (this.getNext()!= null) {
            LocalDateTime compareTime = (this.getStatus()==Status.PLAYING)?now(ZoneId.of("UTC")):lastTickTime;
            long nanos = ((this.getNext().createdAt - this.skipNanoOffset) / this.speed) - ChronoUnit.NANOS.between(localStartTime, compareTime);
            System.out.println("At current speed, next dataset in " + TimeUnit.NANOSECONDS.toMillis(nanos) / 1000.0 + " Seconds");
        }
    }

    public LocalDateTime tick() {
        this.ticking.acquireUninterruptibly();
        if (this.status==Status.PLAYING) {
            LocalDateTime curTime = now(ZoneId.of("UTC"));
            lastTickTime=curTime;
            List<DataPoint> dataset = this.data;
            for (int i=this.lastSentIdx+1;i<dataset.size() && this.getStatus()==Status.PLAYING;i++) {
                DataPoint d = dataset.get(i);
                LocalDateTime sendTime = getSendTime(d);
                if (d.createdAt != null && !sendTime.isAfter(curTime)) {
                    Manipulator.nanoOffsetsToLocalDateTime(d.data,localStartTime,skipNanoOffset,speed);
                    Manipulator.generateID(d.data);
                    // TODO: can add modifier call here for realtime modifications.  Not sure how performant these would be.
                    Main.apiHandler.postData(d,lastSentIdx);
                    lastSentIdx = i;
                }else{
                    break;
                }
            }
            if (lastSentIdx >= this.data.size()-1) {
                this.status = Status.COMPLETE;
            }
        }
        this.ticking.release();
        return lastTickTime;
    }
    public LocalDateTime getSendTime(DataPoint d) {
        return this.localStartTime.plusNanos((d.createdAt-this.skipNanoOffset)/this.speed);
    }
}


