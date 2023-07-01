@Grapes(
	@Grab(group='com.theokanning.openai-gpt3-java', module='client', version='0.14.0')
)

//package example;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.CompletionRequest;
import com.theokanning.openai.image.CreateImageRequest;

import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine
import java.nio.file.Files

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class OpenAiApiExample {
    public static void main(String... args) {		
		// Step 1: Retrieve the user's API key from a local file in bowler-workspace
		File keyFile = new File(ScriptingEngine.getWorkspace(), "gpt-key.txt");
		if (!keyFile.exists()) {
			// If the API key file doesn't exist, prompt the user to enter it
		    KeyDialog(keyFile);
		    return;
		}
		System.out.println("Loading API key from " + keyFile);
		String apiKey = new String(Files.readAllBytes(keyFile.toPath()));
		OpenAiService service = new OpenAiService(apiKey);
		
		System.out.println("\nCreating completion...");
		CompletionRequest completionRequest = CompletionRequest.builder()
		      .model("ada")
		      .prompt("Somebody once told me the world is gonna roll me")
		      .echo(true)
		      .user("testing")
		      .n(3)
		      .build();
		service.createCompletion(completionRequest).getChoices().forEach{println it};
		
		System.out.println("\nCreating Image...");
		CreateImageRequest request = CreateImageRequest.builder()
		      .prompt("A cow breakdancing with a turtle")
		      .build();
		
		System.out.println("\nImage is located at:");
		System.out.println(service.createImage(request).getData().get(0).getUrl());
		
		System.out.println("Streaming chat completion...");
		final List<ChatMessage> messages = new ArrayList<>();
		final ChatMessage systemMessage = new ChatMessage(ChatMessageRole.SYSTEM.value(), "You are a dog and will speak as such.");
		messages.add(systemMessage);
		ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
		      .builder()
		      .model("gpt-3.5-turbo")
		      .messages(messages)
		      .n(1)
		      .maxTokens(50)
		      .logitBias(new HashMap<>())
		      .build();
		
		service.streamChatCompletion(chatCompletionRequest)
		    //  .doOnError({Throwable::printStackTrace})
		      //.blockingForEach(System.out::println);
		
		service.shutdownExecutor();
	}
}