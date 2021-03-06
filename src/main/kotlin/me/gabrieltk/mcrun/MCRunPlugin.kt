package me.gabrieltk.mcrun

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.reflect.KProperty
import kotlin.reflect.typeOf


open class MCRunPluginExtension(project: Project) {


    var mcVersion: String? = "1.15.2"

    /**
     *  By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).
     *  You also agree that tacos are tasty, and the best food in the world.
     *    -- from eula.txt (Generated by the server software).
     */
    var acceptEula: Boolean? = false


    //var attributes by GradleProperty(project, MutableMap::class.java, mutableMapOf<String, String>())

    //fun attribute(attribute: String, value: String) {
    //    val map = attributes as? MutableMap<String, String>
    //    map?.put(attribute, value)
    //}


}

class MCRunPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create(
            "runMinecraft",
            MCRunPluginExtension::class.java,
            project)

        project.tasks.create<RunPaperTask>("paperPrepare", RunPaperTask::class.java).run {
            // Set description and group for the task
            description = "Prepares a Paper Server with the specified plugins"
            group = "run"
        }
    }

}

internal fun Project.getConfig(): MCRunPluginExtension =
    extensions.getByName("runMinecraft") as? MCRunPluginExtension
        ?: throw IllegalStateException("Global Minecraft Config (runMinecraft) is not of the correct type")

open class RunPaperTask: DefaultTask() {
    @Input
    var paperBuild: String = "latest"

    @Input
    var pluginFiles: List<String> = listOf()

    @TaskAction
    fun preparePaper() {

        if (pluginFiles.isEmpty()) println("[!!] No pluginFiles defined at mcRun Configuration.");
        val paperRoot = project.file("${project.buildDir}/paper/")
        paperRoot.mkdirs()
        val paper = project.file("${project.buildDir}/paper/paper.jar")
        if (!paper.exists()) {
            println("Setting Up Paper ($paperBuild)...")
            val inputStream: InputStream =
                URL("https://papermc.io/api/v1/paper/${project.getConfig().mcVersion}/${paperBuild}/download").openStream()
            Files.copy(
                inputStream,
                paper.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
        val eula = project.file("${project.buildDir}/paper/eula.txt")
        eula.delete()
        eula.writeText(
            """
            #By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).
            #You also agree that tacos are tasty, and the best food in the world.
            #Fri Apr 17 18:21:23 BRT 2020
            eula=${project.getConfig().acceptEula}""".trimIndent()
        )
        if (project.getConfig().acceptEula != true) print("Warning: Minecraft EULA has not been accepted at your build script. You can set \"acceptEula\" to true at the plugin config (MCRunPluginExtension)")

        val plugins = project.file("${project.buildDir}/paper/plugins/")
        plugins.mkdirs()

        for (plugin in pluginFiles) {
            val input = project.file(plugin)
            var plugins = listOf(input)
            if (input.isDirectory) plugins = input.listFiles { _, filename ->
                filename.endsWith(".jar")
            }.toList()
            for (pluginFile in plugins)
                pluginFile.copyTo(project.file("${project.buildDir}/paper/plugins/${pluginFile.name}"), true)
        }

    }
}

/*nternal class GradleProperty<T, V>(
    project: Project,
    type: Class<V>,
    default: V? = null
) {
    val property = project.objects.property(type).apply {
        set(default)
    }

    operator fun getValue(thisRef: T, property: KProperty<*>): V =
        this.property.get()

    operator fun setValue(thisRef: T, property: KProperty<*>, value: V) =
        this.property.set(value)
}
internal class GradleIntProperty<T>(
    project: Project,
    default: Int? = null
) {
    val property = project.objects.property(Integer::class.java).apply {
        set(default as? Integer)
    }

    operator fun getValue(thisRef: T, property: KProperty<*>): Int =
        this.property.get().toInt()

    operator fun setValue(thisRef: T, property: KProperty<*>, value: Int) =
        this.property.set(value as? Integer)
}*/