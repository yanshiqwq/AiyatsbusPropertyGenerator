import net.sourceforge.argparse4j.ArgumentParsers
import net.sourceforge.argparse4j.inf.ArgumentParser
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.writeText

// By: yanshiqwq (https://github.com/yanshiqwq)
// Usage: java -jar {jar_path} -i {input_folder} -o {output_folder} -p {package_name}
// Example: java -jar .\AiyatsbusPropertyGenerator-1.0-SNAPSHOT.jar -i F:\yanshiqwq\Project\AiyatsbusPropertyGenerator\data\bukkit\src\main\java\org\bukkit\ -o F:\yanshiqwq\Project\AiyatsbusPropertyGenerator\data\test -p org.bukkit

fun main(args: Array<String>) {
    val parser = createArgumentParser()
    val namespace = parser.parseArgs(args)

    var inputDirectory = namespace.getString("input")
    var outputDirectory = namespace.getString("output")

    if (!inputDirectory.endsWith("\\")) inputDirectory += "\\"
    if (!outputDirectory.endsWith("\\")) outputDirectory += "\\"
    val packageName = namespace.getString("package")

    processInputAndOutput(inputDirectory, outputDirectory, packageName)
}

fun createArgumentParser(): ArgumentParser {
    val parser = ArgumentParsers.newFor("AiyatsbusPropertyGenerator").build()
        .defaultHelp(true)
        .description("Generate Kotlin property classes from Java getter methods.")
    parser.addArgument("-i", "--input")
        .dest("input")
        .type(String::class.java)
        .required(true)
        .help("Input directory containing Java files.")
    parser.addArgument("-o", "--output")
        .dest("output")
        .type(String::class.java)
        .required(true)
        .help("Output directory to save Kotlin files.")
    parser.addArgument("-p", "--package")
        .dest("package")
        .type(String::class.java)
        .required(true)
        .help("Package name for generated Kotlin files.")
    return parser
}

fun processInputAndOutput(inputDirectory: String, outputDirectory: String, packageName: String) {
    val inputDir = File(inputDirectory)
    val outputDir = File(outputDirectory)

    if (!inputDir.isDirectory) {
        println("Input directory does not exist or is not a directory.")
        return
    }

    if (!outputDir.isDirectory && !outputDir.mkdirs()) {
        println("Failed to create output directory.")
        return
    }

    for (javaFile: File in inputDir.walkTopDown().filter { it.isFile && it.extension == "java" }) {
        // 读取 Java 文件
        val className = javaFile.nameWithoutExtension
        val javaCode = javaFile.readText()
        val getterRegex = Regex("(get|is|has|can)[A-Z][a-zA-Z]*\\(\\) \\{", RegexOption.MULTILINE)
        val setterRegex = Regex("(set)[A-Z][a-zA-Z]*\\((Boolean|Short|Int|Long|Float|Double|List)\\) \\{", RegexOption.MULTILINE)
        val getterList = ArrayList<String>()
        val setterMap = mutableMapOf<String, String>()
        if (!getterRegex.containsMatchIn(javaCode)) continue

        val getterMatches = getterRegex.findAll(javaCode)
        for (it in getterMatches) {
            val match = it.groups[0]!!.value.removeSuffix(" {")
            if (listOf("isCancelled()", "getHandlers()", "getHandlerList()").contains(match)) continue
            val method = when {
                match.startsWith("get") -> match.removePrefix("get")
                match.startsWith("is") -> match.removePrefix("is")
                match.startsWith("has") -> match.removePrefix("has")
                else -> continue
            }.removeSuffix("()").replaceFirstChar { it.lowercase(Locale.getDefault()) }
            getterList.add(method)
        }
        try {
            if (setterRegex.containsMatchIn(javaCode)) {
                val setterMatches = setterRegex.findAll(javaCode)
                setterMatches.forEach { it ->
                    val match = it.groups[0]!!.value.removeSuffix("() {").split(" ")
                    val type = match[0]
                    val method = match[1].replaceFirstChar { it.lowercase(Locale.getDefault()) }.removePrefix("set")
                    setterMap[type] = method
                }
            }
        } catch (ignored: IndexOutOfBoundsException) {} // wtf is this

        val outputFile = Path(
            javaFile.path.replace(inputDirectory.replace("\\\\", "\\"), outputDirectory.replace("\\\\", "\\"))
                .replace(".java", ".kt")
        )

        if (!outputFile.exists() && getterList.isNotEmpty()) {
            outputFile.parent.toFile().mkdirs()
            outputFile.toFile().createNewFile()
            println("Processing ${outputFile.name} ...")
            outputFile.writeText(generateKotlinPropertyClass(packageName, className, getterList, setterMap))
        }
    }
}

fun camelCaseToHyphenated(text: String): String {
    return text.replace(Regex("([a-z])([A-Z])")) {
        "${it.groups[1]?.value}-${it.groups[2]?.value?.lowercase(Locale.getDefault())}"
    }.lowercase(Locale.getDefault())
}

fun generateKotlinPropertyClass(
    packageName: String,
    className: String,
    getterList: ArrayList<String>,
    setterMap: Map<String, String>
): String {
    val classNameByHyphen = camelCaseToHyphenated(className)
    var getterCode = ""
    var setterCode = ""
    val indent = "                    "
    var getterFirst = true
    var setterFirst = true
    getterList.forEach {
        if (getterFirst) {
            getterCode = "\"$it\" -> instance.$it\n"
            getterFirst = false
        } else {
            getterCode += "$indent\"$it\" -> instance.$it\n"
        }
    }
    setterMap.forEach { (type, method) ->
        if (setterFirst) {
            setterCode = "\"$method\" -> instance.$method = value?.coerce$type() ?: return OpenResult.successful()"
            setterFirst = false
        } else {
            setterCode += "$indent\"$method\" -> instance.$method = value?.coerce$type() ?: return OpenResult.successful()"
        }
    }
    return """
        package com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.$className

        import com.mcstarrysky.aiyatsbus.module.kether.AiyatsbusGenericProperty
        import com.mcstarrysky.aiyatsbus.module.kether.AiyatsbusProperty
        import $packageName.$className
        import taboolib.common.OpenResult

        /**
         * Aiyatsbus
         * com.mcstarrysky.aiyatsbus.module.kether.property.bukkit.$className
         * 
         * @author yanshiqwq
         * @since ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/M/d HH:mm"))}
         * 
         * # Generated by AiyatsBusPropertyGenerator #
         * 
         */
         
        @AiyatsbusProperty(
            id = "$classNameByHyphen",
            bind = $className::class
        )
        class Property$className : AiyatsbusGenericProperty<$className>("$classNameByHyphen") {

            override fun readProperty(instance: $className, key: String): OpenResult {
                val property: Any? = when (key) {
                    $getterCode
                    else -> OpenResult.failed()
                }
                return OpenResult.successful(property)
            }

            override fun writeProperty(instance: $className, key: String, value: Any?): OpenResult {
                ${
        if (setterMap.isEmpty()) {
            "return OpenResult.failed()"
        } else {
            """
                            when (key) {
                                $setterCode
                                else -> return OpenResult.failed()
                            }
                            return OpenResult.successful()
                        """.trimIndent()
        }
    }
            }
    """.trimIndent()
}
