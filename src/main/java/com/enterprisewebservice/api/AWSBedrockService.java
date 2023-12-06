package com.enterprisewebservice.api;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.json.JSONArray;
import org.json.JSONObject;

import com.enterprisewebservice.embeddings.EmbeddingData;
import com.enterprisewebservice.embeddings.EmbeddingResponse;
import com.enterprisewebservice.embeddings.Usage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeAsyncClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@ApplicationScoped
public class AWSBedrockService {
    @Inject
    @ConfigProperty(name = "aws.accessKeyId")
    String accessKeyId;

    @Inject
    @ConfigProperty(name = "aws.secretAccessKey")
    String secretAccessKey;

    @Inject
    @ConfigProperty(name = "aws.region")
    String region;

        /**
     * Invokes the Meta Llama 2 Chat model to run an inference based on the provided input.
     *
     * @param prompt The prompt for Llama 2 to complete.
     * @return The generated response.
     */
    public String invokeLlama2(String prompt) {
        /*
          The different model providers have individual request and response formats.
          For the format, ranges, and default values for Meta Llama 2 Chat, refer to:
          https://docs.aws.amazon.com/bedrock/latest/userguide/model-parameters-meta.html
         */

        String llama2ModelId = "meta.llama2-13b-chat-v1";

        BedrockRuntimeClient client = BedrockRuntimeClient.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
        .build();


        String payload = new JSONObject()
                .put("prompt", prompt)
                .put("max_gen_len", 512)
                .put("temperature", 0.5)
                .put("top_p", 0.9)
                .toString();

        InvokeModelRequest request = InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(llama2ModelId)
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = client.invokeModel(request);

        JSONObject responseBody = new JSONObject(response.body().asUtf8String());

        String generatedText = responseBody.getString("generation");

        return generatedText;
    }


    public EmbeddingResponse invokeTitanEmbedding(List<String> texts) {
        System.out.println("Input texts are: " + texts);
        StringBuilder sb = new StringBuilder();
        String delimiter = "\n"; // Change this to your preferred delimiter

        for (String text : texts) {
            sb.append(text).append(delimiter);
        }

        // Optional: remove the last delimiter
        if (sb.length() > 0) {
            sb.setLength(sb.length() - delimiter.length());
        }

        String combinedText = sb.toString();
        System.out.println("Combined text is: " + combinedText);
        String amazonTitanEmbed = "amazon.titan-embed-text-v1";

        BedrockRuntimeClient client = BedrockRuntimeClient.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
        .build();


        String payload = new JSONObject()
                .put("inputText", combinedText)
                .toString();

        InvokeModelRequest request = InvokeModelRequest.builder()
                .body(SdkBytes.fromUtf8String(payload))
                .modelId(amazonTitanEmbed)
                .contentType("application/json")
                .accept("application/json")
                .build();

        InvokeModelResponse response = client.invokeModel(request);
        System.out.println("Response is: " + response.body().asUtf8String());
        JSONObject responseBody = new JSONObject(response.body().asUtf8String());
        JSONArray generatedEmbeddings = responseBody.getJSONArray("embedding");

        EmbeddingResponse embeddingResponse = new EmbeddingResponse();
        List<EmbeddingData> embeddingDataList = new ArrayList<>();
        EmbeddingData embeddingData = new EmbeddingData();
        List<Float> embeddings = new ArrayList<>();
        for (int i = 0; i < generatedEmbeddings.length(); i++) {
            float embeddingValue = generatedEmbeddings.getFloat(i);
            embeddings.add(embeddingValue);         
            
        }
        embeddingData.setEmbedding(embeddings);
        // Set other necessary properties of EmbeddingData if required
        embeddingDataList.add(embeddingData);

        embeddingResponse.setData(embeddingDataList);
        embeddingResponse.setModel("amazon.titan-embed-text-v1"); // Set the model name
        embeddingResponse.setObject("YourObject"); // Set the object name
        embeddingResponse.setUsage(new Usage()); // Set the usage data

        return embeddingResponse;
    }

}
