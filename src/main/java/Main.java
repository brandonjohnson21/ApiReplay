import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static ApiHandler apiHandler;
    public static void main(String[] args) throws IOException, InterruptedException {
//        System.out.println(Arrays.toString(args));
        Path queryDir;
        List<String> validArguments = List.of(
                "-h", // Api host Address
                "-u", // username
                "-p", // password
                "-a", // auth string
                "-d", // query directory
                "-s", // playback speed
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
            System.out.println("replay  -h hostAddress -d queryFileDirectory [-u username -p password] [-a authString] [-s playback_speed] [-?]\n" +
                               "    hostAddress             the address of the Rest endpoints.\n" +
                               "    queryFileDirectory      The directory that contains the json data to push to the api\n" +
                               "    username,password       The username and password for authorization for the api endpoints\n" +
                               "    authString              The encoded authstring for the authorization. Can be used in place\n" +
                               "                                of the username/password pair\n" +
                               "    playbackSpeed           The initial playback speed of the replayed data\n" +
                               "    -?                      This usage message\n" +
                               "\n" +
                               "    Sample usage:\n" +
                               "            replay -h https://jsonplaceholder.typicode.com/ -d \"./data/\" -u fakeUser -p fakePassword\n" +
                               "\n" +
                               "Hotkeys while running:\n" +
                               "p        Pause/Play replay\n" +
                               "s#       set playback speed to #x\n" +
                               "r        reset playback to beginning of data\n" +
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

        System.out.println("Replaying data from: "+queryDir);
        System.out.println("Replaying data to: "+apiHandler.getApiUrl());

        Replay.getInstance().setData(DataLoader.getInstance().load(queryDir.toFile()));
        Replay.getInstance().playPause();

        Thread uiThread = new Thread(TUI.getInstance());
        Thread ReplayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Running");
                while (Replay.getInstance().getStatus() != Status.COMPLETE && Replay.getInstance().getStatus() != Status.STOPPED) {
                    Replay.getInstance().tick();
                }
                System.exit(0);
            }
        } );
        ReplayThread.start();
        uiThread.run(); // run tui on main, run replay on its own thread.
        //ReplayThread.interrupt();
        ReplayThread.join(1000);
        //List<HashMap<handler.queryData("test","/todos/1");
//        JFrame frame = new JFrame("");
//        UI panel = new UI();
//        frame.addWindowListener(
//                new WindowAdapter() {
//                    public void windowClosing(WindowEvent e) {
//                        System.exit(0);
//                    }
//                }
//        );
//        frame.getContentPane().add(panel,"Center");
//        frame.setSize(panel.getPreferredSize());
//        frame.setVisible(true);

    }

}



