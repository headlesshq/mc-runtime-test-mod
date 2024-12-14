package io.github.headlesshq.mcrt

import org.gradle.api.*
import org.gradle.api.plugins.*
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

class VersionConventionPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {

        project.pluginManager.apply('java')
        project.pluginManager.apply('maven-publish')
        project.pluginManager.apply('xyz.wagyourtail.unimined')
        project.pluginManager.apply('com.github.johnrengelman.shadow')
        project.pluginManager.apply('systems.manifold.manifold-gradle-plugin')

        def verId = project.name
        println "Configuring subproject: ${project.path}, version=${verId}"
        if (verId == 'shared') {
            return
        }
        if (verId == 'api') {
            return
        }

        def propsFile = new File(project.rootDir, "versions/${verId}/gradle.properties")
        if (!propsFile.exists()) {
            throw new GradleException("Properties file not found: ${propsFile}")
        }

        project.gradle.ext.getProperties().each { prop ->
            project.rootProject.ext.set(prop.key, prop.value)
        }

        def setupPreprocessors = { List<String> mcVersions, String currentVersion ->
            StringBuilder sb = new StringBuilder()
            mcVersions.eachWithIndex { ver, i ->
                String verStr = ver.replace(".", "_")
                sb.append("MC_${verStr}=${i}\n")
                if (ver == currentVersion) {
                    sb.append("MC_VER=${i}\n")
                }
            }
            new File(project.projectDir, "build.properties").text = sb.toString()
        }

        def props = new Properties()
        props.load(new FileInputStream(propsFile))

        def currentMinecraftVersion = props.getProperty('minecraft_version')
        def currentLexforgeVersion = props.getProperty('lexforge_version')
        def currentNeoForgeVersion = props.getProperty('neoforge_version')
        def currentLoaders = props.getProperty('loaders')?.split(',')*.trim()
        def currentMappings = props.getProperty('mappings')

        println "'minecraft_version' = ${currentMinecraftVersion}"
        println "'lexforge_version'  = ${currentLexforgeVersion}"
        println "'neoforge_version'  = ${currentNeoForgeVersion}"
        println "'loaders'           = ${currentLoaders}"
        println "'mappings'          = ${currentMappings}"

        setupPreprocessors(project.rootProject.mc_versions, currentMinecraftVersion)

        def buildVersion = "${currentMinecraftVersion}-${project.rootProject.project(':api').project_version}"

        project.sourceSets {
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

        project.unimined.minecraft {
            version currentMinecraftVersion
            mappings {
                currentMappings.split(',').each { mapping ->
                    String[] split = mapping.split(':')
                    String name = split[0]
                    if (name == 'searge') {
                        searge()
                    } else if (name == 'mcp') {
                        mcp("stable", split[1])
                    } else if (name == 'mojmap') {
                        mojmap()
                        devFallbackNamespace "mojmap"
                    }
                }
            }
            defaultRemapJar = false
        }

        project.unimined.minecraft(project.sourceSets.fabric) {
            combineWith(project.sourceSets.main)
            if (currentLoaders.contains('legacyfabric')) {
                legacyFabric {
                    loader project.rootProject.fabric_version
                }
            } else {
                fabric {
                    loader project.rootProject.fabric_version
                }
            }
            defaultRemapJar = true
        }

        if (currentLoaders.contains('neoforge')) {
            project.unimined.minecraft(project.sourceSets.neoforge) {
                combineWith(project.sourceSets.main)
                neoForge {
                    loader currentNeoForgeVersion
                    mixinConfig 'mc_runtime_test.mixins.json'
                }
                minecraftRemapper.config {
                    ignoreConflicts(true)
                }
                defaultRemapJar = true
            }
        }

        project.unimined.minecraft(project.sourceSets.lexforge) {
            combineWith(project.sourceSets.main)
            minecraftForge {
                loader currentLexforgeVersion
                mixinConfig 'mc_runtime_test.mixins.json'
            }
            defaultRemapJar = true
        }

        project.configurations {
            mainImplementation
            implementation {
                extendsFrom lexforgeImplementation
                extendsFrom fabricImplementation
            }
            jarLibs
            implementation.extendsFrom jarLibs
        }
        project.sourceSets.main {
            compileClasspath += project.configurations.mainImplementation
            runtimeClasspath += project.configurations.mainImplementation
        }

        project.dependencies {
            annotationProcessor("systems.manifold:manifold-preprocessor:2024.1.37")
            lexforgeAnnotationProcessor("systems.manifold:manifold-preprocessor:2024.1.37")
            fabricAnnotationProcessor("systems.manifold:manifold-preprocessor:2024.1.37")
            if (currentLoaders.contains('neoforge')) {
                neoforgeAnnotationProcessor("systems.manifold:manifold-preprocessor:2024.1.37")
            }

            if (currentLoaders.contains('legacyfabric')) {
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


        def mc_platforms = ['Fabric', 'Lexforge']
        if (currentLoaders.contains('neoforge')) {
            mc_platforms = ['Fabric', 'Neoforge', 'Lexforge']
        }

        for (String platform_capitalized : mc_platforms) {
            def platform = platform_capitalized.toLowerCase()
            def remapJarTask = project.tasks.named("remap${platform_capitalized}Jar", AbstractArchiveTask).get()
            def shadowTask = project.tasks.register("${platform}ShadowJar", ShadowJar) {
                dependsOn(remapJarTask)
                group = 'build'
                archiveClassifier = "${platform}-release"
                from remapJarTask.outputs
                configurations += [ project.configurations.jarLibs ]
                exclude 'META-INF/*.RSA'
                exclude 'META-INF/*.SF'
                exclude "**/module-info.class"
            }
            project.tasks.named('build') {
                finalizedBy(shadowTask)
            }
        }

        project.tasks.named('jar') {
            enabled = false
        }

        project.tasks.withType(org.gradle.jvm.tasks.Jar).configureEach {
            from("LICENSE") {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
                rename { "${it}_${archivesBaseName}" }
            }
            manifest {
                if (currentLoaders.contains('legacyfabric')) {
                    attributes(
                        'Implementation-Title': 'MC-Runtime-Test',
                        'TweakClass': 'io.github.headlesshq.mcrt.tweaker.McRuntimeTestTweaker',
                        'MixinConfigs': "mc_runtime_test.mixins.json",
                        'Implementation-Version': buildVersion
                    )
                } else {
                    attributes(
                        'Implementation-Title': 'MC-Runtime-Test',
                        'MixinConfigs': "mc_runtime_test.mixins.json",
                        'Implementation-Version': buildVersion
                    )
                }
            }
        }

        def expansions = [
            version    : buildVersion,
            mc_version : currentMinecraftVersion,
        ]

        project.tasks.named('processFabricResources') {
            filesMatching("fabric.mod.json") {
                expand expansions
            }
        }
        project.tasks.named('processLexforgeResources') {
            filesMatching("META-INF/mods.toml") {
                expand expansions
            }
            filesMatching("mcmod.info") {
                expand expansions
            }
        }
        if (currentLoaders.contains('neoforge')) {
            project.tasks.named('processNeoforgeResources') {
                filesMatching("META-INF/mods.toml") {
                    expand expansions
                }
                filesMatching("META-INF/neoforge.mods.toml") {
                    expand expansions
                }
            }
        }

        // Publishing configuration
        project.afterEvaluate {
            project.publishing {
                publications {
                    create(project.name.toLowerCase(), MavenPublication) {
                        groupId    = project.group
                        artifactId = project.archivesBaseName.toLowerCase()
                        version    = project.version

                        from project.components.java

                        for (String platform: mc_platforms) {
                            String pLower = platform.toLowerCase()
                            artifact project.tasks.named("${pLower}Jar").get()
                            artifact project.tasks.named("remap${platform}Jar").get()
                            artifact project.tasks.named("${pLower}ShadowJar").get()
                        }
                    }
                }

                repositories {
                    if (System.getenv('DEPLOY_TO_GITHUB_PACKAGES_URL') == null) {
                        maven {
                            name = 'BuildDirMaven'
                            url = project.rootProject.projectDir.toPath().parent
                                       .resolve('build')
                                       .resolve('maven')
                        }
                    } else {
                        maven {
                            name = 'GithubPagesMaven'
                            url = System.getenv('DEPLOY_TO_GITHUB_PACKAGES_URL')
                            credentials {
                                username = System.getenv('GITHUB_USER')
                                password = System.getenv('GITHUB_TOKEN')
                            }
                        }
                    }
                }
            }
        }
    }
}
