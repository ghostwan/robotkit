package com.ghostwan.robotkit.robot.pepper.`object`

import android.content.Context
import com.aldebaran.qi.sdk.`object`.conversation.Discuss
import com.aldebaran.qi.sdk.util.IOUtils
import com.ghostwan.robotkit.robot.pepper.MyPepper.Companion.info
import com.ghostwan.robotkit.robot.pepper.MyPepper.Companion.warning
import com.ghostwan.robotkit.robot.pepper.ext.await
import com.ghostwan.robotkit.robot.pepper.ext.sha512
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import java.io.File
import java.io.FileNotFoundException
import java.util.regex.Pattern

/**
 * Created by erwan on 12/03/2018.
 */
@Serializable
data class Data(var variables: Map<String, String>, @Optional var bookmark: String? = null) {
    constructor() : this(HashMap(), null)
}

class Discussion {
    var id: String = ""
    var topics = ArrayList<String>()
    var data : Data = Data()

    var onBookmarkReached :((String) -> Unit)? = null
    private var discuss: Discuss? = null

    constructor(context: Context, vararg integers: Int, onBookmarkReached: ((String) -> Unit)? = null) {
        this.onBookmarkReached = onBookmarkReached
        for (integer in integers) {
            var content = IOUtils.fromRaw(context, integer)
            id += context.getString(integer).substringAfterLast("/")
            content = addBookmarks(content, "(\\s*proposal:)(.*)", "rk-p")
            content = addBookmarks(content, "(\\s*u\\d+:\\([^)]*\\))(.*)", "rk-u")
            println(content)
            topics.add(content)
        }
        id = id.sha512()
        println("Discussion id is $id")

    }

    private fun addBookmarks(content : String, regex : String, template : String) : String {
        val pattern = Pattern.compile(regex)
        val matcher = pattern.matcher(content)
        matcher.matches()
        val buffer = StringBuffer()
        var i = 1
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "$1 %$template$i $2")
            i++
        }
        return matcher.appendTail(buffer).toString()
    }

    fun getVariables(): List<String> {
        val reg = "(\\\$\\w+)".toRegex()
        return reg.findAll(topics.joinToString { it })
                .map { it.groups[1]?.value?.replace("\$", "") }
                .distinct().map { it!! }.toList()
    }

    private fun getFilename() = "$id.data"

    suspend fun saveData(context: Context) {
        data.variables = getVariables().map {
            val variable = discuss?.async()?.variable(it).await()
            var value = variable.async().value.await()
            if(value == "")
                value=" "
            it to value
        }.toMap()

        val json = JSON.stringify(data)

        val filename = getFilename()
        context.openFileOutput(filename, Context.MODE_PRIVATE).use {
            it.write(json.toByteArray())
        }
        info("Data saved : $json")
    }

    fun restoreData(context: Context,
                    restoreVariable : Boolean = true,
                    restoreState : Boolean = true): Boolean {
        val filename = getFilename()

        val json: String?
        return try {
            json = context.openFileInput(filename).use {
                it.bufferedReader().use { it.readLine() }
            }
            val dataLocal : Data = JSON.parse(json)
            info("get json data : $json")
            if(restoreState && dataLocal.bookmark != null) {
                info("state ${dataLocal.bookmark} restored")
                data.bookmark = dataLocal.bookmark
            }
            if(restoreVariable) {
                info("variables ${dataLocal.variables} restored")
                data.variables = dataLocal.variables
            }
            true
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            false
        }
    }

    fun clearData() {
        val file = File(getFilename());
        if(file.delete())
            info("File ${getFilename()} deleted")
        else
            warning("Fail to delete ${getFilename()}!")
        data = Data()
    }

    internal suspend fun prepare(discuss: Discuss) : String?{
        this.discuss = discuss
        data.variables.forEach { (key, value) ->
            val qichatvar =  discuss?.async()?.variable(key).await()
            qichatvar.async().setValue(value).await()
        }

        discuss.async().setOnBookmarkReachedListener {
            data.bookmark = it.name
            onBookmarkReached?.invoke(it.name)
        }
        return data.bookmark
    }


}