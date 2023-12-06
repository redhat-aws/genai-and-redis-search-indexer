package com.enterprisewebservice.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@ApplicationScoped
public class ApiClient {
    private final HttpClient client;
    private final ObjectMapper objectMapper;
    private final String baseUri = "https://strapi-goalsetter.apps.ocpai.enterprisewebservice.com/api";

    @Inject
    public ApiClient(HttpClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public String get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + path))
                .header("Authorization", lookupBearerToken())
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("HTTP error code : " + response.statusCode());
        }
        return response.body();
    }

    // Implement POST, PUT, DELETE if necessary

   public String post(String path, Object object) throws Exception {
        
        JsonNode jsonNode = objectMapper.valueToTree(object);
        ((ObjectNode) jsonNode).remove("id");
        Map<String, JsonNode> dataObject = new HashMap<>();
        dataObject.put("data", jsonNode);
       
        String jsonString = objectMapper.writeValueAsString(dataObject);
        System.out.println("jsonString: " + jsonString);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + path))
                .header("Authorization", lookupBearerToken())
                .header("Content-Type", MediaType.APPLICATION_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(jsonString))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.out.println("HTTP error code : " + response.toString());
            throw new Exception("HTTP error code : " + response.statusCode());
        }
        return response.body();
    }

    public String delete(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUri + path))
                .header("Authorization", lookupBearerToken())
                .DELETE()
                .build();
    
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    
        if (response.statusCode() != 200) {
            throw new Exception("HTTP error code : " + response.statusCode());
        }
        return response.body();
    }
    

    private String lookupBearerToken() {
        return "Bearer 99e19ac11446114f9f969450860b6c29cefdaf44a8122ba1f216fad3f096b80bb8e9137b5a09803e7234f1ca26e6440a33319b1f9c4dc9c83461b5439caa586bf9f8c8b514f2a3f218e4ee0d34583916f7af8b4d01c6ed3abab646c75c9525eb79a93b0891f6504314b658f508e640f0e4f03cc276cecdd05718d4db8a3a94dc";
    }
}
