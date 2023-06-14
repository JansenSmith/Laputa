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
        System.out.println("Prompt:: " + prompt);

        // Step 3: Call the OpenAI API with the prompt
        String answer = callOpenAIAPI(prompt, apiKey);

        // Step 4: Process and display the answer
        String processedAnswer = processAnswer(answer);
        System.out.println("Answer: " + processedAnswer);
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


	private static String callOpenAIAPI(String prompt, String apiKey) throws IOException {
		URL url = new URL("https://api.openai.com/v1/engines/davinci-codex/completions");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
		connection.setDoOutput(true);
		
		//String data = "{\"prompt\": \"" + prompt + "\", \"max_tokens\": 100}";
	    String promptWithEscapes = prompt.replace("\n", "\\n");
	    String data = "{\"prompt\": \"" + promptWithEscapes + "\"}";
	    System.out.println("JSON Data: " + data);
	
	    // Validate the JSON
	    try {
	        new JsonSlurper().parseText(data);
	        System.out.println("JSON is valid.");
	    } catch (Exception e) {
	        System.out.println("JSON is invalid. Error: " + e.getMessage());
	    }
	
	    connection.getOutputStream().write(data.getBytes());
		
	    int responseCode = connection.getResponseCode();
	    System.out.println("Response Code: " + responseCode);
		
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
	
	    	System.out.println("Response Body: " + response.toString());

		return response.toString();
	}


    private static String processAnswer(String answer) {
        // Extract and process the answer from the API response
        // Return the processed answer as a string
        return "Processed answer: " + answer;
    }
}
