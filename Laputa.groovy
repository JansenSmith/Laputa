import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.neuronrobotics.bowlerstudio.BowlerKernel
import com.neuronrobotics.bowlerstudio.BowlerStudio
import com.neuronrobotics.bowlerstudio.BowlerStudioController
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine
import javafx.scene.control.TextInputDialog
import java.nio.file.Files
import java.nio.file.Paths
import groovy.json.JsonSlurper

// inspired by https://simonwillison.net/2023/Jan/13/semantic-search-answers/

public class QAPatternExample {

    public static void main(String[] args) throws IOException {
        String question = "What does shot scraper do?";
        String keyLocation = ScriptingEngine.getWorkspace().getAbsolutePath()+"/gpt-key.txt"
		if(!new File(keyLocation).exists()) {
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
			return;
		}
		
		
		println "Loading API key from "+keyLocation
		String apiKey = new String(Files.readAllBytes(Paths.get(keyLocation)));
		//println "API key: "+apiKey

        // Step 1: Run a search query to find relevant content
        String searchResults = runSearchQuery(question);

        // Step 2: Extract relevant content and construct the prompt
        String prompt = constructPrompt(searchResults, question);
        System.out.println("\nPrompt: \n" + prompt);

        // Step 3: Call the OpenAI API with the prompt
        String answer = OpenAIAPIClient.callDavinciAPI(prompt, apiKey);

        // Step 4: Process and display the answer
        String processedAnswer = processAnswer(answer);
        System.out.println("\nProcessed Answer: \n" + processedAnswer);
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


	
	public class OpenAIAPIClient {
		private static final String DAVINCI_API_URL = "https://api.openai.com/v1/engines/davinci/completions";
		private static final String EMBEDDING_API_URL = "https://api.openai.com/v1/embeddings";
	
		private static String callOpenAIAPI(String url, String prompt, String apiKey) throws IOException {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setRequestProperty("Authorization", "Bearer " + apiKey);
			connection.setDoOutput(true);
	
			String data = "{\"prompt\": \"" + escapeNewLines(prompt) + "\"}";
	
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
	
			return response.toString();
		}
	
		private static String escapeNewLines(String text) {
			return text.replace("\n", "\\n");
		}
	
		public static String callDavinciAPI(String prompt, String apiKey) throws IOException {
			return callOpenAIAPI(DAVINCI_API_URL, prompt, apiKey);
		}
	
		public static String callEmbeddingAPI(String prompt, String apiKey) throws IOException {
			return callOpenAIAPI(EMBEDDING_API_URL, prompt, apiKey);
		}
	}
	
	
//	private static String callOpenAIAPI(String prompt, String apiKey) throws IOException {
//		URL url = new URL("https://api.openai.com/v1/engines/davinci/completions");
//		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//		connection.setRequestMethod("POST");
//		connection.setRequestProperty("Content-Type", "application/json");
//		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
//		connection.setDoOutput(true);
//		
//		//String data = "{\"prompt\": \"" + prompt + "\", \"max_tokens\": 100}";
//	    String promptWithEscapes = prompt.replace("\n", "\\n");
//	    String data = "{\"prompt\": \"" + promptWithEscapes + "\"}";
//	    //System.out.println("JSON Data: " + data);
//	
//	    // Validate the JSON
//	    try {
//	        new JsonSlurper().parseText(data);
//	        //System.out.println("JSON is valid.");
//	    } catch (Exception e) {
//	        System.out.println("JSON is invalid. Error: " + e.getMessage());
//	    }
//	
//	    connection.getOutputStream().write(data.getBytes());
//		
//	    int responseCode = connection.getResponseCode();
//	    //System.out.println("Response Code: " + responseCode);
//		
//	    BufferedReader reader;
//	    if (responseCode >= 400) {
//	        reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//	    } else {
//	        reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//	    }
//
//		StringBuilder response = new StringBuilder();
//		String line;
//		while ((line = reader.readLine()) != null) {
//			response.append(line);
//		}
//		reader.close();
//	
//	    	//System.out.println("Response Body: " + response.toString());
//
//		return response.toString();
//	}


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
