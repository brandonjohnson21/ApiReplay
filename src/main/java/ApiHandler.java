import com.google.gson.Gson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ApiHandler {
//    public static final int RESULT_LIMIT = 10000;
    private static HttpClient client = HttpClient.newHttpClient();
    private final String apiUrl;
    private String authHeaderValue;
    private ExecutorService executor;
    private static Gson gson = new Gson();

    public void setThreads(int t) {
        executor=Executors.newFixedThreadPool(t);
    }
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

    public void postData(DataPoint dataPoint, int index) {
    	this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String s=gson.toJson(dataPoint.data);
                HttpRequest request = HttpRequest.newBuilder(
                        URI.create(apiUrl+dataPoint.endpoint))
                        .headers("accept", "application/json",
                                "Authorization", authHeaderValue,
                                "Content-Type","application/json"
                        ).POST(HttpRequest.BodyPublishers.ofString(s, UTF_8))
                        .build();
                    HttpResponse<String> response;
                try {
                	long x=System.currentTimeMillis();
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    long y=System.currentTimeMillis();
                    System.out.print(index+"["+(y-x)+"] ");
                    if (response != null && response.statusCode() > 399) {
                        System.out.println("ERROR response " + response.statusCode());
                        System.out.println("Body:\n" + response.body());
                        throw new IOException("Failed to post data to api. Received status code: " + response.statusCode() + "\n" + request);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
//        System.out.println("-> "+request.toString());
//        System.out.println("   "+request.headers());
//        System.out.println("   "+s);
        
    }
    public void awaitThreads() {
        executor.shutdown();
        try {
            executor.awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
        }catch(InterruptedException e) {
            System.out.println("Thread interrupted while awaiting completion of api posts");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
