package io.spoud.kafka.connect.extensions

import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItems
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.images.builder.dockerfile.DockerfileBuilder
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.io.File
import java.util.*


@Testcontainers
class RestPolicyExtensionIT {

    @Test
    fun `should require additional properties when creating a connector via PUT`() {
        val baseUrl = "http://${connect.host}:${connect.getMappedPort(CONNECT_PORT)}"
        val createConnectorUrl = "$baseUrl/connectors/newTestConnector/config"
        println(createConnectorUrl)
        val response = given()
            .contentType(ContentType.JSON)
            .body(
                """{
                "connector.class": "org.apache.kafka.connect.tools.MockSinkConnector",
                "tasks.max": 1,
                "topics": "test-topic"
                }""".trimIndent()
            ).`when`()
            .put(createConnectorUrl)
            .then()
            .statusCode(400)
            .contentType(ContentType.JSON)
            .body("message", hasItems("missing required properties: [contact.email, contact.teams]"))

        println(response.extract().body().asPrettyString())
    }

    @Test
    fun `should add an endpoint for reloading the policies`() {
        val baseUrl = "http://${connect.host}:${connect.getMappedPort(CONNECT_PORT)}"
        val reloadUrl = "$baseUrl/policies/reload"
        val response = given()
            .`when`()
            .put(reloadUrl)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)

        println(response.extract().body().asPrettyString())
    }

    // TODO: testcases for POST /connectors with and without policy violations, PUT /connectors/<name>, DELETE and other endpoints

    companion object {
        private const val PLUGIN_PATH = "/usr/share/java"
        private const val CONF_PATH = "/usr/share/java"
        private const val EXTENSION_JAR = "connect-extension.jar"
        private const val EXTENSION_PROP = "rest-policy-config.json"
        private const val POLICY_PROP = "require-contact-info.json"
        private const val CONFLUENT_VERSION = "7.5.1"
        private const val CONNECT_PORT = 8083


        private val network: Network = Network.newNetwork()

        private val testId: String = UUID.randomUUID().toString()

        private val kafka: KafkaContainer =
            KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:$CONFLUENT_VERSION"))
                .withCreateContainerCmdModifier { cmd ->
                    cmd
                        .withName("kafka$testId")
                        .withHostName("kafka$testId")

                }
                .withListener { "kafka$testId:29092" }
                .withEnv("CONFLUENT_METRICS_ENABLE", "false")
                .withKraft()
                .withNetwork(network)
                .withExposedPorts(9092, 9093, 29092)

        private val connectWorkerWithExtension = ImageFromDockerfile()
            .withFileFromFile(EXTENSION_JAR, getJarFile())
            .withFileFromFile(EXTENSION_PROP, File("src/test/resources/rest-policy-config.json"))
            .withFileFromFile(POLICY_PROP, File("src/test/resources/require-contact-info.json"))
            .withDockerfileFromBuilder { builder: DockerfileBuilder ->
                builder
                    .from("confluentinc/cp-kafka-connect-base:$CONFLUENT_VERSION")
                    .copy(EXTENSION_JAR, PLUGIN_PATH)
                    .copy(EXTENSION_PROP, CONF_PATH)
                    .copy(POLICY_PROP, CONF_PATH)
                    .build()
            }

        private val connect: GenericContainer<*> = GenericContainer(
            connectWorkerWithExtension
        )
            .dependsOn(kafka)
            .withNetwork(network)
            .withCreateContainerCmdModifier { it.hostConfig?.withMemory(1024 * 1024 * 1024) }
            .withCreateContainerCmdModifier { cmd ->
                cmd
                    .withHostName("connect$testId")
                    .withName("connect$testId")
            }
            .withEnv("CONNECT_BOOTSTRAP_SERVERS", "kafka$testId:29092")
            .withEnv("CONNECT_REST_ADVERTISED_HOST_NAME", "connect$testId")
            .withEnv("CONNECT_REST_PORT", "$CONNECT_PORT")
            .withEnv("CONNECT_GROUP_ID", "compose-connect-group")
            .withEnv("CONNECT_CONFIG_STORAGE_TOPIC", "connect-configs")
            .withEnv("CONNECT_CONFIG_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_OFFSET_FLUSH_INTERVAL_MS", "10000")
            .withEnv("CONNECT_OFFSET_STORAGE_TOPIC", "connect-offsets")
            .withEnv("CONNECT_OFFSET_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_STATUS_STORAGE_TOPIC", "connect-status")
            .withEnv("CONNECT_STATUS_STORAGE_REPLICATION_FACTOR", "1")
            .withEnv("CONNECT_KEY_CONVERTER", "org.apache.kafka.connect.storage.StringConverter")
            .withEnv("CONNECT_VALUE_CONVERTER", "org.apache.kafka.connect.json.JsonConverter")
            .withEnv("CONNECT_PLUGIN_PATH", PLUGIN_PATH)
            .withEnv(
                "CONNECT_REST_EXTENSION_CLASSES", RestPolicyExtension::class.java.name
            )
            .withEnv("CONNECT_REST_POLICY_CONF", "$CONF_PATH/$EXTENSION_PROP")
            .withExposedPorts(CONNECT_PORT)
            .withLogConsumer(Slf4jLogConsumer(LoggerFactory.getLogger("connect-container")))


        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            kafka.start()
            connect.start()
            connect
                .waitingFor(Wait.forHealthcheck())
                .waitingFor(Wait.forHttp("/connectors"))
                .waitingFor(
                    Wait.forLogMessage(
                        "REST resources initialized; server is started and ready to handle requests",
                        1
                    )
                )
            // ensure that rest resources are ready to serve requests
            Thread.sleep(1000)
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            kafka.stop()
            connect.stop()
        }

        private fun getJarFile(): File {
            val jars = File("build/libs").walk()
                .filter { file: File -> file.name.matches("connect-extension-.+-all\\.jar$".toRegex()) }
                .toList()
            assertThat(jars).hasSize(1)
            return jars[0]
        }
    }
}
