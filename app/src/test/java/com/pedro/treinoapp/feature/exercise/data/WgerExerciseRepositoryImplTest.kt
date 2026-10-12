package com.pedro.treinoapp.feature.exercise.data

import com.pedro.treinoapp.feature.exercise.data.remote.WgerApi
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseError
import com.pedro.treinoapp.feature.exercise.domain.model.ExerciseResult
import com.pedro.treinoapp.feature.exercise.domain.model.MuscleGroup
import com.pedro.treinoapp.feature.exercise.domain.model.WgerExercise
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class WgerExerciseRepositoryImplTest {

    private val server = MockWebServer()
    private lateinit var repository: WgerExerciseRepositoryImpl

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().readTimeout(1, TimeUnit.SECONDS).build()
        val api = Retrofit.Builder()
            .baseUrl(server.url("/api/v2/"))
            .client(client)
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WgerApi::class.java)
        repository = WgerExerciseRepositoryImpl(api)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource(name)).readText()

    @Test
    fun `searches exerciseinfo by name in portuguese and english`() = runTest {
        server.enqueue(MockResponse().setBody(fixture("wger/exerciseinfo_search.json")))

        val result = repository.search("supino")

        assertEquals(
            ExerciseResult.Success(
                listOf(
                    WgerExercise(75, "Supino com halteres", true, "Deite no banco.", MuscleGroup.CHEST),
                    WgerExercise(1778, "Jumping Jacks", false, null, null),
                ),
            ),
            result,
        )
        val request = server.takeRequest()
        assertEquals("/api/v2/exerciseinfo/", request.requestUrl?.encodedPath)
        assertEquals("supino", request.requestUrl?.queryParameter("name__search"))
        assertEquals("pt,en", request.requestUrl?.queryParameter("language__code"))
    }

    @Test
    fun `server error means service unavailable`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        assertEquals(ExerciseResult.Failure(ExerciseError.ServiceUnavailable), repository.search("supino"))
    }

    @Test
    fun `unexpected body means service unavailable`() = runTest {
        server.enqueue(MockResponse().setBody("""{"results": "not a list"}"""))

        assertEquals(ExerciseResult.Failure(ExerciseError.ServiceUnavailable), repository.search("supino"))
    }

    @Test
    fun `network failure means no connection`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

        assertEquals(ExerciseResult.Failure(ExerciseError.NoConnection), repository.search("supino"))
    }

    @Test
    fun `timeout means no connection`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))

        assertEquals(ExerciseResult.Failure(ExerciseError.NoConnection), repository.search("supino"))
    }
}
