import java.util.Map;

public class DataPoint implements Comparable<DataPoint> {
    String endpoint;
    Long createdAt=null;
    Map<String,Object> data;

    @Override
    public int compareTo(DataPoint o) {
        long v=createdAt-o.createdAt;
        return (v<0)?-1:(v>0)?1:0;
    }

}
