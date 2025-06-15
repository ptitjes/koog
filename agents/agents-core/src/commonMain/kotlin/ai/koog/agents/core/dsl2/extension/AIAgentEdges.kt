package ai.koog.agents.core.dsl2.extension

import ai.koog.agents.core.dsl2.builder.AIAgentEdgeBuilder
import ai.koog.agents.core.dsl2.builder.onCondition
import ai.koog.agents.core.dsl2.builder.transformed
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.environment.SafeTool
import ai.koog.agents.core.environment.toSafeResult
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolResult
import ai.koog.prompt.message.MediaContent
import ai.koog.prompt.message.Message
import kotlin.reflect.KClass

/**
 * Creates an edge that filters outputs based on their type.
 *
 * @param klass The class to check instance against (not actually used, see implementation comment)
 */
public inline infix fun <SourceOutput, TargetInput, reified T : Any>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onIsInstance(
    /*
     klass is not used, but we need to use this trick to avoid passing all generic parameters on the usage side.
     Removing this parameter and just passing the correct type via generic reified parameter won't work, it requires all
     generic types in this case, which is not nice from the API perspective (trust me, I tried).
     */
    @Suppress("unused")
    klass: KClass<T>,
): AIAgentEdgeBuilder<SourceOutput, T> {
    return onCondition { output -> output is T }.transformed { it as T }
}


/**
 * Filters and transforms the intermediate outputs of the AI agent node based on the success results of a tool operation.
 *
 * This method is used to create a conditional path in the agent's execution by selecting only the successful results
 * of type [SafeTool.Result.Success] and evaluating them against a provided condition.
 *
 * @param condition A suspending lambda function that accepts a result of type [TResult]
 *                  and evaluates it to a Boolean value. Returns `true` if the condition is satisfied,
 *                  and `false` otherwise.
 * @return An instance of [AIAgentEdgeBuilder] configured to handle only successful tool results
 *         that satisfy the specified condition, with output type adjusted to [SafeTool.Result.Success].
 */
@Suppress("UNCHECKED_CAST")
public inline infix fun <SourceOutput, reified TResult : ToolResult>
        AIAgentEdgeBuilder<SourceOutput, SafeTool.Result<TResult>>.onSuccessful(
    crossinline condition: suspend (TResult) -> Boolean,
): AIAgentEdgeBuilder<SourceOutput, SafeTool.Result.Success<TResult>> =
    onIsInstance(SafeTool.Result.Success::class).transformed { it as SafeTool.Result.Success<TResult> }
        .onCondition { condition(it.result) }

/**
 * Defines a handler to process failure cases in a directed edge strategy by applying a condition
 * to filter intermediate results of type `SafeTool.Result.Failure`. This method is used to specialize
 * processing for failure results and to propagate or transform them based on the provided condition.
 *
 * @param condition A suspending lambda function that takes an error message string as input and returns a boolean.
 *                  It specifies whether the error should be further processed based on the condition provided.
 * @return A new instance of `AIAgentEdgeBuilder`, where the intermediate output type is restricted
 *         to `SafeTool.Result.Failure` containing the specified `TResult` for failure results that match the condition.
 */
@Suppress("UNCHECKED_CAST")
public inline infix fun <SourceOutput, reified TResult : ToolResult>
        AIAgentEdgeBuilder<SourceOutput, SafeTool.Result<TResult>>.onFailure(
    crossinline condition: suspend (error: String) -> Boolean,
): AIAgentEdgeBuilder<SourceOutput, SafeTool.Result.Failure<TResult>> =
    onIsInstance(SafeTool.Result.Failure::class).transformed { it as SafeTool.Result.Failure<TResult> }
        .onCondition { condition(it.message) }

/**
 * Creates an edge that filters tool call messages based on a custom condition.
 *
 * @param block A function that evaluates whether to accept a tool call message
 */
public infix fun <SourceOutput, TargetInput : List<Message>>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onSingleToolCall(
    block: suspend (Message.Tool.Call) -> Boolean,
): AIAgentEdgeBuilder<SourceOutput, Message.Tool.Call> =
    onCondition { it.size == 1 && it[0] is Message.Tool.Call }
        .transformed { it[0] as Message.Tool.Call }
        .onCondition { toolCall -> block(toolCall) }

/**
 * Creates an edge that filters tool call messages for a specific tool and arguments condition.
 *
 * @param tool The tool to match against
 * @param block An optional function that evaluates the tool arguments to determine if the edge should accept the message
 */
public inline fun <SourceOutput, TargetInput : List<Message>, reified Args : ToolArgs>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onSingleToolCall(
    tool: Tool<Args, *>,
    crossinline block: suspend (Args) -> Boolean = { true },
): AIAgentEdgeBuilder<SourceOutput, Message.Tool.Call> {
    return onSingleToolCall { it.tool == tool.name }
        .onCondition { toolCall ->
            val args = tool.decodeArgs(toolCall.contentJson)
            block(args)
        }
}

/**
 * Creates an edge that filters tool call messages to NOT be a specific tool
 *
 * @param tool The tool to match against
 */
public infix fun <SourceOutput, TargetInput : List<Message>>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onToolNotCalled(
    tool: Tool<*, *>,
): AIAgentEdgeBuilder<SourceOutput, TargetInput> =
    onCondition { it.size != 1 || it[0] !is Message.Tool.Call || (it[0] as Message.Tool.Call).tool != tool.name }

/**
 * Creates an edge that filters tool result messages for a specific tool and result condition.
 *
 * @param tool The tool to match against
 * @param block A function that evaluates the tool result to determine if the edge should accept the message
 */
public inline fun <SourceOutput, TargetInput, reified Result : ToolResult>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onToolResult(
    tool: Tool<*, Result>,
    crossinline block: suspend (SafeTool.Result<Result>) -> Boolean,
): AIAgentEdgeBuilder<SourceOutput, ReceivedToolResult> {
    return onIsInstance(ReceivedToolResult::class)
        .onCondition { toolResult ->
            (toolResult.tool == tool.name) && block(toolResult.toSafeResult())
        }
}

/**
 * Creates an edge that filters lists of tool call messages based on a custom condition.
 *
 * @param block A function that evaluates whether to accept a list of tool call messages
 */
public infix fun <SourceOutput, TargetInput : List<Message>>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onToolCalls(
    block: suspend (List<Message.Tool.Call>) -> Boolean,
): AIAgentEdgeBuilder<SourceOutput, List<Message.Tool.Call>> =
    transformed { it.filterIsInstance<Message.Tool.Call>() }
        .onCondition { toolCalls -> toolCalls.isNotEmpty() && block(toolCalls) }

/**
 * Creates an edge that filters lists of tool result messages based on a custom condition.
 *
 * @param block A function that evaluates whether to accept a list of tool result messages
 */
@Suppress("unused")
public infix fun <SourceOutput, TargetInput : List<ReceivedToolResult>>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onToolResults(
    block: suspend (List<ReceivedToolResult>) -> Boolean,
): AIAgentEdgeBuilder<SourceOutput, List<ReceivedToolResult>> =
    transformed { it.filterIsInstance<ReceivedToolResult>() }
        .onCondition { toolResults -> toolResults.isNotEmpty() && block(toolResults) }

/**
 * Creates an edge that filters assistant messages based on a custom condition and extracts their content.
 *
 * @param block A function that evaluates whether to accept an assistant message
 */
public infix fun <SourceOutput, TargetInput : List<Message>>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onAssistantMessages(
    block: suspend (List<Message.Assistant>) -> Boolean,
): AIAgentEdgeBuilder<SourceOutput, List<Message.Assistant>> =
    transformed { it.filterIsInstance<Message.Assistant>() }
        .onCondition { toolResults -> block(toolResults) }

/**
 * Creates an edge that filters assistant messages based on a custom condition and extracts their content.
 *
 * @param block A function that evaluates whether to accept an assistant message
 */
public infix fun <SourceOutput, TargetInput : List<Message>>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onSingleAssistantMessage(
    block: suspend (Message.Assistant) -> Boolean,
): AIAgentEdgeBuilder<SourceOutput, Message.Assistant> =
    onCondition { it.size == 1 && it[0] is Message.Assistant }
        .transformed { it[0] as Message.Assistant }
        .onCondition { message -> block(message) }

public infix fun <SourceOutput, T>
        AIAgentEdgeBuilder<SourceOutput, Message.Assistant>.unwrapResponse(
    block: suspend (Message.Assistant) -> T,
): AIAgentEdgeBuilder<SourceOutput, T> = transformed { block(it) }

/**
 * Creates an edge that filters assistant messages based on a custom condition and provides access to media content.
 *
 * @param block A function that evaluates whether to accept an assistant message with media
 */
public infix fun <SourceOutput, TargetInput>
        AIAgentEdgeBuilder<SourceOutput, TargetInput>.onAssistantMessageWithMedia(
    block: suspend (Message.Assistant) -> Boolean,
): AIAgentEdgeBuilder<SourceOutput, MediaContent> {
    return onIsInstance(Message.Assistant::class)
        .onCondition { it.mediaContent != null }
        .onCondition { signature -> block(signature) }
        .transformed { it.mediaContent!! }
}
