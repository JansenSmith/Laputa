
@Grapes(
	@Grab(group='com.theokanning.openai-gpt3-java', module='service', version='0.14.0')
)


import com.theokanning.openai.model.Model;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.image.CreateImageRequest;

import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine
import com.neuronrobotics.bowlerstudio.BowlerStudioController
import com.neuronrobotics.sdk.common.DeviceManager
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.awt.image.BufferedImage
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.scene.control.Tab
import javafx.scene.image.Image
import javafx.application.Platform
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;

import javax.imageio.ImageIO;


class OpenAiApiExample {
    public static void main(String... args) {
        boolean shouldDisplayModels = false; // Set to true to run the model display test
        boolean shouldRunTextCompletion = false; // Set to true to run the text completion test
        boolean shouldRunImageGeneration = false; // Set to true to run the image generation test
        boolean shouldStreamChatCompletion = false; // Set to true to run the chat completion streaming test
        boolean shouldRunChatCompletion = true; // Set to true to run the chat completion test
		boolean shouldRunFunction = false; // Set to true to run the function test
		boolean shouldParseFile = false; // Set to true to test parsing a file with NLM
        
		
		//
		// Test 1: Retrieve the user's API key from a local file in bowler-workspace
		File keyFile = new File(ScriptingEngine.getWorkspace(), "gpt-key.txt");
		if (!keyFile.exists()) {
			// If the API key file doesn't exist, prompt the user to enter it
			KeyDialog(keyFile);
			return;
		}
		System.out.println("Loading API key from " + keyFile);
		String apiKey = new String(Files.readAllBytes(keyFile.toPath()));
		OpenAiService service = new OpenAiService(apiKey);
		
		
		//
		// Test 2: List models
		if (shouldDisplayModels) {
		    System.out.println("Listing models...");
		    List<Model> models = service.listModels();
		    for (Model model : models) {
		        System.out.println(model.id);
		    }
		    // assertFalse(models.isEmpty());
		}
		
		
        //
        // Test 3: Create a text completion request
        if (shouldRunTextCompletion) {
            System.out.println("\nCreating completion...");
            CompletionRequest completionRequest = CompletionRequest.builder()
                    .model("ada")
                    .prompt("Somebody once told me the world is gonna roll me")
                    .echo(true)
                    .user("testing")
                    .n(3)
                    .build();
            service.createCompletion(completionRequest).getChoices().forEach{println it};
        }
		
		
        //
        // Test 4: Create an image generation request
        if (shouldRunImageGeneration) {
            System.out.println("\nCreating Image...");
            CreateImageRequest request = CreateImageRequest.builder()
                    .prompt("An orange cow breakdancing with a turtle in a scene from Stomp The Yard, hyper realistic high fidelity")
                    .build();

            System.out.println("\nImage is located at:");
            String imageUrl = service.createImage(request).getData().get(0).getUrl();
            TabManagerDevice tabManager = new TabManagerDevice("ImageRequest");

	        Platform.runLater(new Runnable() {
	            @Override
	            public void run() {
	                ImageView imageView = tabManager.imageView;
	                imageView.setPreserveRatio(true);
	                imageView.setFitHeight(500); // Set the desired height
	                Image image = new Image(imageUrl);
	
	                // Create a timestamp for the image file name
	                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
	                LocalDateTime now = LocalDateTime.now();
	                String timestamp = now.format(formatter);
	
	                // Save the image to the bowler-workspace/ImageGeneration folder with timestamp in the file name
	                String imageName = "image_" + timestamp + ".png";
	                String workspacePath = ScriptingEngine.getWorkspace();
	                String folderPath = Paths.get(workspacePath, "ImageGeneration").toString();
	                File folder = new File(folderPath);
	                String imagePath = Paths.get(folderPath, imageName).toString();
	                System.out.println(imagePath);
	
	                // Check if the folder exists, and create it if it doesn't
	                if (!folder.exists()) {
	                    folder.mkdirs();
	                }
	
	                File imageFile = new File(imagePath);
	
	                FileOutputStream fos = null;
	                try {
	                    fos = new FileOutputStream(imageFile);
	
	                    // Save the image to the file
	                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
	                    ImageIO.write(bufferedImage, "png", fos);
	                    System.out.println("Image saved to: " + imageFile.getAbsolutePath());
	
	                    // Display the saved image
	                    imageView.setImage(new Image(imageFile.toURI().toString()));
	                    tabManager.connect();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                } finally {
	                    if (fos != null) {
	                        try {
	                            fos.close();
	                        } catch (IOException e) {
	                            e.printStackTrace();
	                        }
	                    }
	                }
	            }
	        });

            System.out.println(imageUrl);
        }
		
		
		//
		// Test 5: Stream a chat completion request
		if (shouldStreamChatCompletion) {
		    println("Streaming chat completion...")
		    final messages = []
		    final systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are a fortune teller. Tell me my fortune.")
		    messages.add(systemMessage)
		    final chatCompletionRequest = ChatCompletionRequest
		            .builder()
		            .model("gpt-3.5-turbo-0613")
		            .messages(messages)
		            .n(1)
		            .maxTokens(50)
		            .logitBias(new HashMap<>())
		            .build()
		
		    def messageContent = new StringBuilder()
			println("Current message content:")
		
		    service.streamChatCompletion(chatCompletionRequest).blockingForEach { chunk ->
		        chunk.getChoices().each { choice ->
		            def message = choice.getMessage()
		            if (message != null && message.getContent() != null) {
		                messageContent.append(message.getContent())
		                print("${message.getContent().toString().trim()} ")
		                System.out.flush()
		            }
		        }
		    }
			
			println("")
		    println("\nFinal message content:")
			println("${messageContent.toString().trim()}")
		}


		
		
        //
        // Test 6: Run a chat completion request
		// for implementation details, see https://github.com/TheoKanning/openai-java/blob/c047f73c9cdb4d14b9f88fd554cb1339ec61e78b/api/src/main/java/com/theokanning/openai/completion/chat/ChatCompletionRequest.java#L16
        if (shouldRunChatCompletion) {
		    println("Running chat completion...")
		    final messages = []
		    final systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are a fortune teller. Tell me my fortune.")
		    messages.add(systemMessage)
			
			ChatCompletionRequest completionRequest = ChatCompletionRequest
				.builder()
				.model("gpt-3.5-turbo-0613")
				.messages(messages)
				.temperature(0.6)
				.n(3)
				.stream(false)
                .maxTokens(100)
				.logitBias(new HashMap<>())
				.build();
				
			ChatMessage responseMessage = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage();
			messages.add(responseMessage); // don't forget to update the conversation with the latest response
			String response = responseMessage.getContent()
			
		    println("\nResponse:")
			println("${response.toString().trim()}")
        }
		
		//
		// Bookkeeping: shutdown the service executor 
		service.shutdownExecutor();
    }
}

class TabManagerDevice{
	String myName;
	boolean connected=false;
	ImageView imageView = new ImageView();
	Tab t = new Tab()
	public TabManagerDevice(String name) {
		myName=name;
		
	}
	
	String getName() {
		return myName
	}
	
	boolean connect() {
		connected=true;
		t.setContent(imageView)
		t.setText(myName)
		t.setOnCloseRequest({event ->
			disconnect()
		});
		BowlerStudioController.addObject(t, null)
		return connected
	}
	void disconnect() {
		if(connected) {
			BowlerStudioController.removeObject(t)
		}
		
	}
}


