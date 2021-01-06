package dev.domnikl.schema_registry_gitops.state

import dev.domnikl.schema_registry_gitops.Compatibility
import dev.domnikl.schema_registry_gitops.SchemaRegistryClient
import dev.domnikl.schema_registry_gitops.State
import dev.domnikl.schema_registry_gitops.Subject
import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class DumperTest {
    private val client = mockk<SchemaRegistryClient>()
    private val stateDumper = Dumper(client)

    @Test
    fun `can dump current state with subjects`() {
        val schema = AvroSchema("{\"name\":\"FooKey\",\"type\":\"string\"}")

        every { client.globalCompatibility() } returns Compatibility.FULL
        every { client.subjects() } returns listOf("foo", "bar")

        every { client.compatibility("foo") } returns Compatibility.FULL_TRANSITIVE
        every { client.getLatestSchema("foo") } returns schema

        every { client.compatibility("bar") } returns Compatibility.BACKWARD
        every { client.getLatestSchema("bar") } returns schema

        val expectedState = State(
            Compatibility.FULL,
            listOf(
                Subject(
                    "foo",
                    Compatibility.FULL_TRANSITIVE,
                    schema
                ),
                Subject(
                    "bar",
                    Compatibility.BACKWARD,
                    schema
                )
            )
        )

        val state = stateDumper.dump()

        assertEquals(expectedState, state)
    }

    @Test
    fun `can dump current state handling implicit compatibility`() {
        val schema = AvroSchema("{\"name\":\"FooKey\",\"type\":\"string\"}")

        every { client.globalCompatibility() } returns Compatibility.FULL
        every { client.subjects() } returns listOf("bar")

        every { client.compatibility("bar") } returns Compatibility.NONE
        every { client.getLatestSchema("bar") } returns schema

        val expectedState = State(
            Compatibility.FULL,
            listOf(
                Subject(
                    "bar",
                    Compatibility.NONE,
                    schema
                )
            )
        )

        val state = stateDumper.dump()

        assertEquals(expectedState, state)
    }
}
