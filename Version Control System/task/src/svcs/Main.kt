package svcs

import java.io.File
import java.io.InputStream
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.LocalDateTime

const val VCS = "vcs"
val VCS_COMMITS = "vcs${File.separator}commits"
val VCS_config = "vcs${File.separator}config.txt"
val VCS_index = "vcs${File.separator}index.txt"
val VCS_log = "vcs${File.separator}log.txt"
val l = File.separator

data class Log(val hash: String, val name: String, val comment: String)

fun main(args: Array<String>) {
    prepareFilesAndFolders()

    val namesOfindexedFiles = File(VCS_index).readText()
        .split("\n").filter { it != "" }.toMutableSet()

    val nameOfUser = File(VCS_config).readText()

    val commitsList = loadLog()

    val commands = buildMap {
        put("config", "Get and set a username.")
        put("add", "Add a file to the index.")
        put("log", "Show commit logs.")
        put("commit", "Save changes.")
        put("checkout", "Restore a file.")
    }

    if (args.isEmpty() || args[0] == "--help") {
        println("These are SVCS commands:")
        for (v in commands) {
            println("${"%-9s".format(v.key)} ${v.value}")
        }
    } else if (commands.containsKey(args[0])) {
        when (args[0]) {
            "add" -> addCommand(args, namesOfindexedFiles, commands)
            "config" -> configCommand(args, nameOfUser)
            "log" -> logCommand(commitsList)
            "commit" -> commitCommand(args, commitsList, nameOfUser, namesOfindexedFiles)
            "checkout" -> checkOutCommand(args, commitsList)
        }
    } else {
        println("'${args[0]}' is not a SVCS command.")
    }
}

private fun commitCommand(
    args: Array<String>,
    commitsList: MutableList<Log>,
    nameOfUser: String,
    namesOfindexedFiles: MutableSet<String>
) {
    if (args.size == 1) {
        println("Message was not passed.")
        return
    }
    if (commitsList.isEmpty()) {
        val newHashFolder = getSHA("${LocalDateTime.now()}${args[1]}$nameOfUser") ?: "error hash"
        Files.createDirectory(Paths.get("$VCS_COMMITS$l$newHashFolder"))
        newLogEntry(commitsList, newHashFolder, nameOfUser, userComment = args[1])

        namesOfindexedFiles.forEach {
            File(it).copyTo(File("$VCS_COMMITS$l$newHashFolder$l${File(it).name}"))
        }
        println("Changes are committed.")
        return
    }

    val realIndexedFiles = namesOfindexedFiles.map { File(it) }
    val filesToBeCopied = mutableSetOf<File>()

    fun findFile(findMe: String): Boolean {
        for (folder in commitsList) {
            val path = "$VCS_COMMITS$l${folder.hash}"
            val filesInFolder = File(path).listFiles().map { it.path.replace("$path$l", "") }

            for (i in filesInFolder) {
                if (findMe == i) {
                    val trackedFile = getSHA(File(findMe).readText()) ?: ""
                    val lastHashFile = getSHA(File("$path$l$i").readText()) ?: ""
                    return trackedFile != lastHashFile
                }
            }
        }
        return false
    }

    for (file in realIndexedFiles) {
        if (findFile(file.path)) {
            namesOfindexedFiles.forEach {
                filesToBeCopied.add(File(it))
            }
            break
        }
    }

    if (filesToBeCopied.isNotEmpty()) {
        val newHashFolder = getSHA("${LocalDateTime.now()}${args[1]}$nameOfUser") ?: "error hash"
        Files.createDirectory(Paths.get("$VCS_COMMITS$l$newHashFolder"))
        newLogEntry(commitsList, newHashFolder, nameOfUser, args[1])
        filesToBeCopied.forEach {
            it.copyTo(File("$VCS_COMMITS$l$newHashFolder$l${it.name}"))
        }
        println("Changes are committed.")
    } else {
        println("Nothing to commit.")
    }
}


fun checkOutCommand(args: Array<String>, commitsList: MutableList<Log>) {
    if (args.size == 1) {
        println("Commit id was not passed.")
        return
    }
    checkOut(args[1], commitsList)
}

fun checkOut(secondArg: String, commitsList: MutableList<Log>) {
    for (commit in commitsList) {
        if (commit.hash == secondArg) {
            val path = "$VCS_COMMITS$l${commit.hash}"
            val filesInFolder = File(path).listFiles()
            filesInFolder.forEach {
                val oldFile = File(it.path.replace("$path$l", ""))
                oldFile.delete()
                it.copyTo(File(it.path.replace("$path$l", "")))
            }

            println("Switched to commit ${commit.hash}.")
            return
        }
    }
    println("Commit does not exist.")
}

fun newLogEntry(commitsList: MutableList<Log>, hashFolder: String, nameOfUser: String, userComment: String) {
    commitsList.add(0, Log(hashFolder, nameOfUser, userComment))
    val str = buildString {
        commitsList.forEach {
            append("commit ${it.hash}\nAuthor: ${it.name}\n${it.comment}\n\n")
        }
    }
    File(VCS_log).writeText(str)
}

private fun loadLog(): MutableList<Log> {
    val inputStream: InputStream = File(VCS_log).inputStream()
    val logList = mutableListOf<Log>()
    var hash = ""
    var name = ""
    var comment = ""
    var j = -1
    inputStream.bufferedReader().useLines { lines ->
        lines.forEach { value ->
            j++
            when (j) {
                0 -> hash = value.split(" ").last()
                1 -> name = value.split(" ").last()
                2 -> {
                    comment = value
                    logList.add(Log(hash = hash, name = name, comment = comment))
                }
                3 -> j = -1
            }
        }
    }
    return logList
}

private fun logCommand(logsList: MutableList<Log>) {
    if (logsList.isEmpty()) {
        println("No commits yet.")
    } else {
        logsList.forEach {
            println("commit ${it.hash}\nAuthor: ${it.name}\n${it.comment}")
            println()
        }
    }
}

fun buildFoldersPath() {
    val dirsToCreate = "1\\2\\3\\4".split("\\")
    var newFolder = "$VCS${File.separator}"
    for (i in dirsToCreate) {
        newFolder += "${i}${File.separator}"
        Files.createDirectory(Paths.get(newFolder))
        println(newFolder)

    }
}

private fun prepareFilesAndFolders() {
    if (!File(VCS).exists())
        Files.createDirectory(Paths.get(VCS))
    if (!File(VCS_COMMITS).exists())
        Files.createDirectory(Paths.get(VCS_COMMITS))

    File(VCS_config).createNewFile()
    File(VCS_index).createNewFile()
    File(VCS_log).createNewFile()
}

private fun configCommand(
    args: Array<String>,
    nameOfUser: String,
) {
    if (args.size == 1) {
        if (nameOfUser.isEmpty()) {
            println("Please, tell me who you are.")
        } else {
            println("The username is $nameOfUser.")
        }

    } else {
        println("The username is ${args[1]}.")
        File(VCS_config).writeText(args[1])

    }
}


private fun addCommand(
    args: Array<String>,
    indexedFiles: MutableSet<String>,
    commands: Map<String, String>
) {
    if (args.size == 1) {
        if (indexedFiles.isEmpty()) {
            println(commands[args[0]])
        } else {
            println("Tracked files:")
            indexedFiles.forEach { println(it) }
        }
    } else {
        if (File(args[1].trim()).exists() && File(args[1].trim()).isFile) {
            println("The file '${args[1]}' is tracked.")
            indexedFiles.add(args[1])
            File(VCS_index).writeText(indexedFiles.joinToString("\n"))
        } else {
            println("Can't find '${args[1]}'.")
        }
    }
}

fun getSHA(input: String): String? {
    try {
        val md = MessageDigest.getInstance("SHA-256")
        val messageDigest = md.digest(input.toByteArray())
        val num = BigInteger(1, messageDigest)
        var hashText = num.toString(16)
        while (hashText.length < 32) {
            hashText = "0$hashText"
        }
        return hashText
    } catch (ex: NoSuchAlgorithmException) {
        println("Exception Occured: ${ex.message}")
        return null
    }
}