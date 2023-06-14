import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import com.neuronrobotics.bowlerstudio.BowlerKernel
import com.neuronrobotics.bowlerstudio.BowlerStudio
import com.neuronrobotics.bowlerstudio.BowlerStudioController
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine
import javafx.scene.control.TextInputDialog
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets;
import groovy.json.JsonSlurper
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.http.Header;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.Header;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

// inspired by https://simonwillison.net/2023/Jan/13/semantic-search-answers/

public class QAPatternExample {

    public static void main(String[] args) throws IOException {
        String question = "What is a CSG file?";
		
		// Look for or save OpenAI API key
        String keyLocation = ScriptingEngine.getWorkspace().getAbsolutePath() + File.separator +"gpt-key.txt"
		if(!new File(keyLocation).exists()) {
			KeyDialog(keyLocation)
			return;
		}
		
		
		println "Loading API key from "+keyLocation
		String apiKey = new String(Files.readAllBytes(Paths.get(keyLocation)));
		//println "API key: "+apiKey
		
		// Step 1: Download documentation in markdown format
		String linkToDocumentation = "https://github.com/CommonWealthRobotics/CommonWealthRobotics.github.io/tree/a0551b55ee1cc64f48c16e08a6f7928e7d6601bd/content/JavaCAD";
		String savePath = ScriptingEngine.getWorkspace().getAbsolutePath() + File.separator + "documentation"
		new MarkdownDownloader(linkToDocumentation, savePath)

        // Step 2: Run a search query to find relevant content
        String searchResults = runSearchQuery(question);

        // Step 3: Extract relevant content and construct the prompt
        String prompt = constructPrompt(searchResults, question);
        System.out.println("\nPrompt: \n" + prompt);

        // Step 4: Call the OpenAI API with embeddings
        String[] inputs = ["input1", "input2", "input3"];

        List<List<Float>> embeddings = OpenAIAPIClient.callEmbeddingAPI(inputs, apiKey);

//        System.out.println("Embeddings:");
//        for (List<Float> embedding : embeddings) {
//        	for (Float emb : embedding) {
//            	System.out.println("found a number");
//            	System.out.println(emb);
//        	}
//        }
//        String answer = OpenAIAPIClient.callDavinciAPI(prompt, apiKey);

        // Step 4: Process and display the answer
//        String processedAnswer = processAnswer(answer);
//        System.out.println("\nProcessed Answer: \n" + processedAnswer);
    }

	private static KeyDialog(String keyLocation) {
		BowlerStudio.runLater({
			TextInputDialog dialog = new TextInputDialog("your OpenAI API Key here");
			dialog.setTitle("Enter your OpenAI Key");
			dialog.setHeaderText("Create key here - https://platform.openai.com/account/api-keys");
			dialog.setContentText("Please enter your key:");

			// Traditional way to get the response value.
			Optional<String> result = dialog.showAndWait();
			if (result.isPresent()){
				String resultGet = result.get()
				System.out.println("Your key: " + resultGet);
				new Thread({
					try {
						File myObj = new File(keyLocation);
						if (myObj.createNewFile()) {
							System.out.println("File created: " + myObj.getName());
						} else {
							System.out.println("File already exists.");
						}
						FileWriter myWriter = new FileWriter(keyLocation);
						myWriter.write(resultGet);
						myWriter.close();
						System.out.println("Successfully wrote key to your local file.");
					} catch (IOException e) {
						System.out.println("An error occurred.");
						e.printStackTrace();
					}


				}).start()
			}

		})
	}

    private static String runSearchQuery(String question) {
        // Implement your search query logic here
        // Use the question to search for relevant content in your documentation
        // Return the search results as a string
        return "Search results for the question: " + question;
    }

    private static String constructPrompt(String searchResults, String question) {
        // Extract relevant content from search results
        // Glue the extracted content together with the question to form the prompt
        return searchResults + "\n\nGiven the above content, answer the following question: " + question;
    }


	private static String processAnswer(String answer) {
	    // Extract the answer from the API response
	    String processedAnswer = "";
	
	    try {
	        // Parse the JSON response
	        JsonSlurper slurper = new JsonSlurper();
	        Map<String, Object> responseMap = (Map<String, Object>) slurper.parseText(answer);
	
	        // Extract the answer text from the response
	        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
	        if (choices != null && !choices.isEmpty()) {
	            Map<String, Object> answerChoice = choices.get(0);
	            String text = (String) answerChoice.get("text");
	            processedAnswer = text.trim();
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	
	    return processedAnswer;
	}

}



public class OpenAIAPIClient {
    private static final String DAVINCI_API_URL = "https://api.openai.com/v1/engines/davinci/completions";
    private static final String EMBEDDING_API_URL = "https://api.openai.com/v1/embeddings";
    private static final int EMBEDDING_SIZE = 1536;

    private static String callOpenAIAPI(String url, String data, String apiKey) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setDoOutput(true);

        connection.getOutputStream().write(data.getBytes());

        int responseCode = connection.getResponseCode();

        BufferedReader reader;
        if (responseCode >= 400) {
            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
        } else {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        }

        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        String resp = response.toString();

        return resp;
    }

    public static String callDavinciAPI(String prompt, String apiKey) throws IOException {
        String data = "{\"prompt\": \"" + escapeNewLines(prompt) + "\"}";
        return callOpenAIAPI(DAVINCI_API_URL, data, apiKey);
    }

    public static List<List<Float>> callEmbeddingAPI(String[] inputs, String apiKey) throws IOException {
        String data = "{\"input\": " + buildInputJson(inputs) + ", \"model\": \"text-embedding-ada-002\"}";
        String response = callOpenAIAPI(EMBEDDING_API_URL, data, apiKey);

        // Extract the embeddings from the response
        List<List<Float>> embeddings = extractEmbeddings(response);

        return embeddings;
    }

    private static List<List<Float>> extractEmbeddings(String response) {
        try {
            JSONObject jsonResponse = new JSONObject(response);
            JSONArray data = jsonResponse.getJSONArray("data");

            List<List<Float>> embeddings = new ArrayList<>();

            for (int i = 0; i < data.length(); i++) {
                JSONObject embeddingObj = data.getJSONObject(i);
                JSONArray embeddingArray = embeddingObj.getJSONArray("embedding");

                List<Float> embedding = new ArrayList<>();

                for (int j = 0; j < embeddingArray.length(); j++) {
                    float value = (float) embeddingArray.getDouble(j);
                    embedding.add(value);
                }

                embeddings.add(embedding);
            }

            return embeddings;
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid response format: " + e.getMessage());
        }
    }

    private static String escapeNewLines(String text) {
        return text.replace("\n", "\\n");
    }

    private static String buildInputJson(String[] inputs) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < inputs.length; i++) {
            json.append("\"").append(inputs[i]).append("\"");
            if (i != inputs.length - 1) {
                json.append(", ");
            }
        }
        json.append("]");
        return json.toString();
    }
}

public class MarkdownDownloader {
    private CloseableHttpClient httpClient;

    public MarkdownDownloader(String targetSite, String savePath) {
        try {
            httpClient = HttpClients.createDefault();
            downloadMarkdownFiles(targetSite, savePath);
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        } finally {
            closeHttpClient();
        }
    }

    private void closeHttpClient() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void downloadMarkdownFiles(String targetSite, String savePath) throws IOException, URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(targetSite);
        HttpGet request = new HttpGet(uriBuilder.build());
        HttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        String htmlContent = EntityUtils.toString(entity);
        Document document = Jsoup.parse(htmlContent);
        Elements links = document.select("a[href]");
        for (Element link : links) {
            String href = link.attr("href");
            if (isMarkdownFile(href)) {
                downloadFile(targetSite + href, savePath);
            }
        }
    }

    private boolean isMarkdownFile(String link) {
        return link.endsWith(".md");
    }

	private void downloadFile(String fileUrl, String savePath) throws IOException {
	    String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
	    Path saveDirectory = Paths.get(savePath);
	    if (!Files.exists(saveDirectory)) {
	        Files.createDirectories(saveDirectory);
	    }
	    savePath = savePath + File.separator + fileName;
	    Path outputFile = Paths.get(savePath);
	
	    // Check if the file already exists and looks unchanged
	    if (Files.exists(outputFile) && isFileUnchanged(outputFile, fileUrl)) {
	        System.out.println("Skipped: " + fileName);
	        return;
	    }
	
	    HttpGet request = new HttpGet(fileUrl);
	    HttpResponse response = null;
	    try {
	        response = httpClient.execute(request);
	        HttpEntity entity = response.getEntity();
	        InputStream inputStream = entity.getContent();
	        Files.copy(inputStream, outputFile, StandardCopyOption.REPLACE_EXISTING);
	    } finally {
	        if (response != null) {
	            EntityUtils.consumeQuietly(response.getEntity());
	        }
	    }
	    System.out.println("Downloaded: " + fileName);
	}
	
	private boolean isFileUnchanged(Path filePath, String fileUrl) throws IOException {
		// This feature appears to be broken.
		
	    // Check if the file already exists
	    if (!Files.exists(filePath)) {
	        return false; // File does not exist, so it is considered changed
	    }
	
	    // Read the content of the local file
	    String localContent = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
	
	    HttpGet request = new HttpGet(fileUrl);
	    HttpResponse response = null;
	    try {
	        response = httpClient.execute(request);
	        HttpEntity entity = response.getEntity();
	        String remoteContent = EntityUtils.toString(entity, StandardCharsets.UTF_8);
	
	        // Compare the content of the local file with the remote file
	        return localContent.equals(remoteContent);
	    } finally {
	        if (response != null) {
	            EntityUtils.consumeQuietly(response.getEntity());
	        }
	    }
	}

}
