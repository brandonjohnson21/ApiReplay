
import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ApiHandler {
//    public static final int RESULT_LIMIT = 10000;
    private static HttpClient client = HttpClient.newHttpClient();
    private final String apiUrl;
    private String authHeaderValue;

//    ApiHandler(String apiUrl, String username, String password) {
//        this(apiUrl,"Basic " + new String(Base64.getEncoder().encode((username+":"+password).getBytes(UTF_8))));
//    }
//    ApiHandler(String apiUrl, String authValue) {
//        this(apiUrl);
//        this.authHeaderValue=authValue;
//    }
    ApiHandler(String apiUrl) {
        while (apiUrl.endsWith("/"))
            apiUrl = apiUrl.substring(0,apiUrl.length()-1);

        this.apiUrl=apiUrl;
    }
    public void setCredentials(String username, String password) {
        this.authHeaderValue = "Basic " + new String(Base64.getEncoder().encode((username+":"+password).getBytes(UTF_8)));
    }
    public void setCredentials(String authValue) {
        this.authHeaderValue=authValue;
    }
    public String getApiUrl() {
        return apiUrl;
    }
//    public List<Map<String,Object>> queryData(String queryFilename) throws IOException, InterruptedException {
//        //String endpoint = "";
//        Gson gson = new Gson();
//        long dataCount = 0;
//        List<Map<String,Object>> list = new ArrayList<>();
//        String query = apiUrl + Files.readString(Paths.get(queryFilename));
//        if (query.indexOf('?')!=-1) {
//            query+="&";
//        }else{
//            query+="?";
//        }
//        List<Map<String,Object>> jsonData = null;
//        while(jsonData == null || jsonData.size() > 0) {
//            HttpRequest request = HttpRequest.newBuilder(
//                    URI.create(query + "maxResults=" + RESULT_LIMIT + "&firstResult=" + dataCount ))
//                    .headers("accept", "application/json",
//                            "Authorization", authHeaderValue
//                            )
//
//                    .build();
//            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//            if (response.statusCode()==200) {
//                jsonData = gson.fromJson(response.body(), new TypeToken<List<Map<String, Object>>>() {
//                }.getType());
//                dataCount+=RESULT_LIMIT;
//            }else{
//                jsonData = new ArrayList<>();
//            }
//            list.addAll(jsonData);
//            System.out.println(response.body());
//        }
//        return list;
//    }
//    public Path saveQueryResultToJson(String queryFilename, String jsonFilename) throws IOException, InterruptedException {
//        Gson gson = new Gson();
//        List<Map<String,Object>> qData = queryData(queryFilename);
//        gson.toJson(qData,new FileWriter(jsonFilename));
//        return Paths.get(jsonFilename);
//    }
    public void postData(DataPoint dataPoint) throws IOException, InterruptedException {
    	Gson gson = new Gson();
    	String s=gson.toJson(dataPoint.data);
        HttpRequest request = HttpRequest.newBuilder(
                URI.create(apiUrl+dataPoint.endpoint))
                .headers("accept", "application/json",
                        "Authorization", authHeaderValue,
                        "Content-Type","application/json"
                ).POST(HttpRequest.BodyPublishers.ofString(s, UTF_8))
                .build();
//        System.out.println("-> "+request.toString());
//        System.out.println("   "+request.headers());
//        System.out.println("   "+s);
        long x=System.currentTimeMillis();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        long y=System.currentTimeMillis();
        System.out.print(y-x+" ");
        
        if (response.statusCode()>399) {
            System.out.println("ERROR response "+response.statusCode());
            System.out.println("Body:\n"+response.body());
            
            throw new IOException ("Failed to post data to api. Received status code: "+response.statusCode()+"\n"+request);
        }
    }
}
