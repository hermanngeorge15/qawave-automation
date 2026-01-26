package com.qawave.integration

import com.qawave.TestConfig
import com.qawave.presentation.controller.UpdateStatusRequest
import com.qawave.presentation.dto.request.CreateQaPackageRequest
import com.qawave.presentation.dto.request.QaPackageConfigRequest
import com.qawave.presentation.dto.request.UpdateQaPackageRequest
import com.qawave.presentation.dto.response.QaPackageResponse
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Integration tests for QaPackageController.
 * Tests the full HTTP request/response cycle with the in-memory repository.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(TestConfig::class)
@EnableAutoConfiguration(
    exclude = [
        RedisAutoConfiguration::class,
        RedisReactiveAutoConfiguration::class,
        KafkaAutoConfiguration::class,
    ],
)
class QaPackageControllerIntegrationTest {
    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Nested
    inner class CreatePackageTests {
        @Test
        fun `POST creates package with valid request`() {
            val request =
                CreateQaPackageRequest(
                    name = "Test Package",
                    description = "A test package",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0\ninfo:\n  title: Test API\n  version: 1.0.0",
                )

            val response =
                webTestClient.post()
                    .uri("/api/qa/packages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-Id", "test-user")
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody

            assertNotNull(response)
            assertNotNull(response.id)
            assertEquals("Test Package", response.name)
            assertEquals("A test package", response.description)
            assertEquals("https://api.example.com", response.baseUrl)
            assertEquals("REQUESTED", response.status)
            assertEquals("test-user", response.triggeredBy)
        }

        @Test
        fun `POST creates package with custom config`() {
            val request =
                CreateQaPackageRequest(
                    name = "Custom Config Package",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0",
                    config =
                        QaPackageConfigRequest(
                            maxScenarios = 20,
                            maxStepsPerScenario = 15,
                            parallelExecution = false,
                        ),
                )

            val response =
                webTestClient.post()
                    .uri("/api/qa/packages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody

            assertNotNull(response)
            assertNotNull(response.config)
            assertEquals(20, response.config.maxScenarios)
            assertEquals(15, response.config.maxStepsPerScenario)
            assertEquals(false, response.config.parallelExecution)
        }

        @Test
        fun `POST returns 400 when name is missing`() {
            val invalidJson = """{"baseUrl": "https://api.example.com"}"""

            webTestClient.post()
                .uri("/api/qa/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `POST returns 400 when baseUrl is missing`() {
            val invalidJson = """{"name": "Test Package"}"""

            webTestClient.post()
                .uri("/api/qa/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class GetPackageTests {
        @Test
        fun `GET returns package when found`() {
            // Create a package first
            val createRequest =
                CreateQaPackageRequest(
                    name = "Get Test Package",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0",
                )

            val created =
                webTestClient.post()
                    .uri("/api/qa/packages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody!!

            // Get the package
            val response =
                webTestClient.get()
                    .uri("/api/qa/packages/${created.id}")
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody

            assertNotNull(response)
            assertEquals(created.id, response.id)
            assertEquals("Get Test Package", response.name)
        }

        @Test
        fun `GET returns 404 when package not found`() {
            val nonExistentId = "00000000-0000-0000-0000-000000000000"

            webTestClient.get()
                .uri("/api/qa/packages/$nonExistentId")
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        fun `GET returns 400 for invalid UUID`() {
            webTestClient.get()
                .uri("/api/qa/packages/not-a-uuid")
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    inner class ListPackagesTests {
        @Test
        fun `GET list returns paginated results`() {
            // Create multiple packages
            repeat(3) { i ->
                val request =
                    CreateQaPackageRequest(
                        name = "List Test Package $i",
                        baseUrl = "https://api$i.example.com",
                        specContent = "openapi: 3.0.0",
                    )
                webTestClient.post()
                    .uri("/api/qa/packages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isCreated
            }

            // Get the list
            webTestClient.get()
                .uri("/api/qa/packages?page=0&size=10")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.page").isEqualTo(0)
                .jsonPath("$.size").isEqualTo(10)
                .jsonPath("$.content").isArray
        }

        @Test
        fun `GET list with status filter`() {
            // Create a package
            val request =
                CreateQaPackageRequest(
                    name = "Status Filter Package",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0",
                )
            webTestClient.post()
                .uri("/api/qa/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated

            // Filter by REQUESTED status
            webTestClient.get()
                .uri("/api/qa/packages?status=REQUESTED")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.content").isArray
        }
    }

    @Nested
    inner class UpdatePackageTests {
        @Test
        fun `PUT updates package`() {
            // Create a package
            val createRequest =
                CreateQaPackageRequest(
                    name = "Original Name",
                    description = "Original Description",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0",
                )

            val created =
                webTestClient.post()
                    .uri("/api/qa/packages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody!!

            // Update the package
            val updateRequest =
                UpdateQaPackageRequest(
                    name = "Updated Name",
                    description = "Updated Description",
                )

            val response =
                webTestClient.put()
                    .uri("/api/qa/packages/${created.id}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(updateRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody

            assertNotNull(response)
            assertEquals("Updated Name", response.name)
            assertEquals("Updated Description", response.description)
        }

        @Test
        fun `PUT returns 404 for non-existent package`() {
            val nonExistentId = "00000000-0000-0000-0000-000000000001"
            val updateRequest = UpdateQaPackageRequest(name = "New Name")

            webTestClient.put()
                .uri("/api/qa/packages/$nonExistentId")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateRequest)
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class UpdateStatusTests {
        @Test
        fun `PATCH updates status with valid transition`() {
            // Create a package
            val createRequest =
                CreateQaPackageRequest(
                    name = "Status Update Package",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0",
                )

            val created =
                webTestClient.post()
                    .uri("/api/qa/packages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody!!

            // Update status
            val statusRequest = UpdateStatusRequest(status = "SPEC_FETCHED")

            val response =
                webTestClient.patch()
                    .uri("/api/qa/packages/${created.id}/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(statusRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody

            assertNotNull(response)
            assertEquals("SPEC_FETCHED", response.status)
        }

        @Test
        fun `PATCH returns 400 for invalid status transition`() {
            // Create a package
            val createRequest =
                CreateQaPackageRequest(
                    name = "Invalid Transition Package",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0",
                )

            val created =
                webTestClient.post()
                    .uri("/api/qa/packages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody!!

            // Try invalid transition from REQUESTED to COMPLETE
            val statusRequest = UpdateStatusRequest(status = "COMPLETE")

            webTestClient.patch()
                .uri("/api/qa/packages/${created.id}/status")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(statusRequest)
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        fun `PATCH allows CANCELLED from any state`() {
            // Create a package
            val createRequest =
                CreateQaPackageRequest(
                    name = "Cancel Package",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0",
                )

            val created =
                webTestClient.post()
                    .uri("/api/qa/packages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody!!

            // Cancel the package
            val statusRequest = UpdateStatusRequest(status = "CANCELLED")

            val response =
                webTestClient.patch()
                    .uri("/api/qa/packages/${created.id}/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(statusRequest)
                    .exchange()
                    .expectStatus().isOk
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody

            assertNotNull(response)
            assertEquals("CANCELLED", response.status)
        }
    }

    @Nested
    inner class DeletePackageTests {
        @Test
        fun `DELETE removes package`() {
            // Create a package
            val createRequest =
                CreateQaPackageRequest(
                    name = "Delete Package",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0",
                )

            val created =
                webTestClient.post()
                    .uri("/api/qa/packages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(createRequest)
                    .exchange()
                    .expectStatus().isCreated
                    .expectBody(QaPackageResponse::class.java)
                    .returnResult()
                    .responseBody!!

            // Delete the package
            webTestClient.delete()
                .uri("/api/qa/packages/${created.id}")
                .exchange()
                .expectStatus().isNoContent

            // Verify it's gone
            webTestClient.get()
                .uri("/api/qa/packages/${created.id}")
                .exchange()
                .expectStatus().isNotFound
        }

        @Test
        fun `DELETE returns 404 for non-existent package`() {
            val nonExistentId = "00000000-0000-0000-0000-000000000002"

            webTestClient.delete()
                .uri("/api/qa/packages/$nonExistentId")
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    inner class CountTests {
        @Test
        fun `GET count returns total count`() {
            webTestClient.get()
                .uri("/api/qa/packages/count")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.count").isNumber
        }

        @Test
        fun `GET count with status filter`() {
            // Create a package
            val createRequest =
                CreateQaPackageRequest(
                    name = "Count Filter Package",
                    baseUrl = "https://api.example.com",
                    specContent = "openapi: 3.0.0",
                )
            webTestClient.post()
                .uri("/api/qa/packages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(createRequest)
                .exchange()
                .expectStatus().isCreated

            webTestClient.get()
                .uri("/api/qa/packages/count?status=REQUESTED")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.count").isNumber
        }
    }
}
