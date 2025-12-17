# Contributing to SlackCat Framework

This guide provides best practices for creating SlackCat modules and using the framework effectively. It's designed to help both human developers and agentic coding tools understand the patterns and conventions used in this codebase.

## Table of Contents

- [Quick Start](#quick-start)
- [Module Architecture](#module-architecture)
- [Dependency Injection Patterns](#dependency-injection-patterns)
- [Network Client Usage](#network-client-usage)
- [Database Integration](#database-integration)
- [Event System](#event-system)
- [Testing Patterns](#testing-patterns)
- [Best Practices](#best-practices)
- [Common Patterns](#common-patterns)

---

## Quick Start

### Creating a Simple Module

The simplest module requires only implementing three methods:

```kotlin
class MyModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // Handle the command
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage("Hello!"),
            ),
        )
    }

    override fun commandInfo() = CommandInfo(
        command = "mycommand",
        aliases = listOf("mc", "mycmd"),
    )

    override fun help(): BotMessage = buildMessage {
        heading("My Command Help")
        text("This command does something useful.")
        section("Usage") {
            code("?mycommand [options]")
        }
    }
}
```

### Registering Your Module

Add your module to the module list in `AppModule.kt`:

```kotlin
val appModule = module {
    single<List<KClass<out SlackcatModule>>> {
        SlackcatModules.all + listOf(
            MyModule::class,
            // ... other custom modules
        )
    }
}
```

---

## Module Architecture

### Module Organization

Modules are categorized by their dependencies:

- **simple**: No external dependencies (pure logic)
- **network**: Require API keys or external services
- **storage**: Require database access
- **gamification**: Track user engagement
- **fun**: Entertainment-focused
- **utility**: Practical functionality

**Directory Structure:**
```
library/slackcat-modules/src/main/kotlin/com/slackcat/modules/
├── simple/          # PingModule, FlipModule, etc.
├── network/         # WeatherModule, CryptoModule, etc.
└── storage/         # KudosModule, LearnModule, etc.

app/src/main/kotlin/com/slackcat/app/modules/
└── [app-specific modules]
```

### Base Module Interface

All modules extend `SlackcatModule`:

```kotlin
abstract class SlackcatModule : KoinComponent {
    // Required methods
    abstract suspend fun onInvoke(incomingChatMessage: IncomingChatMessage)
    abstract fun help(): BotMessage
    open fun commandInfo(): CommandInfo { throw NotImplementedError() }

    // Optional overrides for custom branding
    open val botName: String? = null
    open val botIcon: BotIcon? = null

    // Optional reaction handling
    open fun reactionsToHandle(): Set<String> = emptySet()
    open suspend fun onReaction(event: SlackcatEvent) {}

    // Built-in utilities (inherited from KoinComponent)
    val chatClient: ChatClient by inject()
    val coroutineScope: CoroutineScope by inject()
    val config: SlackcatConfig by inject()

    suspend fun sendMessage(message: OutgoingChatMessage): Result<Unit>
    suspend fun postHelpMessage(channelId: String): Result<Unit>
}
```

### Optional Interfaces

Implement these interfaces for additional functionality:

```kotlin
// For modules that need database tables
interface StorageModule {
    fun tables(): List<Table>
}

// For modules that need to listen to framework events
interface SlackcatEventsModule {
    suspend fun onEvent(event: SlackcatEvent)
}

// For modules that handle unrecognized commands
interface UnhandledCommandModule {
    suspend fun onUnhandledCommand(message: IncomingChatMessage): Boolean
}
```

### Message Data Models

**Incoming Messages:**
```kotlin
data class IncomingChatMessage(
    val arguments: List<String>,     // Parsed arguments from user text
    val command: String,              // The primary command (e.g., "ping")
    val channelId: String,            // Channel where message was sent
    val chatUser: ChatUser,           // User who sent the message
    val messageId: String,            // Unique message identifier
    val userText: String,             // Raw user input (without command)
    val rawMessage: String,           // Complete message including command
)
```

**Outgoing Messages:**
```kotlin
data class OutgoingChatMessage(
    val channelId: String,
    val content: BotMessage,
)

// Build rich messages with DSL
val message = buildMessage {
    heading("Section Title")
    text("Plain text content")
    section("Subsection") {
        code("code snippet")
        bulletList("item1", "item2", "item3")
    }
}
```

---

## Dependency Injection Patterns

SlackCat uses **Koin** for dependency injection. All dependencies should be injected, never hard-coded.

### Core Available Dependencies

These are automatically available via `by inject()`:

```kotlin
class MyModule : SlackcatModule() {
    val chatClient: ChatClient by inject()          // For sending messages
    val coroutineScope: CoroutineScope by inject()  // For async operations
    val config: SlackcatConfig by inject()          // For app configuration
    val engine: Engine by inject()                  // CLI or Slack environment
}
```

### Constructor Injection Pattern

**Pattern 1: No Dependencies**
```kotlin
class SimpleModule : SlackcatModule()
// Uses only inherited dependencies via KoinComponent
```

**Pattern 2: NetworkClient Dependency**
```kotlin
class ApiModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val apiClient by lazy { MyApiClient(networkClient) }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val result = apiClient.fetchData()
        // ...
    }
}
```

**Pattern 3: Router Dependency (Advanced)**
```kotlin
class AdvancedModule(
    private val router: Router,
) : SlackcatModule() {
    // Can access other modules via router
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val allModules = router.getAllModules()
        // ...
    }
}
```

**Pattern 4: Mixed Injection**
```kotlin
class MixedModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val engine: Engine by inject()  // Property injection

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        if (engine == Engine.CLI) {
            // CLI-specific logic
        } else {
            // Slack-specific logic
        }
    }
}
```

### Adding Custom Dependencies

To make new dependencies available for injection, add them to `CoreModule.kt`:

```kotlin
val coreModule = module {
    // Example: Add a custom service
    single<MyService> { MyServiceImpl(get()) }

    // Example: Add a scoped dependency
    scope<SlackcatModule> {
        scoped<MyHelper> { MyHelperImpl() }
    }
}
```

**Important:** Never create singletons outside of Koin. Always inject dependencies.

---

## Network Client Usage

SlackCat provides a centralized `NetworkClient` for all HTTP operations. **Always use this client instead of creating your own HttpClient instances.**

### NetworkClient API

```kotlin
class NetworkClient(val httpClient: HttpClient) {
    // Fetch JSON and deserialize to type T
    suspend inline fun <reified T> fetch(
        url: String,
        serializer: KSerializer<T>,
        headers: Map<String, String> = emptyMap(),
    ): T

    // Fetch raw string response
    suspend fun fetchString(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): String

    // POST request with string body
    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap(),
    ): String
}
```

### Pattern: Dedicated Client Wrapper

**Best Practice:** Create a dedicated client class that wraps `NetworkClient` for your module's API.

```kotlin
// Step 1: Define your data models
@Serializable
data class WeatherResponse(
    val temperature: Double,
    val condition: String,
    val location: String,
)

// Step 2: Create a dedicated client
class WeatherClient(private val networkClient: NetworkClient) {
    private val apiKey = System.getenv("WEATHER_API_KEY")
    private val cache = mutableMapOf<String, WeatherResponse>()

    suspend fun getWeather(city: String): WeatherResponse? {
        // Check cache first
        cache[city]?.let { return it }

        // Fetch from API
        val result = fetchFromApi(city)
        result?.let { cache[city] = it }
        return result
    }

    private suspend fun fetchFromApi(city: String): WeatherResponse? {
        val url = "https://api.weather.com/v1/current?city=$city&apiKey=$apiKey"
        return runCatching {
            networkClient.fetch(
                url = url,
                serializer = WeatherResponse.serializer(),
                headers = mapOf("Accept" to "application/json"),
            )
        }.getOrNull()
    }
}

// Step 3: Use in your module
class WeatherModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val weatherClient by lazy { WeatherClient(networkClient) }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val city = incomingChatMessage.userText.trim()

        if (city.isEmpty()) {
            postHelpMessage(incomingChatMessage.channelId)
            return
        }

        val weather = weatherClient.getWeather(city)
        if (weather == null) {
            sendMessage(
                OutgoingChatMessage(
                    channelId = incomingChatMessage.channelId,
                    content = textMessage("Could not fetch weather for $city"),
                ),
            )
            return
        }

        val message = buildMessage {
            heading("Weather for ${weather.location}")
            text("Temperature: ${weather.temperature}°F")
            text("Condition: ${weather.condition}")
        }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = message,
            ),
        )
    }

    override fun commandInfo() = CommandInfo(command = "weather")
    override fun help(): BotMessage = buildMessage {
        heading("Weather Help")
        text("Get current weather for a city")
        code("?weather [city name]")
    }
}
```

### Error Handling Best Practices

**Always use `runCatching` for network operations:**

```kotlin
private suspend fun fetchData(): MyData? {
    return runCatching {
        networkClient.fetch(
            url = apiUrl,
            serializer = MyData.serializer(),
            headers = emptyMap(),
        )
    }.getOrNull()  // Returns null on failure
}

// Or with custom error handling
private suspend fun fetchDataWithLogging(): MyData? {
    return runCatching {
        networkClient.fetch(
            url = apiUrl,
            serializer = MyData.serializer(),
            headers = emptyMap(),
        )
    }.onFailure { exception ->
        println("API call failed: ${exception.message}")
    }.getOrNull()
}
```

### Caching Strategy

Implement caching at the client level:

```kotlin
class CachedApiClient(private val networkClient: NetworkClient) {
    private val cache = mutableMapOf<String, CachedData>()

    suspend fun getData(key: String): Data? {
        // Check cache with TTL
        val cached = cache[key]
        if (cached != null && !cached.isExpired()) {
            return cached.data
        }

        // Fetch and cache
        val fresh = fetchFromApi(key) ?: return null
        cache[key] = CachedData(fresh, System.currentTimeMillis())
        return fresh
    }

    data class CachedData(
        val data: Data,
        val timestamp: Long,
        val ttlMillis: Long = 5 * 60 * 1000, // 5 minutes
    ) {
        fun isExpired() = System.currentTimeMillis() - timestamp > ttlMillis
    }
}
```

---

## Database Integration

Modules that need persistent storage should implement the `StorageModule` interface.

### Basic Database Module Pattern

```kotlin
class MyStorageModule(
    private val networkClient: NetworkClient,
) : SlackcatModule(), StorageModule {

    private val dao by lazy { MyDAO() }

    // Define your database tables
    override fun tables(): List<Table> = listOf(
        MyDAO.UsersTable,
        MyDAO.ScoresTable,
    )

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // Use DAO to query/update database
        val user = dao.getUser(incomingChatMessage.chatUser.id)
        // ...
    }

    override fun commandInfo() = CommandInfo(command = "mystorage")
    override fun help(): BotMessage = buildMessage {
        heading("My Storage Help")
        text("This module stores data persistently")
    }
}
```

### DAO Pattern with Exposed ORM

**Step 1: Define Tables**

```kotlin
class MyDAO {
    object UsersTable : Table("users") {
        val userId = text("user_id")
        val username = text("username")
        val points = integer("points").default(0)
        val createdAt = long("created_at")
        override val primaryKey = PrimaryKey(userId)
    }

    object ScoresTable : Table("scores") {
        val id = integer("id").autoIncrement()
        val userId = text("user_id").references(UsersTable.userId)
        val score = integer("score")
        val timestamp = long("timestamp")
        override val primaryKey = PrimaryKey(id)
    }
}
```

**Step 2: Define Data Models**

```kotlin
@Serializable
data class User(
    val userId: String,
    val username: String,
    val points: Int,
    val createdAt: Long,
)

@Serializable
data class Score(
    val id: Int,
    val userId: String,
    val score: Int,
    val timestamp: Long,
)
```

**Step 3: Implement Query Methods**

```kotlin
class MyDAO {
    // ... table definitions above ...

    suspend fun getUser(userId: String): User? = dbQuery {
        UsersTable.selectAll().where { UsersTable.userId eq userId }
            .map { it.toUser() }
            .singleOrNull()
    }

    suspend fun createUser(userId: String, username: String): User = dbQuery {
        UsersTable.insert {
            it[UsersTable.userId] = userId
            it[UsersTable.username] = username
            it[UsersTable.points] = 0
            it[UsersTable.createdAt] = System.currentTimeMillis()
        }
        getUser(userId)!!
    }

    suspend fun updateUserPoints(userId: String, pointsDelta: Int): User? {
        return dbUpsert(
            table = UsersTable,
            keys = arrayOf(UsersTable.userId),
            onUpdate = listOf(
                UsersTable.points to (UsersTable.points + pointsDelta),
            ),
            insertBody = {
                it[UsersTable.userId] = userId
                it[UsersTable.username] = "Unknown"
                it[UsersTable.points] = pointsDelta
                it[UsersTable.createdAt] = System.currentTimeMillis()
            },
            selectWhere = { UsersTable.userId eq userId },
            mapper = { it.toUser() },
        )
    }

    suspend fun getTopScores(limit: Int = 10): List<Score> = dbQuery {
        ScoresTable.selectAll()
            .orderBy(ScoresTable.score to SortOrder.DESC)
            .limit(limit)
            .map { it.toScore() }
    }

    // Mapper extension functions
    private fun ResultRow.toUser() = User(
        userId = this[UsersTable.userId],
        username = this[UsersTable.username],
        points = this[UsersTable.points],
        createdAt = this[UsersTable.createdAt],
    )

    private fun ResultRow.toScore() = Score(
        id = this[ScoresTable.id],
        userId = this[ScoresTable.userId],
        score = this[ScoresTable.score],
        timestamp = this[ScoresTable.timestamp],
    )
}
```

### Database Helper Functions

Use these helper functions for common operations:

```kotlin
// For simple queries
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

// For upsert operations (insert or update)
suspend fun <T> dbUpsert(
    table: Table,
    keys: Array<Column<*>>,
    onUpdate: List<Pair<Column<*>, Expression<*>>>,
    insertBody: Table.(InsertStatement<*>) -> Unit,
    selectWhere: SqlExpressionBuilder.() -> Op<Boolean>,
    mapper: (ResultRow) -> T,
): T
```

**Example upsert usage:**

```kotlin
// Increment user score, creating user if not exists
val updatedUser = dbUpsert(
    table = UsersTable,
    keys = arrayOf(UsersTable.userId),
    onUpdate = listOf(
        UsersTable.points to (UsersTable.points + 10),
    ),
    insertBody = {
        it[UsersTable.userId] = userId
        it[UsersTable.username] = username
        it[UsersTable.points] = 10
        it[UsersTable.createdAt] = System.currentTimeMillis()
    },
    selectWhere = { UsersTable.userId eq userId },
    mapper = { it.toUser() },
)
```

### Database Initialization Pattern

For modules that need initial data:

```kotlin
class JeopardyModule(
    private val networkClient: NetworkClient,
) : SlackcatModule(), StorageModule {
    private val dao by lazy { JeopardyDAO(networkClient) }

    override fun tables(): List<Table> = listOf(
        JeopardyDAO.QuestionsTable,
    )

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // Hydrate database on first use
        if (dao.getQuestionCount() == 0L) {
            dao.hydrateQuestions()
        }

        // Process command
        // ...
    }
}

class JeopardyDAO(private val networkClient: NetworkClient) {
    suspend fun getQuestionCount(): Long = dbQuery {
        QuestionsTable.selectAll().count()
    }

    suspend fun hydrateQuestions() {
        val url = "https://example.com/questions.json"
        val jsonData = networkClient.fetchString(url, emptyMap())

        dbQuery {
            val questions: List<Question> = Json.decodeFromString(jsonData)
            questions.forEach { question ->
                QuestionsTable.insert {
                    it[QuestionsTable.id] = question.id
                    it[QuestionsTable.text] = question.text
                    it[QuestionsTable.answer] = question.answer
                }
            }
        }
    }
}
```

---

## Event System

SlackCat has an event system for modules that need to listen to framework-level events.

### Available Events

```kotlin
sealed interface SlackcatEvent {
    data object STARTED : SlackcatEvent

    data class MessageReceived(
        val userId: String,
        val channelId: String,
        val text: String,
        val timestamp: String,
        val threadTimestamp: String?,
    ) : SlackcatEvent

    data class ReactionAdded(
        val userId: String,
        val reaction: String,
        val channelId: String,
        val messageTimestamp: String,
        val itemUserId: String?,
        val eventTimestamp: String,
    ) : SlackcatEvent

    data class ReactionRemoved(
        val userId: String,
        val reaction: String,
        val channelId: String,
        val messageTimestamp: String,
        val itemUserId: String?,
        val eventTimestamp: String,
    ) : SlackcatEvent
}
```

### Implementing Event Listening

```kotlin
class DeployBotModule : SlackcatModule(), SlackcatEventsModule {
    private val engine: Engine by inject()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // no-op: this module only handles events
    }

    override suspend fun onEvent(event: SlackcatEvent) {
        // Only run in Slack environment
        if (engine != Engine.ChatClient.Slack) return

        val message = when (event) {
            SlackcatEvent.STARTED -> "Bot started! 🎉"
            is SlackcatEvent.MessageReceived -> return // Ignore
            is SlackcatEvent.ReactionAdded ->
                "Reaction added: ${event.reaction}"
            is SlackcatEvent.ReactionRemoved ->
                "Reaction removed: ${event.reaction}"
        }

        sendMessage(
            OutgoingChatMessage(
                channelId = "monitoring-channel-id",
                content = textMessage(message),
            ),
        )
    }

    override fun commandInfo() = CommandInfo(command = "deploy-bot")
    override fun help(): BotMessage = buildMessage {
        heading("Deploy Bot Help")
        text("Monitors bot lifecycle events")
    }
}
```

### Reaction Handling Pattern

For modules that respond to specific emoji reactions:

```kotlin
class KudosModule : SlackcatModule(), StorageModule {

    // Define which reactions this module handles
    override fun reactionsToHandle(): Set<String> = setOf(
        "tada",
        "heart",
        "fire",
    )

    // Handle reaction events
    override suspend fun onReaction(event: SlackcatEvent) {
        when (event) {
            is SlackcatEvent.ReactionAdded -> {
                handleReactionAdded(
                    userId = event.userId,
                    reaction = event.reaction,
                    targetUserId = event.itemUserId,
                )
            }
            is SlackcatEvent.ReactionRemoved -> {
                handleReactionRemoved(
                    userId = event.userId,
                    reaction = event.reaction,
                    targetUserId = event.itemUserId,
                )
            }
            else -> {}
        }
    }

    private suspend fun handleReactionAdded(
        userId: String,
        reaction: String,
        targetUserId: String?,
    ) {
        if (targetUserId == null || userId == targetUserId) return

        val points = when (reaction) {
            "tada" -> 5
            "heart" -> 3
            "fire" -> 2
            else -> 1
        }

        dao.addPoints(targetUserId, points)
    }

    // ... rest of module implementation
}
```

---

## Testing Patterns

SlackCat modules should be thoroughly tested using **MockK** and **Koin** for dependency injection.

### Basic Test Setup

```kotlin
class MyModuleTest {
    private lateinit var myModule: MyModule
    private lateinit var mockChatClient: ChatClient
    private lateinit var mockCoroutineScope: CoroutineScope
    private lateinit var mockConfig: SlackcatConfig

    @BeforeEach
    fun setup() {
        // Create mocks with relaxed mode
        mockChatClient = mockk(relaxed = true)
        mockCoroutineScope = mockk(relaxed = true)
        mockConfig = mockk(relaxed = true)

        // Configure mock behavior
        every { mockConfig.botNameProvider() } returns "TestBot"
        every { mockConfig.botIconProvider() } returns mockk(relaxed = true)
        coEvery { mockChatClient.sendMessage(any(), any(), any()) }
            returns Result.success(Unit)

        // Start Koin with test module
        startKoin {
            modules(
                module {
                    single<ChatClient> { mockChatClient }
                    single<CoroutineScope> { mockCoroutineScope }
                    single<SlackcatConfig> { mockConfig }
                },
            )
        }

        // Initialize module under test
        myModule = MyModule()
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    // Helper to create test messages
    private fun createTestMessage(
        command: String,
        userText: String = "",
        channelId: String = "channel123",
        arguments: List<String> = emptyList(),
    ) = IncomingChatMessage(
        arguments = arguments,
        command = command,
        channelId = channelId,
        chatUser = ChatUser("user123"),
        messageId = "msg123",
        rawMessage = "?$command $userText",
        userText = userText,
    )
}
```

### Testing Module Commands

```kotlin
@Test
fun `commandInfo returns correct command and aliases`() {
    val info = myModule.commandInfo()

    assertEquals("mycommand", info.command)
    assertTrue(info.aliases.contains("mc"))
    assertTrue(info.aliases.contains("mycmd"))
}

@Test
fun `onInvoke sends message to correct channel`() = runTest {
    val testMessage = createTestMessage(
        command = "mycommand",
        userText = "test input",
        channelId = "channel123",
    )

    myModule.onInvoke(testMessage)

    // Capture sent message
    val messageSlot = slot<OutgoingChatMessage>()
    coVerify {
        mockChatClient.sendMessage(
            capture(messageSlot),
            any(),
            any(),
        )
    }

    // Verify message content
    val sentMessage = messageSlot.captured
    assertEquals("channel123", sentMessage.channelId)
}

@Test
fun `onInvoke handles empty input by showing help`() = runTest {
    val testMessage = createTestMessage(
        command = "mycommand",
        userText = "",
    )

    myModule.onInvoke(testMessage)

    // Verify help was posted
    coVerify {
        mockChatClient.sendMessage(
            any(),
            any(),
            any(),
        )
    }
}
```

### Testing Network Clients

```kotlin
class WeatherClientTest {
    private lateinit var weatherClient: WeatherClient
    private lateinit var mockNetworkClient: NetworkClient

    @BeforeEach
    fun setup() {
        mockNetworkClient = mockk(relaxed = true)
        weatherClient = WeatherClient(mockNetworkClient)
    }

    @Test
    fun `getWeather returns data on successful fetch`() = runTest {
        val mockResponse = WeatherResponse(
            temperature = 72.0,
            condition = "Sunny",
            location = "San Francisco",
        )

        coEvery {
            mockNetworkClient.fetch<WeatherResponse>(
                any(),
                any(),
                any(),
            )
        } returns mockResponse

        val result = weatherClient.getWeather("San Francisco")

        assertNotNull(result)
        assertEquals(72.0, result.temperature)
        assertEquals("Sunny", result.condition)
    }

    @Test
    fun `getWeather returns null on network failure`() = runTest {
        coEvery {
            mockNetworkClient.fetch<WeatherResponse>(
                any(),
                any(),
                any(),
            )
        } throws Exception("Network error")

        val result = weatherClient.getWeather("San Francisco")

        assertNull(result)
    }

    @Test
    fun `getWeather uses cache on subsequent calls`() = runTest {
        val mockResponse = WeatherResponse(
            temperature = 72.0,
            condition = "Sunny",
            location = "San Francisco",
        )

        coEvery {
            mockNetworkClient.fetch<WeatherResponse>(
                any(),
                any(),
                any(),
            )
        } returns mockResponse

        // First call
        weatherClient.getWeather("San Francisco")

        // Second call should use cache
        val result = weatherClient.getWeather("San Francisco")

        // Verify fetch was only called once
        coVerify(exactly = 1) {
            mockNetworkClient.fetch<WeatherResponse>(
                any(),
                any(),
                any(),
            )
        }

        assertNotNull(result)
    }
}
```

### Testing Database Interactions

```kotlin
class MyDAOTest {
    private lateinit var dao: MyDAO
    private lateinit var testDatabase: Database

    @BeforeEach
    fun setup() {
        // Use H2 in-memory database for tests
        testDatabase = Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            driver = "org.h2.Driver",
        )

        transaction(testDatabase) {
            SchemaUtils.create(MyDAO.UsersTable, MyDAO.ScoresTable)
        }

        dao = MyDAO()
    }

    @AfterEach
    fun tearDown() {
        transaction(testDatabase) {
            SchemaUtils.drop(MyDAO.UsersTable, MyDAO.ScoresTable)
        }
    }

    @Test
    fun `createUser inserts user into database`() = runTest {
        val user = dao.createUser("user123", "TestUser")

        assertNotNull(user)
        assertEquals("user123", user.userId)
        assertEquals("TestUser", user.username)
        assertEquals(0, user.points)
    }

    @Test
    fun `updateUserPoints increments existing user points`() = runTest {
        dao.createUser("user123", "TestUser")

        val updated = dao.updateUserPoints("user123", 10)

        assertNotNull(updated)
        assertEquals(10, updated.points)
    }

    @Test
    fun `updateUserPoints creates user if not exists`() = runTest {
        val user = dao.updateUserPoints("newuser", 5)

        assertNotNull(user)
        assertEquals("newuser", user.userId)
        assertEquals(5, user.points)
    }

    @Test
    fun `getTopScores returns scores in descending order`() = runTest {
        dao.createUser("user1", "User1")
        dao.createUser("user2", "User2")
        dao.createUser("user3", "User3")

        dao.addScore("user1", 100)
        dao.addScore("user2", 200)
        dao.addScore("user3", 150)

        val topScores = dao.getTopScores(limit = 3)

        assertEquals(3, topScores.size)
        assertEquals(200, topScores[0].score)
        assertEquals(150, topScores[1].score)
        assertEquals(100, topScores[2].score)
    }
}
```

---

## Best Practices

### General Module Design

**DO:**
- ✅ Keep modules focused on a single responsibility
- ✅ Use dependency injection for all external dependencies
- ✅ Implement comprehensive error handling with `runCatching`
- ✅ Provide clear, helpful help messages
- ✅ Write tests for all public functionality
- ✅ Use dedicated client classes for external APIs
- ✅ Implement caching for expensive operations
- ✅ Use descriptive names for commands and aliases

**DON'T:**
- ❌ Create HttpClient instances manually (use injected NetworkClient)
- ❌ Hard-code configuration values (use environment variables or config)
- ❌ Expose internal implementation details in help messages
- ❌ Ignore errors or let exceptions propagate uncaught
- ❌ Mix business logic with presentation logic
- ❌ Duplicate code across modules (extract shared utilities)

### Error Handling

```kotlin
// GOOD: Graceful error handling
override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
    val result = runCatching {
        apiClient.fetchData(incomingChatMessage.userText)
    }.onFailure { exception ->
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage("Error: ${exception.message}"),
            ),
        )
        return
    }.getOrNull()

    // Process result
}

// BAD: Unhandled exceptions
override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
    val result = apiClient.fetchData(incomingChatMessage.userText) // Can throw!
    // Process result
}
```

### Input Validation

```kotlin
// GOOD: Validate and provide feedback
override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
    val input = incomingChatMessage.userText.trim()

    if (input.isEmpty()) {
        postHelpMessage(incomingChatMessage.channelId)
        return
    }

    if (input.length > 100) {
        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage("Input too long (max 100 characters)"),
            ),
        )
        return
    }

    // Process valid input
}

// BAD: Assume input is valid
override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
    val result = processInput(incomingChatMessage.userText) // What if empty?
}
```

### Message Building

```kotlin
// GOOD: Use buildMessage DSL for rich content
val message = buildMessage {
    heading("Weather Report")
    section("Current Conditions") {
        text("Temperature: 72°F")
        text("Humidity: 65%")
        text("Wind: 5 mph")
    }
    section("Forecast") {
        bulletList(
            "Tomorrow: Sunny, 75°F",
            "Wednesday: Partly Cloudy, 70°F",
            "Thursday: Rain, 65°F",
        )
    }
}

// BAD: Plain text with manual formatting
val message = textMessage(
    "Weather Report\nTemperature: 72°F\nHumidity: 65%\nWind: 5 mph"
)
```

### Configuration Management

```kotlin
// GOOD: Use environment variables
class ApiModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val apiKey = System.getenv("MY_API_KEY")
        ?: throw IllegalStateException("MY_API_KEY not set")
    private val apiBaseUrl = System.getenv("MY_API_URL")
        ?: "https://api.example.com"
}

// BAD: Hard-coded values
class ApiModule(
    private val networkClient: NetworkClient,
) : SlackcatModule() {
    private val apiKey = "abc123xyz"  // Security risk!
    private val apiBaseUrl = "https://api.example.com"  // Not configurable
}
```

---

## Common Patterns

### Alias Handler Pattern

For modules with complex alias commands:

```kotlin
class CommandAliasHandler(private val dao: MyDAO) {
    fun handleAliases(message: IncomingChatMessage): OutgoingChatMessage? {
        return when (message.command) {
            "top" -> handleTopCommand(message)
            "stats" -> handleStatsCommand(message)
            "reset" -> handleResetCommand(message)
            else -> null
        }
    }

    private suspend fun handleTopCommand(
        message: IncomingChatMessage,
    ): OutgoingChatMessage {
        val topUsers = dao.getTopUsers(limit = 10)
        val content = buildMessage {
            heading("Top 10 Users")
            topUsers.forEachIndexed { index, user ->
                text("${index + 1}. ${user.username}: ${user.points} points")
            }
        }
        return OutgoingChatMessage(message.channelId, content)
    }

    // ... other alias handlers
}

// In your module
class MyModule : SlackcatModule() {
    private val aliasHandler by lazy { CommandAliasHandler(dao) }

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        // Check aliases first
        val aliasMessage = aliasHandler.handleAliases(incomingChatMessage)
        if (aliasMessage != null) {
            sendMessage(aliasMessage)
            return
        }

        // Handle main command
        // ...
    }
}
```

### Pagination Pattern

For commands that return large datasets:

```kotlin
class PaginatedModule : SlackcatModule() {
    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val page = incomingChatMessage.userText.toIntOrNull() ?: 1
        val pageSize = 10

        val results = dao.getResults(page, pageSize)
        val totalPages = dao.getTotalPages(pageSize)

        val message = buildMessage {
            heading("Results (Page $page of $totalPages)")
            results.forEach { result ->
                text("• ${result.name}: ${result.value}")
            }
            section("Navigation") {
                text("Use `?command [page]` to view other pages")
            }
        }

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = message,
            ),
        )
    }
}
```

### Confirmation Pattern

For destructive operations:

```kotlin
class DestructiveModule : SlackcatModule() {
    private val pendingConfirmations = mutableMapOf<String, PendingAction>()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        val userId = incomingChatMessage.chatUser.id

        if (incomingChatMessage.userText == "confirm") {
            val pending = pendingConfirmations.remove(userId)
            if (pending != null) {
                executeDestructiveAction(pending)
                sendMessage(
                    OutgoingChatMessage(
                        channelId = incomingChatMessage.channelId,
                        content = textMessage("Action completed!"),
                    ),
                )
                return
            }
        }

        // Queue for confirmation
        pendingConfirmations[userId] = PendingAction(
            userId = userId,
            action = incomingChatMessage.userText,
            timestamp = System.currentTimeMillis(),
        )

        sendMessage(
            OutgoingChatMessage(
                channelId = incomingChatMessage.channelId,
                content = textMessage(
                    "⚠️ This will delete all your data. " +
                    "Type `?delete confirm` to proceed."
                ),
            ),
        )
    }

    data class PendingAction(
        val userId: String,
        val action: String,
        val timestamp: Long,
    )
}
```

### Environment-Specific Behavior

```kotlin
class EnvironmentAwareModule : SlackcatModule() {
    private val engine: Engine by inject()

    override suspend fun onInvoke(incomingChatMessage: IncomingChatMessage) {
        when (engine) {
            Engine.CLI -> handleCliMode(incomingChatMessage)
            Engine.ChatClient.Slack -> handleSlackMode(incomingChatMessage)
        }
    }

    private suspend fun handleCliMode(message: IncomingChatMessage) {
        // CLI-specific behavior (e.g., simpler output)
        sendMessage(
            OutgoingChatMessage(
                channelId = message.channelId,
                content = textMessage("Result: ${processCommand(message)}"),
            ),
        )
    }

    private suspend fun handleSlackMode(message: IncomingChatMessage) {
        // Slack-specific behavior (e.g., rich formatting)
        val result = processCommand(message)
        val richMessage = buildMessage {
            heading("Results")
            section("Details") {
                text(result)
            }
        }
        sendMessage(
            OutgoingChatMessage(
                channelId = message.channelId,
                content = richMessage,
            ),
        )
    }
}
```

---

## Additional Resources

### Project Structure

- **Core Framework:** `library/slackcat/`
- **Shared Modules:** `library/slackcat-modules/`
- **App Entry Point:** `app/src/main/kotlin/com/slackcat/app/`
- **Network Client:** `library/slackcat/src/main/kotlin/com/slackcat/core/network/`
- **Database Utils:** `library/slackcat/src/main/kotlin/com/slackcat/core/database/`

### Key Files

- **Module Registration:** `app/src/main/kotlin/com/slackcat/app/di/AppModule.kt`
- **DI Setup:** `library/slackcat/src/main/kotlin/com/slackcat/di/CoreModule.kt`
- **Router:** `library/slackcat/src/main/kotlin/com/slackcat/internal/Router.kt`
- **Base Module:** `library/slackcat/src/main/kotlin/com/slackcat/models/SlackcatModule.kt`

### Example Modules to Study

- **Simple:** `PingModule` - Minimal implementation
- **Network:** `CryptoPriceModule` - API integration with caching
- **Database:** `JeopardyModule` - Complex DAO with hydration
- **Events:** `DeployBotModule` - Event-driven architecture
- **Advanced:** `LearnModule` - Router dependency with storage

---

## Getting Help

If you have questions or need clarification:

1. Study existing modules in `library/slackcat-modules/` and `app/src/main/kotlin/com/slackcat/app/modules/`
2. Review test files for usage examples
3. Check the Koin documentation for DI patterns: https://insert-koin.io/
4. Review the Exposed ORM documentation: https://github.com/JetBrains/Exposed

---

**Happy Building! 🐱**
