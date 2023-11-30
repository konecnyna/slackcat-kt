package com.stash

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ExternalModuleDependencyBundle
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.dsl.DependencyFactory
import java.util.Optional

/**
 * The version catlog cannot be accessed form the source files of plugins, instead here
 * the catalog is loaded manually and values are pulled out of it.
 */
class SlackCatLibraries internal constructor(
    private val catalog: VersionCatalog,
    private val dependencyFactory: DependencyFactory,
) {

    val ktlint: ExternalModuleDependency
        get() = getValue("ktlint")

    val bundles: Bundles = Bundles()

    internal fun getValue(key: String): ExternalModuleDependency {
        return getOptionalValue(key).orElseThrow {
            IllegalStateException("No catalog version found for '$key'")
        }
    }

    private fun getOptionalValue(key: String): Optional<ExternalModuleDependency> {
        return catalog.findLibrary(key).map { minimalDependency ->
            val dependency = minimalDependency.get()
            dependencyFactory.create(dependency.group, dependency.name, dependency.version)
        }
    }
    inner class Bundles {
        val compose: ExternalModuleDependencyBundle
            get() = getBundle("compose")

        val composeDebug: ExternalModuleDependencyBundle
            get() = getBundle("composeDebug")

        val testing: ExternalModuleDependencyBundle
            get() = getBundle("testing")

        val androidTesting: ExternalModuleDependencyBundle
            get() = getBundle("androidTesting")

        private fun getBundle(key: String): ExternalModuleDependencyBundle {
            return getOptionalBundle(key).orElseThrow {
                IllegalStateException("No catalog bundle found for '$key'")
            }
        }

        private fun getOptionalBundle(key: String): Optional<ExternalModuleDependencyBundle> {
            return catalog.findBundle(key).map { it.get() }
        }
    }
}
