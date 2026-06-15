package web.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class WebClient {
    private static final Logger logger = LoggerFactory.getLogger(WebClient.class);
    private HttpClient client;

    public WebClient() {
        this.client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }

    public CompletableFuture<String> sendTask(String url, byte[] requestPayload) {
        logger.info("sendTask");
        HttpRequest request = HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofByteArray(requestPayload))
            .uri(URI.create(url))
            .header("X-Debug", "true")
            .build();

        return this.client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body);
    }
}
