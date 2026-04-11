package ai.koog.prompt.executor.clients.mistralai.models

import ai.koog.prompt.executor.clients.openai.base.models.OpenAIAudio
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIBaseLLMRequest
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIBaseLLMResponse
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIBaseLLMStreamResponse
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIChoiceLogProbs
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIResponseFormat
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIStaticContent
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIStreamChoice
import ai.koog.prompt.executor.clients.openai.base.models.OpenAITool
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIToolCall
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIToolChoice
import ai.koog.prompt.executor.clients.openai.base.models.OpenAIWebUrlCitation
import ai.koog.prompt.executor.clients.serialization.AdditionalPropertiesFlatteningSerializer
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.buildSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlin.jvm.JvmInline

/**
 * Mistral AI Chat Completion Request
 *
 * @property model ID of the model to use.
 * @property temperature What sampling temperature to use, we recommend between 0.0 and 0.7.
 * Higher values like 0.7 will make the output more random,
 * while lower values like 0.2 will make it more focused and deterministic.
 * We generally recommend altering this or [topP] but not both.
 * The default value varies depending on the model you are targeting.
 * @property topP Nucleus sampling, where the model considers the results of the tokens with `topP` probability mass.
 * So 0.1 means only the tokens comprising the top 10% probability mass are considered.
 * We generally recommend altering this or [temperature] but not both.
 * @property maxTokens The maximum number of tokens to generate in the completion.
 * The token count of your prompt plus `maxTokens` cannot exceed the model's context length.
 * @property stream Whether to stream back partial progress.
 * If set, tokens will be sent as data-only server-side events as they become available,
 * with the stream terminated by a data: [DONE] message.
 * Otherwise, the server will hold the request open until the timeout or until completion,
 * with the response containing the full result as JSON.
 * @property stop Stop generation if this token is detected.
 * Or if one of these tokens is detected when providing an array
 * @property randomSeed The seed to use for random sampling.
 * If set, different calls will generate deterministic results.
 * @property messages The prompt(s) to generate completions for, encoded as a list of dict with role and content.
 * @property responseFormat
 * @property tools
 * @property toolChoice
 * @property presencePenalty determines how much the model penalizes the repetition of words or phrases.
 * A higher presence penalty encourages the model to use a wider variety of words and phrases,
 * making the output more diverse and creative.
 * @property frequencyPenalty penalizes the repetition of words based on their frequency in the generated text.
 * A higher frequency penalty discourages the model from repeating words that have already appeared frequently in the output,
 * promoting diversity and reducing repetition.
 * @property numberOfChoices Number of completions to return for each request, input tokens are only billed once.
 * @property prediction Enable users to specify expected results,
 * optimizing response times by leveraging known or predictable content.
 * This approach is especially effective for updating text documents or code files with minimal changes,
 * reducing latency while maintaining high-quality results.
 * @property parallelToolCalls
 * @property promptMode Allows toggling between the reasoning mode and no system prompt.
 * When set to `reasoning` the system prompt for reasoning models will be used.
 * @property safePrompt Whether to inject a safety prompt before all conversations.
 */
@Serializable
internal class MistralAIChatCompletionRequest(
    override val model: String,
    override val temperature: Double? = null,
    override val topP: Double? = null,
    val maxTokens: Int? = null,
    override val stream: Boolean? = null,
    val stop: List<String>? = null,
    val randomSeed: Int? = null,
    val messages: List<MistralAIMessage>,
    val responseFormat: OpenAIResponseFormat? = null,
    val tools: List<OpenAITool>? = null,
    val toolChoice: OpenAIToolChoice? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null,
    @SerialName("n")
    val numberOfChoices: Int? = null,
    val prediction: OpenAIStaticContent? = null,
    val parallelToolCalls: Boolean? = null,
    val promptMode: String? = null,
    val safePrompt: Boolean? = null,
    override val topLogprobs: Int? = null,
    val additionalProperties: Map<String, kotlinx.serialization.json.JsonElement>? = null,
) : OpenAIBaseLLMRequest

/**
 * Mistral AI Chat Completion Response
 *
 */
@Serializable
public class MistralAIChatCompletionResponse(
    override val id: String,
    @SerialName("object")
    public val objectType: String,
    override val model: String,
    public val usage: MistralAIUsage,
    override val created: Long,
    public val choices: List<MistralAIChoice>
) : OpenAIBaseLLMResponse

/**
 * Represents a completion choice in the MistralAI chat completion API.
 *
 * @property finishReason The reason the model stopped generating tokens.
 * This will be `stop` if the model hit a natural stop point or a provided stop sequence,
 * `length` if the maximum number of tokens specified in the request was reached,
 * `content_filter` if content was omitted due to a flag from our content filters,
 * `tool_calls` if the model called a tool, or `function_call` (deprecated) if the model called a function.
 * @property index The index of the choice in the list of choices.
 * @property logprobs Log probability information for the choice.
 * @property message A chat completion message generated by the model.
 *
 * See [choices](https://platform.openai.com/docs/api-reference/chat/object#chat/object-choices)
 */
@Serializable
public class MistralAIChoice(
    public val finishReason: String,
    public val index: Int,
    public val logprobs: OpenAIChoiceLogProbs? = null,
    public val message: MistralAIMessage,
)

/**
 * Represents a message in the MistralAI chat completion API.
 *
 * Each message type has specific roles and capabilities:
 * - [Developer]
 * - [System]
 * - [User]
 * - [Assistant]
 * - [Tool]
 */
@Serializable
@JsonClassDiscriminator("role")
public sealed interface MistralAIMessage {
    /** The content of the message. */
    public val content: MistralAIContent?

    /**
     * Developer-provided instructions that the model should follow,
     * regardless of messages sent by the user.
     * With o1 models and newer, `developer` messages replace the previous `system` messages.
     *
     * @property content The contents of the developer message. For developer messages, only type text is supported.
     * @property name An optional name for the participant.
     * Provides the model information to differentiate between participants of the same role.
     */
    @Serializable
    @SerialName("developer")
    public class Developer(override val content: MistralAIContent, public val name: String? = null) : MistralAIMessage

    /**
     * Developer-provided instructions that the model should follow, regardless of messages sent by the user.
     * With o1 models and newer, use developer messages for this purpose instead.
     *
     * @property content The contents of the system message. For system messages, only type text is supported.
     * @property name An optional name for the participant.
     * Provides the model information to differentiate between participants of the same role.
     */
    @Serializable
    @SerialName("system")
    public class System(override val content: MistralAIContent, public val name: String? = null) : MistralAIMessage

    /**
     * Messages sent by an end user, containing prompts or additional context information.
     *
     * @property content The contents of the user message.
     * @property name An optional name for the participant.
     * Provides the model information to differentiate between participants of the same role.
     */
    @Serializable
    @SerialName("user")
    public class User(override val content: MistralAIContent, public val name: String? = null) : MistralAIMessage

    /**
     * Messages sent by the model in response to user messages.
     *
     * @property audio Data about a previous audio response from the model.
     * @property content The contents of the assistant message.
     * Required unless `[toolCalls]` or `function_call` is specified.
     * @property name An optional name for the participant.
     * Provides the model information to differentiate between participants of the same role.
     * @property refusal The refusal message by the assistant.
     * @property toolCalls The tool calls generated by the model, such as function calls.
     */
    @Serializable
    @SerialName("assistant")
    public class Assistant(
        override val content: MistralAIContent? = null,
        public val reasoningContent: String? = null,
        public val audio: OpenAIAudio? = null,
        public val name: String? = null,
        public val refusal: String? = null,
        public val toolCalls: List<OpenAIToolCall>? = null,
        public val annotations: List<OpenAIWebUrlCitation>? = null,
    ) : MistralAIMessage

    /**
     * @property content The contents of the tool message. For tool messages, only type text is supported.
     * @property toolCallId Tool call that this message is responding to.
     */
    @Serializable
    @SerialName("tool")
    public class Tool(override val content: MistralAIContent, public val toolCallId: String) : MistralAIMessage
}

/**
 * Represents the content of a message in the MistralAI chat completion API.
 * Can be either a simple text message or a complex content consisting of multiple parts.
 *
 * This sealed interface has two implementations:
 * - [Text] - For simple text content
 * - [Parts] - For complex content containing multiple parts (text, images, audio, files)
 */
@Serializable(with = MistralAIContentSerializer::class)
public sealed interface MistralAIContent {
    /** The simple text content of the message. */
    public fun text(): String

    /**
     * The contents of the message.
     */
    @Serializable
    @JvmInline
    public value class Text(public val value: String) : MistralAIContent {
        override fun text(): String = value
    }

    /**
     * An array of content parts with a defined type.
     * Supported options differ based on the model being used to generate the response.
     * Can contain text, image or audio inputs.
     */
    @Serializable
    @JvmInline
    public value class Parts(public val value: List<MistralAIContentPart>) : MistralAIContent {
        override fun text(): String = value
            .filterIsInstance<MistralAIContentPart.Text>()
            .joinToString("\n") { it.text }
    }
}

/**
 * Represents a content part that can be used within MistralAI chat completion APIs.
 * This is a sealed interface that defines various types of content components,
 * like text, images, audio, and files, enabling diverse inputs and outputs in the API.
 */
@Serializable
@JsonClassDiscriminator("type")
public sealed interface MistralAIContentPart {

    /**
     * Text content part in the MistralAI chat completion API.
     *
     *
     * @property text The text content.
     */
    @Serializable
    @SerialName("text")
    public class Text(public val text: String) : MistralAIContentPart

    /**
     * Image content part in the MistralAI chat completion API.
     *
     * @property imageUrl Contains the URL of the image and optional descriptive details about the image
     */
    @Serializable
    @SerialName("image_url")
    public class Image(public val imageUrl: ImageUrl) : MistralAIContentPart

    /**
     * An image URL configuration for image content in the MistralAI chat completion API.
     *
     * @property url Either a URL of the image or the base64 encoded image data.
     * @property detail Specifies the detail level of the image.
     */
    @Serializable
    public class ImageUrl(public val url: String, public val detail: String? = null)

    /**
     * Audio content part in the MistralAI chat completion API.
     *
     * @property inputAudio Contains the encoded audio data and format information.
     * The audio data should be base64 encoded and the format can be either "wav" or "mp3"
     */
    @Serializable
    @SerialName("input_audio")
    public class Audio(public val inputAudio: InputAudio) : MistralAIContentPart

    /**
     * Represents the audio data and format configuration for audio content in the MistralAI chat completion API.
     *
     * @property data Base64 encoded audio data.
     * @property format The format of the encoded audio data. Currently, it supports "wav" and "mp3".
     */
    @Serializable
    public class InputAudio(public val data: String, public val format: String)

    /**
     * File content part in the MistralAI chat completion API.
     *
     * @property file The file data containing file content, ID and filename information
     */
    @Serializable
    @SerialName("file")
    public class File(public val file: FileData) : MistralAIContentPart

    /**
     * File data containing optional file content, ID and filename information.
     * Used to pass file data to OpenAI APIs requiring file handling capabilities.
     *
     * @property fileData The base64 encoded file data, used when passing the file to the model as a string.
     * @property fileId The ID of an uploaded file to use as input.
     * @property filename The name of the file, used when passing the file to the model as a string.
     */
    @Serializable
    public class FileData(
        public val fileData: String? = null,
        public val fileId: String? = null,
        public val filename: String? = null
    )

    /**
     * Document content part in the MistralAI chat completion API.
     *
     * @property documentUrl Contains the URL of the document
     * @property documentName Optional filename of the document
     */
    @Serializable
    @SerialName("document_url")
    public class Document(
        @SerialName("document_url")
        public val documentUrl: String,
        @SerialName("document_name")
        public val documentName: String?,
    ) : MistralAIContentPart

    /**
     * Reference content part in the MistralAI chat completion API.
     *
     * @property referenceIds Contains the ids of the references
     */
    @Serializable
    @SerialName("reference")
    public class Reference(
        @SerialName("reference_ids")
        public val referenceIds: List<Int>,
    ) : MistralAIContentPart

    /**
     * Thinkng content part in the MistralAI chat completion API.
     *
     * @property closed Whether the thinking chunk is closed or not. Currently only used for prefixing.
     */
    @Serializable
    @SerialName("thinking")
    public class Thinking(
        // TODO val thinking: List<...>
        public val closed: Boolean = true,
    ) : MistralAIContentPart
}

/**
 * @property completionTokens Number of tokens in the generated completion.
 * @property promptTokens Number of tokens in the prompt.
 * @property totalTokens Total number of tokens used in the request (prompt + completion).
 * @property promptAudioSeconds
 */
@Serializable
public class MistralAIUsage(
    @SerialName("prompt_tokens")
    public val promptTokens: Int,
    @SerialName("completion_tokens")
    public val completionTokens: Int,
    @SerialName("total_tokens")
    public val totalTokens: Int,
    @SerialName("prompt_audio_seconds")
    public val promptAudioSeconds: Int? = null,
)

/**
 * Mistral AI Chat Completion Streaming Response
 */
@Serializable
public class MistralAIChatCompletionStreamResponse(
    public val choices: List<OpenAIStreamChoice>,
    override val created: Long,
    override val id: String,
    override val model: String,
    public val systemFingerprint: String? = null,
    @SerialName("object")
    public val objectType: String,
    public val usage: MistralAIUsage? = null,
) : OpenAIBaseLLMStreamResponse

internal object MistralAIChatCompletionRequestSerializer :
    AdditionalPropertiesFlatteningSerializer<MistralAIChatCompletionRequest>(MistralAIChatCompletionRequest.serializer())

internal object MistralAIContentSerializer : KSerializer<MistralAIContent> {
    @OptIn(InternalSerializationApi::class)
    override val descriptor: SerialDescriptor = buildSerialDescriptor("MistralAIContent", SerialKind.CONTEXTUAL)

    override fun serialize(encoder: Encoder, value: MistralAIContent) {
        when (value) {
            is MistralAIContent.Text -> encoder.encodeString(value.value)
            is MistralAIContent.Parts -> encoder.encodeSerializableValue(
                ListSerializer(MistralAIContentPart.serializer()),
                value.value
            )
        }
    }

    override fun deserialize(decoder: Decoder): MistralAIContent {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Content can only be deserialized from JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> {
                if (!element.isString) {
                    throw SerializationException(
                        "Expected string for text content, but got: ${element.contentOrNull}"
                    )
                }
                MistralAIContent.Text(element.content)
            }

            is JsonArray -> {
                if (element.isEmpty()) {
                    throw SerializationException("Content array cannot be empty")
                }
                MistralAIContent.Parts(
                    jsonDecoder.json.decodeFromJsonElement(
                        ListSerializer(MistralAIContentPart.serializer()),
                        element
                    )
                )
            }

            else -> throw SerializationException(
                "MistralAIContent must be either a string or an array of content parts. " +
                    "Got: ${element::class.simpleName}"
            )
        }
    }
}
