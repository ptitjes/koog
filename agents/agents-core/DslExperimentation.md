# Pushing the experimentation

## Reducing the node vocabulary

To simplify the cognitive load and enhance nodes composability:

- promote nodes that take `List<*>` inputs and outputs
- split `nodeLLMRequest*` in multiple nodes, by extracting the prompt modifications
    - `addToPrompt(/* ... */): AIAgentNodeDelegateBase<List<Message>, List<Message>>`,
      adds the messages to the prompt and returns them
    - `dumpToPrompt(/* ... */): AIAgentNodeDelegateBase<List<Message>, Unit>`,
      dumps the messages to the prompt and returns nothing
    - `requestLLM(/* ... */): AIAgentNodeDelegateBase<Unit, List<Message.Response>>`,
      makes a LLM request and returns the responses
- have all variants of `requestLLM` as overloads
    - `requestLLM(name: String? = null, toolChoice: LLMParams.ToolChoice): /* ... */`
    - `requestLLM(name: String? = null, tool: ToolDescriptor): /* ... */`
    - `requestLLM(name: String? = null, tool: Tool<*, *>): /* ... */`
    - `requestLLM(name: String? = null, allowToolCalls: Boolean = true): /* ... */`
    - `requestLLM(name: String? = null, structure: StructuredData<T>): /* ... */`
    - `requestLLM(name: String? = null, structure: StructuredData<T>, /* ... */): /* ... */`

## Promoting typed AIAgent<Input, Output>

`AIAgent` would be parameterized (as would strategy/graph).

```kotlin
fun singleRunStrategy(
    name: String,
) = graph<List<Message.Request>, List<Message.Response>>(name) {
    val dumpUserRequest by dumpToPrompt()
    val requestLLM by requestLLM()
    val addLLMResponses by addToPrompt()
    val executeToolCalls by executeToolCalls()
    val wrapToolResults by wrapToolResults()
    val dumpToolResults by dumpToPrompt()

    nodeStart forwardTo dumpUserRequest
    dumpUserRequest forwardTo requestLLM

    requestLLM forwardTo addLLMResponses
    addLLMResponses onToolCalls { true } forwardTo executeToolCalls
    addLLMResponses onAssistantMessages { true } forwardTo nodeFinish

    executeToolCalls onToolResults { true } forwardTo wrapToolResults
    wrapToolResults forwardTo dumpToolResults
    dumpToolResults forwardTo requestLLM
}
```

The current `runAndGetResult` would become deprecated in favor of `run`.
```kotlin
class AIAgent<Input, Output> {
    fun run(input: Input): Output
}
```

```kotlin
// would type as AIAgent<List<Message.Request>, List<Message.Response>>
val agent = AIAgent(
    // ...
    strategy = singleRunStategy("my-agent-strategy"),
)
```

## Still offering helpers for the simpler use-cases

Simpler use-cases would still be supported by offering a few well-chosen `run` wrappers.

E.g.:
```kotlin
// Helpers for the simpler use-cases
fun AIAgent<List<Message.Request>, List<Message.Response>>.run(input: String): String? {
    val request = listOf(
        Message.User(content = input, metaInfo = RequestMetaInfo.create(clock))
    )
    val result = run(request)
    if (result.size > 1) {
        throw IllegalStateException("At most one answer was expected")
    }
    return result.getOrNull(0)?.content
}
```
