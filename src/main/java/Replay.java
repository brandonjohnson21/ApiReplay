import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static java.time.LocalDateTime.now;

// TODO: Can replace the data list + index with a queue if reverse operation/rewind is not being considered.

public class Replay {
    private Replay() {

    }
    private static Replay _instance = new Replay();
    private static Thread ReplayThread;
    private Status status  = Status.STOPPED;
    private LocalDateTime curTime;
    private LocalDateTime start;
//    private LocalDateTime end;
    private int lastSentIdx=-1;
    private long skipNanoOffset = 0;
    private LocalDateTime localStartTime;
    private LocalDateTime lastTickTime;
    private LocalDateTime playbackPauseTime;
    private long pauseTime = 0;
    private int speed = 1;
    private List<DataPoint> data;
    private Semaphore ticking = new Semaphore(1);
    public static Thread getThread() { return ReplayThread; }

    public void setData(List<DataPoint> data) {
        this.data = data;
    }
    public static Replay getInstance() {
        if (_instance == null)
            _instance = new Replay();
        return _instance;
    }
    public LocalDateTime getCurrentTime() {
        if (curTime != null)
            return curTime;
        return LocalDateTime.MIN;
    }

    public void setCurrentTime(LocalDateTime d) {
        curTime = d;
    }

    public LocalDateTime getStartTime() {
        return start;
    }

    public void setStartTime(LocalDateTime start) {
        this.start = start;
    }
//
//    public LocalDateTime getEndTime() {
//        return end;
//    }
//
//    public void setEndTime(LocalDateTime end) {
//        this.end = end;
//    }
    public void playPause() {
        switch (this.status) {
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
    public void start() {
        ticking.acquireUninterruptibly();
        this.reset();
        this.localStartTime = now(ZoneId.of("UTC"));
        this.play();
        ticking.release();
    }
    private void play() {
        this.status=Status.PLAYING;
    }
    public void pause() {
        ticking.acquireUninterruptibly();
        //this.restartAt(lastTickTime);
        this.status=Status.PAUSED;
        ticking.release();
    }
    private void restartAt(LocalDateTime newStartTime) {
        if (localStartTime != null) {
            this.skipNanoOffset += ChronoUnit.NANOS.between(localStartTime, newStartTime);
            this.localStartTime = now(ZoneId.of("UTC"));
            this.lastTickTime = null;
        }
    }
    public void stop() {
        ticking.acquireUninterruptibly();
        this.status=Status.STOPPED;
        ticking.release();
    }
    public void rewind() {
        ticking.acquireUninterruptibly();
        this.reset();
        ticking.release();
    }
    private void reset() {
        this.lastSentIdx=-1;
        this.pauseTime=0;
    }
//    public void rewind(long ms) {
//
//    }
    public void setSpeed(int multiplier) {
        ticking.acquireUninterruptibly();
        if (multiplier>0) {
            this.speed = multiplier;
            System.out.println("Replay speed set to: " + multiplier);
            restartAt(lastTickTime);
        }
        ticking.release();
    }
    public Status getStatus() {
        return this.status;
    }
    public void printStatus() {
        System.out.println("Replay status: "+getStatus());
    }

    public void tick() {
        this.ticking.acquireUninterruptibly();
        if (this.status==Status.PLAYING) {
            LocalDateTime curTime = now(ZoneId.of("UTC"));
            lastTickTime=curTime;
            // current nanos to compare = number of nanoseconds to skip + the current running nanoseconds
            // skipNanoOffset is updated when restarting. it should be reset to the last ticked nanos on speed changes and
            long curNano = ChronoUnit.NANOS.between(localStartTime,curTime);
            List<DataPoint> dataset = this.data;
            for (int i=this.lastSentIdx+1;i<dataset.size();i++) {
                    DataPoint d = dataset.get(i);
                    if (d.createdAt != null && (d.createdAt-skipNanoOffset)/this.speed <= curNano) {
                        Manipulator.nanoOffsetsToLocalDateTime(d.data,localStartTime,skipNanoOffset,speed);
                        Manipulator.generateID(d.data);
                        // TODO: can add modifier call here for realtime modifications.  Not sure how performant these would be.
                        try {
                            Main.apiHandler.postData(d);
                            lastSentIdx = i;
                        } catch (IOException | InterruptedException e) {
                            System.err.println("Cannot post status message to endpoint: "+d.endpoint);
                            this.status=Status.STOPPED;
                            e.printStackTrace();
                            System.exit(-1);
                        }
                    }else{
                        break;
                    }
                }
            }
        if (lastSentIdx >= this.data.size()-1) {
            this.status = Status.COMPLETE;
        }
        this.ticking.release();
        }

    }


