package bjohnson.apireplay;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.time.LocalDateTime.now;

public class Main {
    public static ApiHandler apiHandler;
    public static Thread replayThread;
    public static Logger Log = Logger.getLogger("ApiReplay");
    public static void main(String[] args) throws IOException, InterruptedException {
        Path queryDir;
        List<String> validArguments = List.of(
                "-h", // Api host Address
                "-u", // username
                "-p", // password
                "-a", // auth string
                "-d", // query directory
                "-s", // playback speed
                "-t", // thread count
                "--startAtSystem", // system search/begin
                "--startAtIndex", // index start
                "-?" // help
        );
        HashMap<String, String> arguments = new HashMap<>();
        Map<String, String> env = System.getenv();

        for (int i =0; i<args.length; i+=2){
            if (!args[i].equals("-?") && !args[i].equals("?"))
                arguments.put(args[i],args[i+1]);
            if (!validArguments.contains(args[i])) {
                throw new IllegalArgumentException("Invalid argument: "+args[i]);
            }
        }
        if (arguments.containsKey("-?") || args.length == 0) {
            System.out.println("replay  -h hostAddress -d queryFileDirectory [-u username -p password] [-a authString] [-s playback_speed] [-t threads] [--startAtSystem system] [--startAtIndex index] [-?]\n" +
                               "    hostAddress             the address of the Rest endpoints.\n" +
                               "    queryFileDirectory      The directory that contains the json data to push to the api\n" +
                               "    username,password       The username and password for authorization for the api endpoints\n" +
                               "    authString              The encoded authstring for the authorization. Can be used in place\n" +
                               "                                of the username/password pair\n" +
                               "    playbackSpeed           The initial playback speed of the replayed data\n" +
                               "    threads                 The number of threads to create for api posting\n" +
                               "    system                  The system in the createdBy field to search for\n" +
                               "    index                   The initial index of the date set to start at  \n" +
                               "    -?                      This usage message\n" +
                               "\n" +
                               "    Sample usage:\n" +
                               "            replay -h https://jsonplaceholder.typicode.com/ -d \"./data/\" -u fakeUser -p fakePassword\n" +
                               "\n" +
                               "Hotkeys while running:\n" +
                               "p        Pause/Play replay\n" +
                               "s#       set playback speed to #x\n" +
                               "q        exit playback\n"
                               );
        }
        if (!arguments.containsKey("-h")) {
            throw new IllegalArgumentException("Missing Api host url argument: -h");
        }else{
            apiHandler=new ApiHandler(arguments.get("-h"));
        }
        if (!(arguments.containsKey("-u") && arguments.containsKey("-p")) && !(arguments.containsKey("-a"))) {
            if (!(env.containsKey("UDL_USR") && env.containsKey("UDL_PWD"))) {
                throw new IllegalArgumentException("Missing credentials: -u/-p or -a");
            }else{
                apiHandler.setCredentials(env.get("UDL_USR"),env.get("UDL_PWD"));
            }
        }else{
            if (arguments.containsKey("-a")){
                apiHandler.setCredentials(arguments.get("-a"));
            }else{
                apiHandler.setCredentials(arguments.get("-u"),arguments.get("-p"));
            }
        }
        if ( !arguments.containsKey("-d")) {
            throw new IllegalArgumentException("Missing query data directory: -d");
        }else{
            queryDir = Paths.get(arguments.get("-d"));
            if (Files.notExists(queryDir)) {
                throw new IOException("Cannot access query data directory: "+queryDir.toString());
            }
        }
        if (arguments.containsKey("-s")) {
            try {
                int speed = Integer.parseInt(arguments.get("-s"));
                Replay.getInstance().setSpeed(speed);
            } catch (NumberFormatException e) {
                System.out.println("Unknown speed: " + arguments.get("-s"));
                e.printStackTrace();
                System.exit(-1);
            }
        }
        if (arguments.containsKey("-t")) {
            try {
                int threads = Integer.parseInt(arguments.get("-t"));
                apiHandler.setThreads(threads);
            } catch (NumberFormatException e) {
                System.out.println("Unknown thread count: " + arguments.get("-t"));
                e.printStackTrace();
                System.exit(-1);
            }
        }else{
            apiHandler.setThreads(1);
        }
        String startAtSearch = "";
        int index=0;
        if (arguments.containsKey("--startAtSystem")) {
                startAtSearch = arguments.get("--startAtSystem");
        }else if(arguments.containsKey("--startAtIndex")){
            try {
                index = Integer.parseInt(arguments.get("--startAtIndex"));
            } catch (NumberFormatException e) {
                System.out.println("Unknown index: " + arguments.get("--startAtIndex"));
                e.printStackTrace();
                index=0;
            }
        }

        System.out.println("Replaying data from: "+queryDir);
        System.out.println("Replaying data to: "+apiHandler.getApiUrl());
        List<DataPoint> data = DataLoader.getInstance().load(queryDir.toFile());
        Replay.getInstance().setData(data);
        if (!startAtSearch.isEmpty()) {
            System.out.println("Searching for createdBy of "+startAtSearch);
            String finalStartAtSearch = startAtSearch.toLowerCase(); // another ide workaround i guess for effectively final
            Optional<DataPoint> found = data.stream().filter(d->{
                String createdBy = (String)d.data.get("createdBy");
                return createdBy!=null && createdBy.toLowerCase().equals(finalStartAtSearch);
            }).findFirst();
            if (!found.isPresent()) {
                found = data.stream().filter(d->{
                    String createdBy = (String)d.data.get("createdBy");
                    return createdBy!=null && createdBy.toLowerCase().equals("system."+finalStartAtSearch);
                }).findFirst();
            }
            if (found.isPresent()) {
                index= data.indexOf(found.get());
                System.out.println("First element created by "+found.get().data.get("createdBy")+" at "+index);
            }
        }
        if (index >= data.size() || index < 0) {
            System.err.println("Invalid starting index detected: "+index+"\n Starting playback at index 0");
            index = 0;
        }
        System.out.println("Starting at index: "+index);
        Replay.getInstance().start(index);

        Thread uiThread = new Thread(TUI.getInstance());
        replayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                LocalDateTime beginTime = now(ZoneId.of("UTC"));
                System.out.println("Running");
                while (Replay.getInstance().getStatus() != Status.COMPLETE && Replay.getInstance().getStatus() != Status.STOPPED) {
                    try {
                        if (Replay.getInstance().getStatus()==Status.PLAYING) {
                                LocalDateTime lastSendTime = Replay.getInstance().tick();
                                Thread.interrupted(); // clear interrupt flag if set
                                long sleepTime = ChronoUnit.NANOS.between(lastSendTime,Replay.getInstance().getSendTime(Replay.getInstance().getNext()));
                                //System.out.println("next dataset in "+TimeUnit.NANOSECONDS.toMillis(sleepTime)+" Milliseconds");
                                TimeUnit.NANOSECONDS.sleep(sleepTime);
                        }else if(Replay.getInstance().getStatus()==Status.PAUSED){
                                Thread.interrupted(); // clear interrupt flag if set
                                Thread.sleep(Long.MAX_VALUE); // Sleep until interrupted from resume
                        }
                    } catch (InterruptedException ignored) { }

                }
                System.out.println("All data has been queued for POSTing.");
                apiHandler.awaitThreads();
                LocalDateTime endTime = now(ZoneId.of("UTC"));
                System.out.println("\n\nCompleted replay over " + ChronoUnit.MILLIS.between(beginTime,endTime)/1000.0+" seconds");
                System.exit(0);
            }
        } );
        replayThread.start();
        uiThread.run(); // run tui on main, run replay on its own thread.
        Replay.getInstance().setStatus(Status.STOPPED);
        replayThread.interrupt();
        replayThread.join(1000);

    }

}



