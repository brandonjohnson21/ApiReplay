package bjohnson.apireplay;

import java.util.Scanner;

// TODO: this is really slow, somehow speed this up

public class TUI implements Runnable{
    private TUI() {}
    private static TUI _instance = new TUI();
    public static TUI getInstance() {
        return _instance;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while(true) {
            String input = scanner.nextLine();
            if (input.startsWith("p")) {
                Replay.getInstance().playPause();
            } else if (input.startsWith("q")) {
                Replay.getInstance().stop();
                break;
            } else if (input.startsWith("s")) {
                try {
                    int speed = Integer.parseInt(input.substring(1));
                    Replay.getInstance().setSpeed(speed);
                } catch (NumberFormatException ignored) {
                    System.out.println("Unknown speed: " + input.substring(1));
                }
            }
            Replay.getInstance().printStatus();
        }
    }
}
