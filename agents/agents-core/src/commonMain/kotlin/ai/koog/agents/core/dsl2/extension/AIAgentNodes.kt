package ai.koog.agents.core.dsl2.extension

import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegateBase
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.environment.SafeTool
import ai.koog.agents.core.environment.executeTool
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolResult
import ai.koog.prompt.dsl.PromptBuilder
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import ai.koog.prompt.params.LLMParams
import ai.koog.prompt.structure.StructuredData
import ai.koog.prompt.structure.StructuredDataDefinition
import ai.koog.prompt.structure.StructuredResponse
import kotlinx.coroutines.flow.Flow

/**
 * A pass-through node that does nothing and returns input as output
 *
 * @param name Optional node name, defaults to delegate's property name.
 */
public fun <T> AIAgentSubgraphBuilderBase<*, *>.passthrough(name: String? = null): AIAgentNodeDelegateBase<T, T> =
    node(name) { input -> input }

// ================
// Simple LLM nodes
// ================

public fun AIAgentSubgraphBuilderBase<*, *>.wrapUserRequest(
    name: String? = null,
): AIAgentNodeDelegateBase<String, List<Message.Request>> =
    node(name) { request ->
        llm.writeSession { listOf(Message.User(request, RequestMetaInfo.create(this.clock))) }
    }

// FIXME this is currently not used anywhere, is this needed?
/**
 * A node that adds messages to the LLM prompt using the provided prompt builder.
 *
 * @param name Optional node name, defaults to delegate's property name.
 * @param body Lambda to modify the prompt using PromptBuilder.
 */
public fun AIAgentSubgraphBuilderBase<*, *>.updatePrompt(
    name: String? = null,
    body: PromptBuilder.() -> Unit,
): AIAgentNodeDelegateBase<Unit, Unit> =
    node(name) {
        llm.writeSession { updatePrompt { body() } }
    }

/**
 * A node that adds the incoming messages to the LLM prompt, and returns them.
 *
 * @param name Optional node name, defaults to delegate's property name.
 */
public fun AIAgentSubgraphBuilderBase<*, *>.addToPrompt(
    name: String? = null,
): AIAgentNodeDelegateBase<List<Message>, List<Message>> =
    node(name) { messages ->
        llm.writeSession { updatePrompt { messages(messages) } }
        messages
    }

/**
 * A node that dumps the incoming messages to the LLM prompt.
 *
 * @param name Optional node name, defaults to delegate's property name.
 */
public fun AIAgentSubgraphBuilderBase<*, *>.dumpToPrompt(
    name: String? = null,
): AIAgentNodeDelegateBase<List<Message>, Unit> =
    node(name) { messages ->
        llm.writeSession { updatePrompt { messages(messages) } }
    }

/**
 * A node that calls the LLM and gets responses.
 *
 * @param name Optional name for the node.
 * @param toolChoice Optional tool choice strategy. Defaults to null, which means no tool calls.
 */
public fun AIAgentSubgraphBuilderBase<*, *>.requestLLM(
    name: String? = null,
    toolChoice: LLMParams.ToolChoice,
): AIAgentNodeDelegateBase<Unit, List<Message.Response>> =
    node(name) { message ->
        llm.writeSession { requestLLM(toolChoice) }
    }

/**
 * A node that that appends a user message to the LLM prompt and forces the LLM to use a specific tool.
 *
 * @param name Optional node name.
 * @param tool Tool descriptor the LLM is required to use.
 */
public fun AIAgentSubgraphBuilderBase<*, *>.requestLLM(
    name: String? = null,
    tool: ToolDescriptor,
): AIAgentNodeDelegateBase<Unit, List<Message.Response>> =
    requestLLM(name, LLMParams.ToolChoice.Named(tool.name))

/**
 * A node that appends a user message to the LLM prompt and forces the LLM to use a specific tool.
 *
 * @param name Optional node name.
 * @param tool Tool the LLM is required to use.
 */
public fun AIAgentSubgraphBuilderBase<*, *>.requestLLM(
    name: String? = null,
    tool: Tool<*, *>,
): AIAgentNodeDelegateBase<Unit, List<Message.Response>> =
    requestLLM(name, tool.descriptor)

/**
 * A node that appends a user message to the LLM prompt and gets a response with optional tool usage.
 *
 * @param name Optional node name.
 * @param allowToolCalls Controls whether LLM can use tools (default: true).
 */
public fun AIAgentSubgraphBuilderBase<*, *>.requestLLM(
    name: String? = null,
    allowToolCalls: Boolean = true,
): AIAgentNodeDelegateBase<Unit, List<Message.Response>> =
    requestLLM(name, if (allowToolCalls) LLMParams.ToolChoice.Auto else LLMParams.ToolChoice.None)

/**
 * A node that appends a user message to the LLM prompt and requests structured data from the LLM.
 *
 * @param name Optional node name.
 * @param structure Definition of expected output format and parsing logic.
 */
public fun <T> AIAgentSubgraphBuilderBase<*, *>.requestLLM(
    name: String? = null,
    structure: StructuredData<T>,
): AIAgentNodeDelegateBase<Unit, StructuredResponse<T>> =
    node(name) {
        llm.writeSession { requestLLMStructuredOneShot(structure) }
    }

/**
 * A node that appends a user message to the LLM prompt and requests structured data from the LLM with error correction capabilities.
 *
 * @param name Optional node name.
 * @param structure Definition of expected output format and parsing logic.
 * @param retries Number of retry attempts for failed generations.
 * @param fixingModel LLM used for error correction.
 */
public fun <T> AIAgentSubgraphBuilderBase<*, *>.requestLLM(
    name: String? = null,
    structure: StructuredData<T>,
    fixingModel: LLModel,
    retries: Int = 1,
): AIAgentNodeDelegateBase<Unit, Result<StructuredResponse<T>>> =
    node(name) {
        llm.writeSession { requestLLMStructured(structure, retries, fixingModel) }
    }

/**
 * A node that appends a user message to the LLM prompt, streams LLM response and transforms the stream data.
 *
 * @param name Optional node name.
 * @param structureDefinition Optional structure to guide the LLM response.
 * @param transformStreamData Function to process the streamed data.
 */
public fun <T> AIAgentSubgraphBuilderBase<*, *>.requestLLMStreaming(
    name: String? = null,
    structureDefinition: StructuredDataDefinition? = null,
    transformStreamData: suspend (Flow<String>) -> Flow<T>,
): AIAgentNodeDelegateBase<String, Flow<T>> =
    node(name) { message ->
        llm.writeSession {
            updatePrompt {
                user(message)
            }

            val stream = requestLLMStreaming(structureDefinition)

            transformStreamData(stream)
        }
    }

/**
 * A node that appends a user message to the LLM prompt and streams LLM response without transformation.
 *
 * @param name Optional node name.
 * @param structureDefinition Optional structure to guide the LLM response.
 */
public fun AIAgentSubgraphBuilderBase<*, *>.requestLLMStreaming(
    name: String? = null,
    structureDefinition: StructuredDataDefinition? = null,
): AIAgentNodeDelegateBase<String, Flow<String>> =
    requestLLMStreaming(name, structureDefinition) { it }

/**
 * A node that compresses the current LLM prompt (message history) into a summary, replacing messages with a TLDR.
 *
 * @param name Optional node name.
 * @param strategy Determines which messages to include in compression.
 * @param preserveMemory Specifies whether to retain message memory after compression.
 */
public fun <T> AIAgentSubgraphBuilderBase<*, *>.compressHistory(
    name: String? = null,
    strategy: HistoryCompressionStrategy = HistoryCompressionStrategy.WholeHistory,
    preserveMemory: Boolean = true,
): AIAgentNodeDelegateBase<T, T> = node(name) { input ->
    llm.writeSession { replaceHistoryWithTLDR(strategy, preserveMemory) }
    input
}

// ==========
// Tool nodes
// ==========

/**
 * A node that executes multiple tool calls. These calls can optionally be executed in parallel.
 *
 * @param name Optional node name.
 * @param parallelTools Specifies whether tools should be executed in parallel, defaults to false.
 */
public fun AIAgentSubgraphBuilderBase<*, *>.executeToolCalls(
    name: String? = null,
    parallelTools: Boolean = false,
): AIAgentNodeDelegateBase<List<Message.Tool.Call>, List<ReceivedToolResult>> =
    node(name) { toolCalls ->
        if (parallelTools) {
            environment.executeTools(toolCalls)
        } else {
            toolCalls.map { environment.executeTool(it) }
        }
    }

public fun AIAgentSubgraphBuilderBase<*, *>.wrapToolResults(
    name: String? = null,
): AIAgentNodeDelegateBase<List<ReceivedToolResult>, List<Message.Tool.Result>> =
    node(name) { results ->
        llm.writeSession { results.map { it.toMessage(this.clock) } }
    }

/**
 * A node that calls a specific tool directly using the provided arguments.
 *
 * @param name Optional node name.
 * @param tool The tool to execute.
 * @param doUpdatePrompt Specifies whether to add tool call details to the prompt.
 */
public inline fun <reified ToolArg : ToolArgs, reified TResult : ToolResult> AIAgentSubgraphBuilderBase<*, *>.executeTool(
    name: String? = null,
    tool: Tool<ToolArg, TResult>,
    doUpdatePrompt: Boolean = true,
): AIAgentNodeDelegateBase<ToolArg, SafeTool.Result<TResult>> =
    node(name) { toolArgs ->
        llm.writeSession {
            if (doUpdatePrompt) {
                updatePrompt {
                    // Why not tool message? Because it requires id != null to send it back to the LLM,
                    // The only workaround is to generate it
                    user(
                        "Tool call: ${tool.name} was explicitly called with args: ${
                            tool.encodeArgs(toolArgs)
                        }"
                    )
                }
            }

            val toolResult = callTool<ToolArg, TResult>(tool, toolArgs)

            if (doUpdatePrompt) {
                updatePrompt {
                    user(
                        "Tool call: ${tool.name} was explicitly called and returned result: ${
                            toolResult.content
                        }"
                    )
                }
            }

            toolResult
        }
    }
