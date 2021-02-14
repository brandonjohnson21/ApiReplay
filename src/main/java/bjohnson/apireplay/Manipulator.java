package bjohnson.apireplay;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Manipulator {
    private static final String[] rootTimestampFields={
            /// common
            "ts",
            "createdAt",

            /// mti
            "dwellts",
            "platlocts"
            /// track
            // msgTs
            /// poi?
            // start
            // stale
    };
    public static void convertTimestampStringsToLocalDateTime(Map<String,Object> m) {
        for (String field:rootTimestampFields) {
            if (m.containsKey(field)) {
                Object f = m.get(field);
                if (f instanceof String)
                    m.replace(field, LocalDateTime.parse((String)f, DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"))));
            }
        }
        if (m.containsKey("platformLocs")) {
            Object locs = m.get("platformLocs");
            if (locs instanceof List) {
                // this is probably not good form but without a full schema of each data type built into classes and handlers in each class...
                ((List)locs).stream().forEach(e->{
                    if (e instanceof Map) {
                        convertTimestampStringsToLocalDateTime((Map<String, Object>) e);
                    }
                });
            }
        }
        if (m.containsKey("dwells")) {
            Object dwells = m.get("dwells");
            if (dwells instanceof List) {
                // this is probably not good form but without a full schema of each data type built into classes and handlers in each class...
                ((List)dwells).stream().forEach(e->{
                    if (e instanceof Map) {
                        convertTimestampStringsToLocalDateTime((Map<String, Object>) e);
                    }
                });
            }
        }
    }
    public static void timestampsToNanoOffset(Map<String,Object> m, LocalDateTime base) {
        if (base == null || m == null)
            return;
        for (String field:rootTimestampFields) {
            if (m.containsKey(field)) {
                Object f = m.get(field);
                if (f instanceof LocalDateTime)
                    m.replace(field, ChronoUnit.NANOS.between(base,(LocalDateTime)f));
            }
        }
        if (m.containsKey("platformLocs")) {
            Object locs = m.get("platformLocs");
            if (locs instanceof List) {
                // this is probably not good form but without a full schema of each data type built into classes and handlers in each object class...
                ((List)locs).stream().forEach(e->{
                    if (e instanceof Map) {
                        timestampsToNanoOffset((Map<String, Object>) e,base);
                    }
                });
            }
        }
        if (m.containsKey("dwells")) {
            Object dwells = m.get("dwells");
            if (dwells instanceof List) {
                // this is probably not good form but without a full schema of each data type built into classes and handlers in each object class...
                ((List)dwells).stream().forEach(e->{
                    if (e instanceof Map) {
                        timestampsToNanoOffset((Map<String, Object>) e,base);
                    }
                });
            }
        }
    }
    public static void nanoOffsetsToLocalDateTime(Map<String,Object> m, LocalDateTime base, long skip, int speed) {
        if (base == null || m == null)
            return;
        for (String field:rootTimestampFields) {
            if (m.containsKey(field)) {
                Object f = m.get(field);
                if (f instanceof Long && f != null)
                    m.replace(field,ChronoUnit.NANOS.addTo(base,((Long)f-skip)/speed).toInstant(ZoneOffset.UTC).toString());
            }
        }
        if (m.containsKey("platformLocs")) {
            Object locs = m.get("platformLocs");
            if (locs instanceof List) {
                // this is probably not good form but without a full schema of each data type built into classes and handlers in each object class...
                ((List)locs).stream().forEach(e->{
                    if (e instanceof Map) {
                        nanoOffsetsToLocalDateTime((Map<String, Object>) e,base,skip,speed);
                    }
                });
            }
        }
        if (m.containsKey("dwells")) {
            Object dwells = m.get("dwells");
            if (dwells instanceof List) {
                // this is probably not good form but without a full schema of each data type built into classes and handlers in each object class...
                ((List)dwells).stream().forEach(e->{
                    if (e instanceof Map) {
                        nanoOffsetsToLocalDateTime((Map<String, Object>) e,base,skip,speed);
                    }
                });
            }
        }
    }
    public static void generateID(Map<String, Object> m) {
        if (m.containsKey("id")) {
            m.replace("id", UUID.randomUUID());
        }
    }
}
