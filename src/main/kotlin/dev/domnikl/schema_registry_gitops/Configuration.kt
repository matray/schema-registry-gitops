package dev.domnikl.schema_registry_gitops

import org.slf4j.LoggerFactory
import java.io.File
import java.util.Properties

class Configuration(private val config: Map<String, String>) {
    val baseUrl by lazy { config[SCHEMA_REGISTRY_URL]!! }

    init {
        require(config.containsKey(SCHEMA_REGISTRY_URL)) { "Either property $SCHEMA_REGISTRY_URL or --registry needs to be provided" }
    }

    fun toMap() = config

    companion object {
        private const val ENV_PREFIX = "SCHEMA_REGISTRY_GITOPS_"
        private const val SCHEMA_REGISTRY_URL = "schema.registry.url"

        fun from(properties: Properties) = Configuration(
            properties.map { it.key.toString() to it.value.toString() }.toMap()
        )

        fun from(cli: CLI, env: Map<String, String> = emptyMap()): Configuration {
            val properties = cli.propertiesFilePath?.let { load(it) } ?: Properties()

            properties.putAll(fromEnv(env))

            // CLI-provided baseUrl overwrites properties file and env var
            cli.baseUrl?.let { properties.put(SCHEMA_REGISTRY_URL, it) }

            return from(properties)
        }

        private fun fromEnv(env: Map<String, String>): Properties {
            val withNormalizedKeys = env.mapKeys { (key, _) ->
                if (key.startsWith(ENV_PREFIX)) {
                    key.removePrefix(ENV_PREFIX).lowercase().replace('_', '.')
                } else {
                    null
                }
            }.filterNot { it.key == null }

            return Properties().also { it.putAll(withNormalizedKeys) }
        }

        private fun load(path: String): Properties {
            LoggerFactory.getLogger(this::class.java).debug("Loading properties from '$path'")

            return Properties().also { it.load(File(path).reader()) }
        }
    }
}
