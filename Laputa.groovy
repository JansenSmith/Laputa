import java.io.*;
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import groovy.json.JsonSlurper
import org.apache.commons.codec.binary.Base64

// inspired by https://simonwillison.net/2023/Jan/13/semantic-search-answers/

public class QAPatternExample {

    public static void main(String[] args) throws IOException {
        String question = "What is a CSG file?";
		
		// Look for or save OpenAI API key
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

        // Step 3: Call the OpenAI API with embeddings
        String[] inputs = ["input1", "input2", "input3"];

        List<Float> embeddings = OpenAIAPIClient.callEmbeddingAPI(inputs, apiKey);

        System.out.println("Embeddings:");
        for (Float embedding : embeddings) {
            System.out.println(embedding);
        }
//        String answer = OpenAIAPIClient.callDavinciAPI(prompt, apiKey);

        // Step 4: Process and display the answer
//        String processedAnswer = processAnswer(answer);
//        System.out.println("\nProcessed Answer: \n" + processedAnswer);
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

        String resp = response.toString()

//	    System.out.println("API Response:");
//	    System.out.println(resp);

        return resp;
    }

    public static String callDavinciAPI(String prompt, String apiKey) throws IOException {
        String data = "{\"prompt\": \"" + escapeNewLines(prompt) + "\"}";
        return callOpenAIAPI(DAVINCI_API_URL, data, apiKey);
    }

    public static List<Float> callEmbeddingAPI(String[] inputs, String apiKey) throws IOException {
        String data = "{\"input\": " + buildInputJson(inputs) + ", \"model\": \"text-embedding-ada-002\"}";
        String response = callOpenAIAPI(EMBEDDING_API_URL, data, apiKey);

	    //System.out.println("API Response:");
	    //System.out.println(response);

        // Extract the binary string from the response
        String binaryString = extractBinaryString(response);
//		System.out.println("Binary String:");
//		System.out.println(binaryString);

        // Decode the binary string to obtain the list of floating-point numbers
        List<Float> embeddings = decodeBinaryString(binaryString);

        return embeddings;
    }

	private static String extractBinaryString(String response) {
	    try {
	        // Find the start and end index of the binary string
	        int startIndex = response.indexOf("\"embedding\": [") + 14;
	        int endIndex = response.indexOf("]", startIndex);
	
	        // Check if start and end indices are valid
	        if (startIndex == -1 || endIndex == -1) {
	            throw new IllegalArgumentException("Invalid response format: missing \"embedding\" field or closing bracket.");
	        }
	
	        // Print diagnostic information
	        System.out.println("Start Index: " + startIndex);
	        System.out.println("End Index: " + endIndex);
	
	        // Extract the substring
	        String binaryString = response.substring(startIndex, endIndex);
	
	        // Remove non-digit characters from the binary string
	        // binaryString = binaryString.replaceAll("[^\\d.-]+", "");
	
	        return binaryString;
	    } catch (IndexOutOfBoundsException e) {
	        throw new IllegalArgumentException("Invalid response format: unexpected index out of bounds.", e);
	    }
	}




	private static List<Float> decodeBinaryString(String binaryString) {
	    byte[] binaryData = binaryString.decodeBase64()
	    ByteBuffer buffer = ByteBuffer.wrap(binaryData)
	    buffer.order(ByteOrder.LITTLE_ENDIAN)
	
	    List<Float> embeddings = []
	    while (buffer.hasRemaining()) {
	        embeddings.add(buffer.getFloat())
	    }
	    return embeddings
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

	
//public class OpenAIAPIClient {
//	private static final String DAVINCI_API_URL = "https://api.openai.com/v1/engines/davinci/completions";
//	private static final String EMBEDDING_API_URL = "https://api.openai.com/v1/embeddings";
//
//	private static String callOpenAIAPI(String url, String prompt, String apiKey) throws IOException {
//		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
//		connection.setRequestMethod("POST");
//		connection.setRequestProperty("Content-Type", "application/json");
//		connection.setRequestProperty("Authorization", "Bearer " + apiKey);
//		connection.setDoOutput(true);
//
//		String data = "{\"prompt\": \"" + escapeNewLines(prompt) + "\"}";
//
//		connection.getOutputStream().write(data.getBytes());
//
//		int responseCode = connection.getResponseCode();
//
//		BufferedReader reader;
//		if (responseCode >= 400) {
//			reader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
//		} else {
//			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
//		}
//
//		StringBuilder response = new StringBuilder();
//		String line;
//		while ((line = reader.readLine()) != null) {
//			response.append(line);
//		}
//		reader.close();
//
//		return response.toString();
//	}
//
//	private static String escapeNewLines(String text) {
//		return text.replace("\n", "\\n");
//	}
//
//	public static String callDavinciAPI(String prompt, String apiKey) throws IOException {
//		return callOpenAIAPI(DAVINCI_API_URL, prompt, apiKey);
//	}
//
//	public static String callEmbeddingAPI(String prompt, String apiKey) throws IOException {
//		return callOpenAIAPI(EMBEDDING_API_URL, prompt, apiKey);
//	}
//}