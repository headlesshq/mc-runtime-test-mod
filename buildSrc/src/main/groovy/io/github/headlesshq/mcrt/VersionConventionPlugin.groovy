package io.github.headlesshq.mcrt

import org.gradle.api.*
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.JavaCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

class VersionConventionPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        // Apply necessary plugins
        project.pluginManager.apply('java')
        project.pluginManager.apply('maven-publish')
        project.pluginManager.apply('xyz.wagyourtail.unimined')
        project.pluginManager.apply('com.gradleup.shadow')
        project.pluginManager.apply('systems.manifold.manifold-gradle-plugin')

        def verId = project.name

        // Skip configuration for certain subprojects
        if (verId == 'shared' || verId == 'api') return

        project.logger.lifecycle("Configuring subproject: ${project.path}, version=${verId}")

        // Load version-specific properties
        def propsFile = project.file("gradle.properties")
        if (!propsFile.exists()) {
            throw new GradleException("Properties file not found: ${propsFile}")
        }

        def props = new Properties()
        propsFile.withInputStream { props.load(it) }

        def currentMinecraftVersion = props.getProperty('minecraft_version')
        def currentLexforgeVersion = props.getProperty('lexforge_version')
        def currentNeoForgeVersion = props.getProperty('neoforge_version')
        def currentLoaders = props.getProperty('loaders')?.split(',')*.trim()
        def currentMappings = props.getProperty('mappings')

        // Load global properties
        def globalPropsFile = project.rootProject.file("build.properties")
        if (!globalPropsFile.exists()) {
            throw new GradleException("Global build properties file not found: ${globalPropsFile}")
        }

        def globalProps = new Properties()
        globalPropsFile.withInputStream { globalProps.load(it) }

        def mappingKey = "MC_${currentMinecraftVersion.replace('.', '_')}"
        def mcVerValue = globalProps.getProperty(mappingKey)

        // Log loaded properties
        project.logger.lifecycle("'minecraft_version' = ${currentMinecraftVersion}")
        project.logger.lifecycle("'lexforge_version' = ${currentLexforgeVersion}")
        project.logger.lifecycle("'neoforge_version' = ${currentNeoForgeVersion}")
        project.logger.lifecycle("'loaders' = ${currentLoaders}")
        project.logger.lifecycle("'mappings' = ${currentMappings}")
        project.logger.lifecycle("MC_VER = ${mcVerValue}")

        // Project version
        def projectVersion = project.rootProject.hasProperty('project_version') ? project.rootProject.property('project_version') : 'unknown'

        // Build version
        def buildVersion = "${currentMinecraftVersion}-${project_version}"

        // Configure source sets
        configureSourceSets(project, currentLoaders)

        // Configure Unimined
        configureUnimined(project, currentMinecraftVersion, currentMappings, currentLoaders, currentLexforgeVersion, currentNeoForgeVersion)

        // Configure configurations
        configureConfigurations(project)

        // Configure dependencies
        configureDependencies(project, currentLoaders)

        // Configure tasks
        configureTasks(project, mcVerValue, buildVersion, currentMinecraftVersion, currentLoaders)

        // Configure publishing
        configurePublishing(project, buildVersion, currentLoaders)
    }

    private void configureSourceSets(Project project, def currentLoaders) {
        project.sourceSets {
            main {
                // java {
                //     srcDirs += "${project.rootDir}/shared/src/main/java"
                // }
                // resources {
                //     srcDirs += "${project.rootDir}/shared/src/main/resources"
                // }
                compileClasspath += project.configurations.mainImplementation
                runtimeClasspath += project.configurations.mainImplementation
            }

            fabric {
                compileClasspath += main.output
                runtimeClasspath += main.output
            }

            lexforge {
                compileClasspath += main.output
                runtimeClasspath += main.output
            }

            if (currentLoaders.contains('neoforge')) {
                neoforge {
                    compileClasspath += main.output
                    runtimeClasspath += main.output
                }
            }
        }
    }

    private void configureUnimined(Project project, def minecraftVersion, def mappings, def loaders, def lexforgeVersion, def neoforgeVersion) {
        project.unimined.minecraft {
            version minecraftVersion
            mappings {
                mappings?.split(',')?.each { mapping ->
                    def split = mapping.split(':')
                    switch (split[0]) {
                        case 'searge': searge(); break
                        case 'mcp': mcp("stable", split[1]); break
                        case 'mojmap': mojmap(); devFallbackNamespace "mojmap"; break
                    }
                }
            }
            defaultRemapJar = false
        }

        if (loaders.contains('legacyfabric')) {
            project.unimined.minecraft(project.sourceSets.fabric) {
                combineWith(project.sourceSets.main)
                legacyFabric { loader project.findProperty('fabric_version') }
                defaultRemapJar = true
            }
        } else {
            project.unimined.minecraft(project.sourceSets.fabric) {
                combineWith(project.sourceSets.main)
                fabric { loader project.findProperty('fabric_version') }
                defaultRemapJar = true
            }
        }

        if (loaders.contains('neoforge')) {
            project.unimined.minecraft(project.sourceSets.neoforge) {
                combineWith(project.sourceSets.main)
                neoForge {
                    loader neoforgeVersion
                    mixinConfig 'mc_runtime_test.mixins.json'
                }
                minecraftRemapper.config { ignoreConflicts(true) }
                defaultRemapJar = true
            }
        }

        project.unimined.minecraft(project.sourceSets.lexforge) {
            combineWith(project.sourceSets.main)
            minecraftForge {
                loader lexforgeVersion
                mixinConfig 'mc_runtime_test.mixins.json'
            }
            defaultRemapJar = true
        }
    }

    private void configureConfigurations(Project project) {
        project.configurations {
            mainImplementation
            implementation {
                extendsFrom lexforgeImplementation
                extendsFrom fabricImplementation
            }
            jarLibs
            implementation.extendsFrom jarLibs
        }
    }

    private void configureDependencies(Project project, def loaders) {
        project.dependencies {
            annotationProcessor("systems.manifold:manifold-preprocessor:2024.1.37")
            lexforgeAnnotationProcessor("systems.manifold:manifold-preprocessor:2024.1.37")
            fabricAnnotationProcessor("systems.manifold:manifold-preprocessor:2024.1.37")
            if (loaders.contains('neoforge')) {
                neoforgeAnnotationProcessor("systems.manifold:manifold-preprocessor:2024.1.37")
            }

            if (loaders.contains('legacyfabric')) {
                implementation 'net.minecraft:launchwrapper:1.12'
                jarLibs 'com.github.ImpactDevelopment:SimpleTweaker:1.2'
                jarLibs('org.spongepowered:mixin:0.7.11-SNAPSHOT') {
                    exclude module: 'launchwrapper'
                    exclude module: 'guava'
                    exclude module: 'gson'
                    exclude module: 'commons-io'
                }
            } else {
                implementation('org.spongepowered:mixin:0.8.5') {
                    exclude module: 'launchwrapper'
                    exclude module: 'guava'
                    exclude module: 'gson'
                    exclude module: 'commons-io'
                }
            }

            jarLibs 'org.junit.jupiter:junit-jupiter-api:5.10.1'
            jarLibs project.project(':api')
        }
    }

    private void configureTasks(Project project, def mcVerValue, def buildVersion, def currentMinecraftVersion, def currentLoaders) {
        def mcPlatforms = currentLoaders.contains('neoforge') ? ['Fabric', 'Neoforge', 'Lexforge'] : ['Fabric', 'Lexforge']

        mcPlatforms.each { platform ->
            def platformLower = platform.toLowerCase()
            def remapJarTask = project.tasks.register("remap${platform}Jar", AbstractArchiveTask) {
                group = 'build'
            }

            def shadowTask = project.tasks.register("${platformLower}ShadowJar", ShadowJar) {
                dependsOn(remapJarTask)
                group = 'build'
                archiveClassifier = "${platformLower}-release"
                from(remapJarTask.map { it.outputs })
                configurations += [project.configurations.jarLibs]
                exclude 'META-INF/*.RSA'
                exclude 'META-INF/*.SF'
                exclude "**/module-info.class"
            }

            project.tasks.named('build').configure {
                finalizedBy(shadowTask)
            }
        }

        project.tasks.named('jar').configure {
            enabled = false
        }

        project.tasks.withType(JavaCompile).configureEach {
            options.compilerArgs += ["-AMC_VER=${mcVerValue}"]
        }

        project.tasks.withType(org.gradle.jvm.tasks.Jar).configureEach {
            from("LICENSE") {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
                rename { "${it}_${project.archivesBaseName}" }
            }
            manifest {
                attributes(
                    'Implementation-Title': 'MC-Runtime-Test',
                    'MixinConfigs': "mc_runtime_test.mixins.json",
                    'Implementation-Version': buildVersion
                )
                if (currentLoaders.contains('legacyfabric')) {
                    attributes(
                        'TweakClass': 'io.github.headlesshq.mcrt.tweaker.McRuntimeTestTweaker'
                    )
                }
            }
        }

        def expansions = [
            version    : buildVersion,
            mc_version : currentMinecraftVersion,
        ]

        project.tasks.named('processFabricResources').configure {
            filesMatching("fabric.mod.json") {
                expand(expansions)
            }
        }

        project.tasks.named('processLexforgeResources').configure {
            filesMatching("META-INF/mods.toml") { expand(expansions) }
            filesMatching("mcmod.info") { expand(expansions) }
        }

        if (currentLoaders.contains('neoforge')) {
            project.tasks.named('processNeoforgeResources').configure {
                filesMatching("META-INF/mods.toml") { expand(expansions) }
                filesMatching("META-INF/neoforge.mods.toml") { expand(expansions) }
            }
        }
    }

    private void configurePublishing(Project project, def buildVersion, def loaders) {
        def mcPlatforms = loaders.contains('neoforge') ? ['Fabric', 'Neoforge', 'Lexforge'] : ['Fabric', 'Lexforge']

        project.publishing {
            publications {
                create(project.name.toLowerCase(), MavenPublication) {
                    groupId = project.group
                    artifactId = project.archivesBaseName.toLowerCase()
                    version = project.version

                    from(project.components.java)

                    mcPlatforms.each { platform ->
                        def platformLower = platform.toLowerCase()
                        artifact project.tasks.named("${platformLower}Jar").get()
                        artifact project.tasks.named("remap${platform}Jar").get()
                        artifact project.tasks.named("${platformLower}ShadowJar").get()
                    }
                }
            }

            repositories {
                if (System.getenv('DEPLOY_TO_GITHUB_PACKAGES_URL') != null) {
                    maven {
                        name = 'GithubPagesMaven'
                        url = System.getenv('DEPLOY_TO_GITHUB_PACKAGES_URL')
                        credentials {
                            username = System.getenv('GITHUB_ACTOR')
                            password = System.getenv('GITHUB_TOKEN')
                        }
                    }
                } else {
                    maven {
                        name = 'BuildDirMaven'
                        url = project.layout.buildDirectory.dir('maven').get()
                    }
                }
            }
        }
    }
}
