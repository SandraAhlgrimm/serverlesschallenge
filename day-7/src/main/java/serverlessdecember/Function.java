package serverlessdecember;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

import java.util.Optional;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    /**
     * This function listens at endpoint "/api/HttpTrigger-Java". Two ways to invoke it using "curl" command in bash:
     * 1. curl -d "HTTP Body" {your host}/api/HttpTrigger-Java&code={your function key}
     * 2. curl "{your host}/api/HttpTrigger-Java?text=HTTP%20Query&code={your function key}"
     * Function Key is not needed when running locally, it is used to invoke function deployed to Azure.
     * More details: https://aka.ms/functions_authorization_keys
     */
    private static HttpURLConnection connection;

    @FunctionName("HttpTrigger-Java")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.FUNCTION) final HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        // Parse query parameter
        final String query = request.getQueryParameters().get("text");
        String text = request.getBody().orElse(query);
        String resultText;

        if (text == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a text on the query string or in the request body to search for").build();
        }
        if(text.contains(" ")) {
            resultText = text.replace(" ", ", ");
            text = text.replace(" ", "%20");
        } else {
            resultText = text;
        }
        BufferedReader reader;
        String line;
        StringBuilder responseContent = new StringBuilder();

        // Get environment variables and access unsplash
        String ACCESS_KEY = System.getenv("UNSPLASH_ACCESS_KEY");
        String SECRET_KEY = System.getenv("UNSPLASH_SECRET_KEY");

        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append("https://api.unsplash.com/search/photos?client_id=");
        urlBuilder.append(ACCESS_KEY);
        urlBuilder.append("&client_secret=");
        urlBuilder.append(SECRET_KEY);
        urlBuilder.append("&query=");
        urlBuilder.append(text);
        urlBuilder.append("&count=1");

        try {
            URL url = new URL(urlBuilder.toString());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode > 299) {
                reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
            } else {
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            }
            while ((line = reader.readLine()) != null) {
                responseContent.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            connection.disconnect();
        }
        JSONObject answer = new JSONObject(responseContent.toString());
        JSONArray results = answer.getJSONArray("results");
        String smallImageUrl = results.getJSONObject(0).getJSONObject("urls").getString("small");

        // return image url
        return request.createResponseBuilder(HttpStatus.OK).body("Search for image with keywords:  " + resultText + ". Got url: " + smallImageUrl).build();
    }
}
