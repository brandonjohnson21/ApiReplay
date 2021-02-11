import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

public class DataLoader {
    private DataLoader() {}
    private static DataLoader _instance = new DataLoader();

    public static DataLoader getInstance() {
        if (_instance == null)
            _instance = new DataLoader();
        return _instance;
    }

    public List<DataPoint> load(File folder) throws IOException {

        if (folder==null)
            return null;
        //Map<String,List<Map<String,Object>>> data = new HashMap<>();
        List<DataPoint> data = new ArrayList<>();
        Files.walk(folder.toPath(),10).filter(f-> Files.isRegularFile(f) && Files.isReadable(f)).forEach(file->{
            String fileName = file.getFileName().toString();
            if (!fileName.endsWith(".json"))
                return;
            String baseName = fileName.substring(0,fileName.lastIndexOf('.'));
            Path queryFile = Paths.get(folder.toString(),baseName+".txt");
            if (Files.isRegularFile(queryFile) && Files.isReadable(queryFile)) {
                try {
                    URL queryURL = new URL(Files.readString(queryFile));
                    String endpoint = queryURL.getPath();
                    if (endpoint.endsWith("/history")) {
                        endpoint=endpoint.substring(0,endpoint.indexOf("/history"));
                    }
                    List<Map<String, Object>> newData = FileHandler.LoadArrayJson(file);

                    // Lambda requires effectively final. IDE doesnt see endpoint above as effectively final.
                    String finalEndpoint = endpoint;
                    int unfilteredSize = newData.size();
                    newData = newData.stream().filter(d->d.containsKey("createdAt") && d.containsKey("ts")).collect(Collectors.toList());

                    data.addAll(
                            newData.stream().map(d -> {
                                DataPoint dataPoint = new DataPoint();
                                dataPoint.endpoint = finalEndpoint;
                                d.replace("createdAt", LocalDateTime.parse((String) d.get("createdAt"), DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))));
                                d.replace("ts", LocalDateTime.parse((String) d.get("ts"), DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))));
                                dataPoint.data = d;
                                return dataPoint;
                            }).collect(Collectors.toList())
                    );
                    if (unfilteredSize != newData.size()) {
                        System.err.println((unfilteredSize-newData.size()) + " entries dropped from "+file.toString()+" due to missing ts or createdAt fields");
                    }
                    System.out.println("Loaded "+newData.size() + " entries from "+file.toString());
                } catch (MalformedURLException e) {
                    System.err.println("Failed to load "+file.toString()+". Invalid query url in "+queryFile.toString());
                } catch (IOException e) {
                    // failed to open file
                    e.printStackTrace();
                }
            }else{
                System.out.println("Failed to process "+ file.toString() + ". Missing "+queryFile.toString());
            }
        });
        LocalDateTime firstTime = getFirstTime(data);
        //convert ts and createdAt entries to nanosecond offsets from dataset start
        data.forEach(d->{
            d.createdAt=ChronoUnit.NANOS.between(firstTime,(LocalDateTime)d.data.get("createdAt"));
            d.timestamp=ChronoUnit.NANOS.between(firstTime,(LocalDateTime)d.data.get("ts"));
            stripProperties(d.data);
        });
        Collections.sort(data);
        return data;
    }
    private LocalDateTime getFirstTime(List<DataPoint> data) {
        LocalDateTime lowestCAForSet = data.stream().map(e->(LocalDateTime)e.data.get("createdAt")).filter(Objects::nonNull).reduce(now(ZoneId.of("UTC")),(lowest,cur)->(cur.compareTo(lowest) < 0)?cur:lowest);
        LocalDateTime lowestTSForSet = data.stream().map(e->(LocalDateTime)e.data.get("ts")).filter(Objects::nonNull).reduce(now(ZoneId.of("UTC")),(lowest,cur)->(cur.compareTo(lowest) < 0)?cur:lowest);
        return (lowestCAForSet.isBefore(lowestTSForSet))?lowestCAForSet:lowestTSForSet;
    }
    private void stripProperties(Map<String,Object> d) {
        //TODO: if this becomes large, it may be better end-game to load schemas and compare against them
        d.remove("idOnOrbit");
        d.remove("createdAt");
    }
}
