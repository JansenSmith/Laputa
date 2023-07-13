
@Grapes(
	@Grab(group='com.theokanning.openai-gpt3-java', module='service', version='0.14.0')
)

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
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardCopyOption
import java.nio.charset.StandardCharsets;
import groovy.json.JsonSlurper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
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
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Whitelist;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import java.util.Comparator
import java.util.PriorityQueue
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// inspired by https://simonwillison.net/2023/Jan/13/semantic-search-answers/

public class QAPatternExample {
    private static ModelAPIClient apiClient; // Declare apiClient as a class member

    public static void main(String[] args) throws IOException {
		// Step 1: Prompt the user for a question
		String defaultQuestion = "How do I make a cube?"
		String question = QuestionDialog(defaultQuestion);
		//System.out.println("Question: " + question);
		
		
		// Step 2: Retrieve the user's API key from a local file in bowler-workspace
		File keyFile = new File(ScriptingEngine.getWorkspace(), "gpt-key.txt");
		if (!keyFile.exists()) {
			// If the API key file doesn't exist, prompt the user to enter it
		    KeyDialog(keyFile);
		    return;
		}
		System.out.println("Loading API key from " + keyFile);
		String apiKey = new String(Files.readAllBytes(keyFile.toPath()));
//		ModelAPIClient apiClient = new ModelAPIClient("gpt-key.txt");
//		System.out.println(apiClient.getModels());
		
		// Step 3: Get embeddings for the user's question
		List<String> questionSegments = getSegmentsFromString(question);
		List<Float> questionEmbeddings = getEmbeddings(questionSegments);
		
		// Step 4: Download BowlerStudio documentation
		String url="https://github.com/CommonWealthRobotics/CommonWealthRobotics.github.io.git";
		ScriptingEngine.cloneRepo(url, null)
		File locationOfDocs = ScriptingEngine.getRepositoryCloneDirectory(url);
		Path savePath = Paths.get(ScriptingEngine.getWorkspace().getAbsolutePath(), "Laputa").normalize().toAbsolutePath();
		def status = 1
		
		//String GroovyFiles = ScriptingEngine.getWorkspace().getAbsolutePath() + File.separator + "gitcache" + File.separator + "gist.github.com";
		//String savePath = ScriptingEngine.getWorkspace().getAbsolutePath() + File.separator + "Laputa";
		//new MarkdownDownloader(linkToDocumentation, savePath);
		//new GistDownloader(linkToDocumentation, savePath);
		
//		// Step 5: Iterate through documentation files, get embeddings, and cache locally
//		List<File> files = getFiles(savePath);
//		Map<File, List<Float>> embeddingsMap = getEmbeddingsMap(files, savePath); // pass savePath to search the cache for new files
//		
//		// Step 6: Calculate similarity between question embeddings and documentation file embeddings
//		Map<File, Float> similarityMap = calculateSimilarity(questionEmbeddings, embeddingsMap);
//		
//		// Step 7: Find the N most similar files
//		List<File> mostSimilarFiles = findNMostSimilarFiles(similarityMap, 2, true);
//		
//		// Step 8: Construct the prompt for the OpenAI API call
//		String prompt = constructPrompt(mostSimilarFiles, question);
//		
//		// Step 9: Call the OpenAI API to get the answer
//		String answer = apiClient.callChatAPI(prompt);
//		
//		// Step 10: Process and display the answer
//		System.out.println("Answer: " + answer);
//		Tab t = new Tab("Laputa");
//		HBox content = new HBox();
//		content.getChildren().add(new TextArea(answer));
//		t.setContent(content);
//		BowlerStudioController.addObject(t, null);

    }
	
	public void close() {
		//capture.release();
		BowlerStudioController.removeObject(t)
		println "Clean Exit"
	}
	
	private static List<File> getFiles(Path savePath) {
		List<File> markdownFiles = getMarkdownFiles(savePath);
		List<File> gistFiles = getGistFiles(savePath);
		List<File> javaFiles = getJavaFiles(savePath);
	
		List<File> files = new ArrayList<>();
		files.addAll(markdownFiles);
		files.addAll(gistFiles);
		files.addAll(javaFiles);
	
		return files;
	}

	private static List<File> getMarkdownFiles(Path savePath) throws IOException {
	    List<File> markdownFiles = new ArrayList<>();
	    try {
	        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(savePath);
	        for (Path path : directoryStream) {
	            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".md")) {
	                markdownFiles.add(path.toFile()); // Convert Path to File and add to the list
	            }
	        }
	    } catch (IOException e) {
	        // Handle the exception or log the error
	        e.printStackTrace();
	    }
	    return markdownFiles;
	}
	
	private static List<File> getGistFiles(Path savePath) throws IOException {
	    List<File> gistFiles = new ArrayList<>();
	    try {
	        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(savePath);
	        for (Path path : directoryStream) {
	            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".groovy")) {
	                gistFiles.add(path.toFile()); // Convert Path to File and add to the list
	            }
	        }
	    } catch (IOException e) {
	        // Handle the exception or log the error
	        e.printStackTrace();
	    }
	    return gistFiles;
	}
	
	private static List<File> getJavaFiles(Path savePath) throws IOException {
	    List<File> javaFiles = new ArrayList<>();
	    try {
	        DirectoryStream<Path> directoryStream = Files.newDirectoryStream(savePath);
	        for (Path path : directoryStream) {
	            if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".java")) {
	                javaFiles.add(path.toFile()); // Convert Path to File and add to the list
	            }
	        }
	    } catch (IOException e) {
	        // Handle the exception or log the error
	        e.printStackTrace();
	    }
	    return javaFiles;
	}

	
	private static String readFileContent(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()));
	}

	private static List<String> getSegmentsFromFile(File file) {
		try {
			String content = new String(Files.readAllBytes(file.toPath()));
			// Split content into smaller segments if necessary
			return Arrays.asList(content.split("\\r?\\n\\r?\\n")); // Split by empty lines
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new ArrayList<>();
	}
	
	private static List<String> getSegmentsFromString(String input) {
		// Split the input string based on whitespace
		String[] segmentsArray = input.split("\\s+");
	
		// Convert the array to a list
		List<String> segmentsList = Arrays.asList(segmentsArray);
	
		// Remove any empty segments
		segmentsList.removeAll { it.isEmpty() };
	
		return segmentsList;
	}
	
	private static List<Float> getEmbeddings(List<String> segments) {
	    // Call the OpenAI API with segments to get embeddings
	    List<List<Float>> embeddings = apiClient.callEmbeddingAPI(segments.toArray(new String[0]));
	
	    List<Float> flattenedEmbeddings = new ArrayList<>();
	    for (List<Float> segmentEmbeddings : embeddings) {
	        flattenedEmbeddings.addAll(segmentEmbeddings);
	    }
	
	    return flattenedEmbeddings;
	}
	
	private static Map<File, List<Float>> getEmbeddingsMap(List<File> files, Path savePath) {
		def filename = "embeddingsMap.ser"
		File saveFile = savePath.resolve(filename).toFile();
	    Map<File, List<Float>> embeddingsMap = new HashMap<>();
	
	    // Load cache from file if it exists
	    Map<File, List<Float>> cache = loadCacheFromFile(saveFile);
	
	    for (File file : files) {
	        if (cache.containsKey(file)) {
	            System.out.println("Using cached embeddings for file: " + file.getName());
	            embeddingsMap.put(file, cache.get(file));
	        } else {
	            System.out.println("Embedding file: " + file.getName()); // Print the file name
	            List<String> segments = getSegmentsFromFile(file);
	            try {
	                List<Float> embeddings = getEmbeddings(segments);
	                embeddingsMap.put(file, embeddings);
	            } catch (Exception e) {
	                System.err.println("Failed to get embeddings for file: " + file.getName());
	                e.printStackTrace();
	                // Skip the file and continue with the next one
	            }
	        }
	    }
	
	    // Save updated embeddingsMap to cache file if there are changes
	    if (!cache.equals(embeddingsMap)) {
	        saveCacheToFile(embeddingsMap, saveFile);
	    }
	
	    return embeddingsMap;
	}
	
//	private static Map<File, List<Float>> loadCacheFromFile(File saveFile) {
//	    try {
//	        RandomAccessFile file = new RandomAccessFile(saveFile, "r");
//	        FileChannel channel = file.getChannel();
//	        MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
//	        return (Map<File, List<Float>>) new ObjectInputStream(new ByteArrayInputStream(buffer.array())).readObject();
//	    } catch (Exception e) {
//	        System.out.println("Cache file not found. Creating a new cache.");
//	        return new HashMap<>();
//	    }
//	}
//	
//	private static void saveCacheToFile(Map<File, List<Float>> cache, File saveFile) {
//	    try {
//	        RandomAccessFile file = new RandomAccessFile(saveFile, "rw");
//	        FileChannel channel = file.getChannel();
//	        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//	        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
//	        objectOutputStream.writeObject(cache);
//	        objectOutputStream.flush();
//	        ByteBuffer buffer = ByteBuffer.wrap(byteArrayOutputStream.toByteArray());
//	        channel.write(buffer);
//	        System.out.println("Cache saved to file: " + saveFile.getName());
//	    } catch (Exception e) {
//	        System.out.println("Failed to save cache to file: " + saveFile.getName());
//	    }
//	}


	private static Map<File, List<Float>> loadCacheFromFile(File saveFile) {
	    try {
	        FileInputStream fileInputStream = new FileInputStream(saveFile);
	        ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
	        Map<File, List<Float>> cache = (Map<File, List<Float>>) objectInputStream.readObject();
	        objectInputStream.close();
	        fileInputStream.close();
	        return cache;
	    } catch (Exception e) {
	        System.out.println("Cache file not found. Creating a new cache.");
	        return new HashMap<>(); // Return an empty cache if the file doesn't exist or there's an error
	    }
	}

	private static void saveCacheToFile(Map<File, List<Float>> cache, File saveFile) {
	    try {
	        FileOutputStream fileOutputStream = new FileOutputStream(saveFile);
	        ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
	        objectOutputStream.writeObject(cache);
	        objectOutputStream.close();
	        fileOutputStream.close();
	        System.out.println("Cache saved to file: " + saveFile.getName());
	    } catch (Exception e) {
	        System.out.println("Failed to save cache to file: " + saveFile.getName());
	    }
	}

	
//	private static Map<File, List<Float>> getEmbeddingsMap(List<File> files) {
//		Map<File, List<Float>> embeddingsMap = new HashMap<>();
//	
//		for (File file : files) {
//			System.out.println("Processing file: " + file.getName()); // Print the file name
//			List<String> segments = getSegmentsFromFile(file);
//			List<Float> embeddings = getEmbeddings(segments, apiKey);
//			embeddingsMap.put(file, embeddings);
//		}
//	
//		return embeddingsMap;
//	}
	
	private static Map<File, Float> calculateSimilarity(List<Float> questionEmbeddings, Map<File, List<Float>> embeddingsMap) {
		Map<File, Float> similarityMap = new HashMap<>();
	
		for (Map.Entry<File, List<Float>> entry : embeddingsMap.entrySet()) {
			File file = entry.getKey();
			List<Float> embeddings = entry.getValue();
			float similarity = cosineSimilarity(questionEmbeddings, embeddings);
			similarityMap.put(file, similarity);
		}
	
		return similarityMap;
	}
	
	private static List<File> findNMostSimilarFiles(Map<File, Float> similarityMap, int n, boolean print) {
	    List<File> mostSimilarFiles = new ArrayList<>()
	
	    PriorityQueue<Map.Entry<File, Float>> priorityQueue = new PriorityQueue<>(
	            n,
	            { a, b -> b.value.compareTo(a.value) }
	    )
	
	    similarityMap.each { entry ->
	        priorityQueue.offer(entry)
	        if (priorityQueue.size() > n) {
	            priorityQueue.poll()
	        }
	    }
	
	    while (!priorityQueue.isEmpty()) {
	        File file = priorityQueue.poll().key
	        mostSimilarFiles.add(file)
	    }
	
	    if (print) {
	        if (!mostSimilarFiles.isEmpty()) {
	            println "Most similar files:"
	            mostSimilarFiles.each { file ->
	                println file.getName()
	            }
	        } else {
	            println "No similar files found."
	        }
	    }
	
	    mostSimilarFiles
	}
	
//	private static File findNMostSimilarFiles(Map<File, Float> similarityMap, int n, boolean print) {
//		File mostSimilarFile = null;
//		float highestSimilarity = -1;
//	
//		for (Map.Entry<File, Float> entry : similarityMap.entrySet()) {
//			File file = entry.getKey();
//			float similarity = entry.getValue();
//			if (similarity > highestSimilarity) {
//				highestSimilarity = similarity;
//				mostSimilarFile = file;
//			}
//		}
//		
//		if(print) {
//			if (mostSimilarFile != null) {
//				System.out.println("Most similar file: " + mostSimilarFile.getName());
//			} else {
//				System.out.println("No similar file found.");
//			}
//		}
//	
//		return mostSimilarFile;
//	}
	
	private static float cosineSimilarity(List<Float> a, List<Float> b) {
	    float dotProduct = 0;
	    float magnitudeA = 0;
	    float magnitudeB = 0;
	
	    // Ensure that the lists have the same size
	    int size = Math.min(a.size(), b.size());
	    for (int i = 0; i < size; i++) {
	        dotProduct += a.get(i) * b.get(i);
	        magnitudeA += Math.pow(a.get(i), 2);
	        magnitudeB += Math.pow(b.get(i), 2);
	    }
	
	    magnitudeA = (float) Math.sqrt(magnitudeA);
	    magnitudeB = (float) Math.sqrt(magnitudeB);
	
	    return dotProduct / (magnitudeA * magnitudeB);
	}
	
	private static String QuestionDialog(String defaultQuestion) {
	    String[] question = new String[1];
	
	    BowlerStudio.runLater({
	        TextInputDialog dialog = new TextInputDialog(defaultQuestion);
	        dialog.setTitle("Laputa");
	        dialog.setHeaderText("Laputa will do its best to help you.\nWhat is your question?");
	        dialog.setContentText("Question:");
	
	        Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                question[0] = result.get();
            }
	    });
	
	    // Wait for the user to enter a question
	    while (question[0] == null) {
	        try {
	            Thread.sleep(100);
	        } catch (InterruptedException e) {
	            e.printStackTrace();
	        }
	    }
	
	    return question[0];
	}
	


	private static KeyDialog(File keyFile) {
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
						File myObj = keyFile;
						if (myObj.createNewFile()) {
							System.out.println("File created: " + myObj.getName());
						} else {
							System.out.println("File already exists.");
						}
						FileWriter myWriter = new FileWriter(keyFile);
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

	 private static String constructPrompt(List<File> mostSimilarFiles, String question) {
	    // Extract relevant content from search results
	    List<String> context = new ArrayList<>();
	    for (File file : mostSimilarFiles) {
	        try {
	            String fileContent = readFileContent(file);
	            context.add(fileContent);
	        } catch (IOException e) {
	            // Handle file read error, if needed
	        }
	    }
	
	    // Glue the extracted content together with the question to form the prompt
	    StringBuilder promptBuilder = new StringBuilder();
		StringBuilder larp = new StringBuilder();
		larp.append("You are a helpful software engineer who is excited to help folks create things they enjoy!");
		larp.append("Given the following sections from the BowlerStudio documentation, ");
		larp.append("answer the question using only that information, outputted in markdown format.");
		larp.append("If you are unsure and the answer is not explicitly written in the documentation, ");
		larp.append("say Sorry, I don't know how to help with that.");
		promptBuilder.append(larp)
	    promptBuilder.append("Context:");
		//promptBuilder.append("\n```\n");
	    for (String fileContent : context) {
	        promptBuilder.append(fileContent).append("\n");
	    }
	    //promptBuilder.append("```\n");
	    promptBuilder.append("Given the above context, answer the following question using markdown format")
		//promptBuilder.append("\n```\n");
	    promptBuilder.append(question);
		//promptBuilder.append("\n```\n");
	
	    return promptBuilder.toString();
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


public class ModelAPIClient {
	private static final String MODELS_API_URL = "https://api.openai.com/v1/models"
	private static final String EMBEDDING_API_URL = "https://api.openai.com/v1/engines/davinci/calculate_embeddings";
	private static final String CHAT_API_URL = "https://api.openai.com/v1/engines/davinci/completions";
	private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json");

	private final String apiKey;
	private final OkHttpClient client;
	private final Gson gson;

	public ModelAPIClient(String apiKey) {
		this.apiKey = apiKey;
		this.client = new OkHttpClient();
		this.gson = new GsonBuilder().create();
	}
	
	public String getModels() throws IOException {
		Request request = createRequestWithAuthorizationHeader(MODELS_API_URL, null);
		Response response = client.newCall(request).execute();
		if (!response.isSuccessful()) {
			throw new IOException("Failed to get models: " + response);
		}

		String responseBody = response.body().string();
		ModelsResponse modelsResponse = gson.fromJson(responseBody, ModelsResponse.class);
		return modelsResponse.toString();
	}
	
	private static class ModelsResponse {
        private final List<ModelInfo> data;

        public ModelsResponse(List<ModelInfo> data) {
            this.data = data;
        }

        public List<ModelInfo> getData() {
            return data;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (ModelInfo modelInfo : data) {
                sb.append("Model ID: ").append(modelInfo.getId()).append("\n");
                sb.append("Owned By: ").append(modelInfo.getOwnedBy()).append("\n");
				/*
				 * sb.append("Permissions:\n"); for (Permission permission :
				 * modelInfo.getPermissions()) {
				 * sb.append("  Permission ID: ").append(permission.getId()).append("\n");
				 * sb.append("  Allow Create Engine: ").append(permission.isAllowCreateEngine())
				 * .append("\n");
				 * sb.append("  Allow Sampling: ").append(permission.isAllowSampling()).append(
				 * "\n");
				 * sb.append("  Allow Logprobs: ").append(permission.isAllowLogprobs()).append(
				 * "\n");
				 * sb.append("  Allow Search Indices: ").append(permission.isAllowSearchIndices(
				 * )).append("\n");
				 * sb.append("  Allow View: ").append(permission.isAllowView()).append("\n");
				 * sb.append("  Allow Fine Tuning: ").append(permission.isAllowFineTuning()).
				 * append("\n");
				 * sb.append("  Organization: ").append(permission.getOrganization()).append(
				 * "\n"); sb.append("  Group: ").append(permission.getGroup()).append("\n");
				 * sb.append("  Is Blocking: ").append(permission.isBlocking()).append("\n");
				 * sb.append("\n"); }
				 */
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    private static class ModelInfo {
        private final String id;
        private final String owned_by;
        private final List<Permission> permission;

        public ModelInfo(String id, String owned_by, List<Permission> permission) {
            this.id = id;
            this.owned_by = owned_by;
            this.permission = permission;
        }

        public String getId() {
            return id;
        }

        public String getOwnedBy() {
            return owned_by;
        }

        public List<Permission> getPermissions() {
            return permission;
        }
    }

    private static class Permission {
        private final String id;
        private final boolean allow_create_engine;
        private final boolean allow_sampling;
        private final boolean allow_logprobs;
        private final boolean allow_search_indices;
        private final boolean allow_view;
        private final boolean allow_fine_tuning;
        private final String organization;
        private final String group;
        private final boolean is_blocking;

        public Permission(String id, boolean allow_create_engine, boolean allow_sampling,
                          boolean allow_logprobs, boolean allow_search_indices, boolean allow_view,
                          boolean allow_fine_tuning, String organization, String group, boolean is_blocking) {
            this.id = id;
            this.allow_create_engine = allow_create_engine;
            this.allow_sampling = allow_sampling;
            this.allow_logprobs = allow_logprobs;
            this.allow_search_indices = allow_search_indices;
            this.allow_view = allow_view;
            this.allow_fine_tuning = allow_fine_tuning;
            this.organization = organization;
            this.group = group;
            this.is_blocking = is_blocking;
        }

        public String getId() {
            return id;
        }

        public boolean isAllowCreateEngine() {
            return allow_create_engine;
        }

        public boolean isAllowSampling() {
            return allow_sampling;
        }

        public boolean isAllowLogprobs() {
            return allow_logprobs;
        }

        public boolean isAllowSearchIndices() {
            return allow_search_indices;
        }

        public boolean isAllowView() {
            return allow_view;
        }

        public boolean isAllowFineTuning() {
            return allow_fine_tuning;
        }

        public String getOrganization() {
            return organization;
        }

        public String getGroup() {
            return group;
        }

        public boolean isBlocking() {
            return is_blocking;
        }
    }

	public List<List<Float>> callEmbeddingAPI(String[] segment) throws IOException {
		String jsonBody = gson.toJson(new EmbeddingRequest(segment));
		RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
		Request request = createRequestWithAuthorizationHeader(EMBEDDING_API_URL, body);
		Response response = client.newCall(request).execute()
		if (!response.isSuccessful()) {
			throw new IOException("Failed to call embedding API: " + response);
		}

		String responseBody = response.body().string();
		EmbeddingResponse embeddingResponse = gson.fromJson(responseBody, EmbeddingResponse.class);
		return embeddingResponse.getEmbeddings();
	}

	public String callChatAPI(String prompt) throws IOException {
		String jsonBody = gson.toJson(new ChatRequest(prompt));
		RequestBody body = RequestBody.create(jsonBody, JSON_MEDIA_TYPE);
		Request request = createRequestWithAuthorizationHeader(CHAT_API_URL, body);
	    Response response = client.newCall(request).execute()
	    if (!response.isSuccessful()) {
	        throw new IOException("Failed to call chat API: " + response);
	    }
	
	    String responseBody = response.body().string();
	    ChatResponse chatResponse = gson.fromJson(responseBody, ChatResponse.class);
	    return chatResponse.getAnswer();
	}

	private Request createRequestWithAuthorizationHeader(String url, RequestBody body) {
	    Request.Builder requestBuilder = new Request.Builder()
	            .url(url)
	            .header("Authorization", "Bearer " + apiKey);
	
	    if (body != null) {
	        requestBuilder.post(body);
	    } else {
	        requestBuilder.get();
	    }
	
	    return requestBuilder.build();
	}

	private static class EmbeddingRequest {
		private final String text;

		public EmbeddingRequest(String text) {
			this.text = text;
		}
	}

	private static class EmbeddingResponse {
		private final List<List<Float>> embeddings;

		public EmbeddingResponse(List<List<Float>> embeddings) {
			this.embeddings = embeddings;
		}

		public List<List<Float>> getEmbeddings() {
			return embeddings;
		}
	}

	private static class ChatRequest {
		private final String prompt;

		public ChatRequest(String prompt) {
			this.prompt = prompt;
		}
	}

	private static class ChatResponse {
		private final String answer;

		public ChatResponse(String answer) {
			this.answer = answer;
		}

		public String getAnswer() {
			return answer;
		}
	}
}



//public class ModelAPIClient {
//    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
//    private static final String EMBEDDING_API_URL = "https://api.openai.com/v1/embeddings";
//    private static final int EMBEDDING_SIZE = 1536;
//
//    private static String callOpenAIAPI(String url, String data) throws IOException {
//        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
//        connection.setRequestMethod("POST");
//        connection.setRequestProperty("Content-Type", "application/json");
//        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
//        connection.setDoOutput(true);
//
////		System.out.println("Sending to API: " + data); // Print the response JSON
//		connection.getOutputStream().write(data.getBytes());
//
//        int responseCode = connection.getResponseCode();
//
//        BufferedReader reader;
//        if (responseCode >= 400) {
//            reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//        } else {
//            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//        }
//
//        StringBuilder response = new StringBuilder();
//        String line;
//        while ((line = reader.readLine()) != null) {
//            response.append(line);
//        }
//        reader.close();
//
//        String resp = response.toString();
////		System.out.println("Response JSON: " + resp); // Print the response JSON
//
//        return resp;
//    }
//
//    public static String callChatAPI(String prompt) throws IOException {
//		def maxTokens = 100;prompt = "[stuff]";
//		String[] messages = {prompt}; // Create an array with the prompt as the only element
//		String data = "{\"messages\": " + Arrays.toString(messages) + ", \"max_tokens\": " + maxTokens + ", \"model\": \"gpt-3.5-turbo-0613\"}";
//		String jsonResponse = callOpenAIAPI(OPENAI_API_URL, data, apiKey);
//		System.out.println("Response JSON: " + jsonResponse)
//		String response = parseOutputText(jsonResponse);
//		System.out.println("Parsed response: " + response)
//        return response;
//	}
//
//    public static List<List<Float>> callEmbeddingAPI(String[] inputs) throws IOException {
//        String data = "{\"input\": " + buildInputJson(inputs) + ", \"model\": \"text-embedding-ada-002\"}";
//        String response = callOpenAIAPI(EMBEDDING_API_URL, data, apiKey);
//
//        // Extract the embeddings from the response
//        List<List<Float>> embeddings = extractEmbeddings(response);
//
//        return embeddings;
//    }
//
//    private static List<List<Float>> extractEmbeddings(String response) {
//        try {
//            JSONObject jsonResponse = new JSONObject(response);
//            JSONArray data = jsonResponse.getJSONArray("data");
//
//            List<List<Float>> embeddings = new ArrayList<>();
//
//            for (int i = 0; i < data.length(); i++) {
//                JSONObject embeddingObj = data.getJSONObject(i);
//                JSONArray embeddingArray = embeddingObj.getJSONArray("embedding");
//
//                List<Float> embedding = new ArrayList<>();
//
//                for (int j = 0; j < embeddingArray.length(); j++) {
//                    float value = (float) embeddingArray.getDouble(j);
//                    embedding.add(value);
//                }
//
//                embeddings.add(embedding);
//            }
//
//            return embeddings;
//        } catch (JSONException e) {
//            throw new IllegalArgumentException("Invalid response format: " + e.getMessage());
//        }
//    }
//
//    private static String escapeNewLines(String text) {
//        return text.replace("\n", "\\n");
//    }
//
//	private static String scrubData(String data) {
//		String cleanedData = cleanHtml(data);
//		cleanedData = cleanSpecialCharacters(cleanedData);
//		cleanedData = escapeQuotes(cleanedData)
//		return cleanedData;
//	}
//	
//    private static String cleanHtml(String html) {
//        Document dirtyDocument = Jsoup.parse(html);
//        Document cleanDocument = new Cleaner(Whitelist.relaxed()).clean(dirtyDocument);
//        return cleanDocument.body().html();
//    }
//
//	private static String cleanSpecialCharacters(String text) {
//		def ret = StringUtils.remove(text, '-'); // Remove "-"
////		ret = StringUtils.remove(ret, ':'); // Remove ":"
//	    return ret
//	}
//	
//	private static String escapeQuotes(String input) {
//		return input.replace("\"", "\\\"");
//	}
//	
//    private static String buildInputJson(String[] inputs) {
//        StringBuilder json = new StringBuilder();
//        for (int i = 0; i < inputs.length; i++) {
//        def str = scrubData(inputs[i])
//
//        // Skip empty or whitespace entries
//        if (str == null || str.trim().isEmpty()) {
//            continue;
//        }
//        if (json.length() > 0) {
//            json.append(", ");
//        }
//        json.append("\"").append(str).append("\"");
//		}
//        json.insert(0, "[").append("]");
//        return json.toString();
//    }
//	
//	public static String parseOutputText(String jsonResponse) throws IOException {
//		ObjectMapper objectMapper = new ObjectMapper();
//		JsonNode rootNode = objectMapper.readTree(jsonResponse);
//		JsonNode choicesNode = rootNode.path("choices");
//	
//		if (choicesNode.isArray() && choicesNode.size() > 0) {
//			JsonNode textNode = choicesNode.get(0).path("text");
//			if (textNode.isTextual()) {
//				return textNode.asText();
//			}
//		}
//	
//		return "";
//	}
//}

public class MarkdownDownloader {
    private CloseableHttpClient httpClient;

    public MarkdownDownloader(String targetSite, Path savePath) {
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

    private void downloadMarkdownFiles(String targetSite, Path savePath) throws IOException, URISyntaxException {
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

	private void downloadFile(String fileUrl, Path savePath) throws IOException {
	    String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
	    Path saveDirectory = savePath;
	    if (!Files.exists(saveDirectory)) {
	        Files.createDirectories(saveDirectory);
	    }
	    File outputFile = savePath + File.separator + fileName;
	
	    // Check if the file already exists
	    if (Files.exists(outputFile)) {
	        System.out.println("Skipped: " + fileName);
	        return;
	    }
	
//	    // Check if the file already exists and looks unchanged
//	    if (Files.exists(outputFile) && isFileUnchanged(outputFile, fileUrl)) {
//	        System.out.println("Skipped: " + fileName);
//	        return;
//	    }
	
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
