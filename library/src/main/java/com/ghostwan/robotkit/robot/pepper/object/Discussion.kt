package com.ghostwan.robotkit.robot.pepper.`object`

import android.content.Context
import android.support.annotation.RawRes
import com.aldebaran.qi.sdk.`object`.conversation.Discuss
import com.aldebaran.qi.sdk.`object`.conversation.QiChatVariable
import com.aldebaran.qi.sdk.`object`.conversation.Topic
import com.ghostwan.robotkit.robot.pepper.ext.await
import com.ghostwan.robotkit.robot.pepper.ext.getLocalizedRaw
import com.ghostwan.robotkit.robot.pepper.ext.sha512
import com.ghostwan.robotkit.robot.pepper.util.info
import com.ghostwan.robotkit.robot.pepper.util.warning
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import java.util.regex.Pattern

/**
 * Created by erwan on 12/03/2018.
 */
@Serializable
data class Data(var variables: Map<String, String>, @Optional var bookmark: String? = null) {
    constructor() : this(HashMap(), null)
}

//Move in a specific extension
data class NAOqiData(var discuss: Discuss? = null,
                     var qiChatVariables: HashMap<String, QiChatVariable>,
                     var qiTopics : Map<Int, Topic>) {
    constructor() : this(null, HashMap(), HashMap())
}


class Discussion {
    var id: String = ""
    var mainTopic : Int
    var topics = HashMap<Int, String>()
    var data : Data = Data()
    var naoqiData : NAOqiData = NAOqiData()
    val locale : Locale?

    private var onBookmarkReached :((String) -> Unit)? = null
    private var onVariableChanged :((String, String) -> Unit)? = null

    constructor(context: Context, @RawRes vararg integers: Int, locale : Locale?=null) {
        this.locale = locale
        mainTopic = integers[0]
        for (integer in integers) {
            var content = context.getLocalizedRaw(integer, locale)
            id += context.getString(integer).substringAfterLast("/")
            content = addBookmarks(content, "(\\s*proposal:)(.*)", "rk-p")
            content = addBookmarks(content, "(\\s*u\\d+:\\([^)]*\\))(.*)", "rk-u")
            println(content)
            topics[integer] = content
        }
        id = id.sha512()
        println("Discussion id is $id")

    }

    fun setOnBookmarkReached(onBookmarkReached: ((name : String) -> Unit)? = null){
        this.onBookmarkReached = onBookmarkReached
    }
    fun setOnVariableChanged(onVariableChanged: ((name: String, value: String) -> Unit)? = null){
        this.onVariableChanged = onVariableChanged
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
        return reg.findAll(topics.values.joinToString { it })
                .map { it.groups[1]?.value?.replace("\$", "") }
                .distinct().map { it!! }.toList()
    }

    private fun getFilename() = "$id.data"

    suspend fun saveData(context: Context) {
        data.variables = getVariables().map {
            val variable = naoqiData.discuss?.async()?.variable(it).await()
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

    internal suspend fun prepare(discuss: Discuss, topics : Map<Int, Topic>) : String?{
        naoqiData.discuss = discuss
        naoqiData.qiChatVariables.clear()
        naoqiData.qiTopics = topics

        getVariables().forEach {key ->
            val qichatvar =  discuss.async()?.variable(key).await()
            naoqiData.qiChatVariables[key] = qichatvar
            qichatvar.setOnValueChangedListener {
                onVariableChanged?.invoke(key, it)
            }
            if(key in data.variables) {
                qichatvar.async().setValue(data.variables[key]).await()
            }
        }

        discuss.async().setOnBookmarkReachedListener {
            data.bookmark = it.name
            onBookmarkReached?.invoke(it.name)
        }
        return data.bookmark
    }

    suspend fun setVariable(name : String , value : String) {
        naoqiData.qiChatVariables[name]?.async()?.setValue(value).await()
    }

    suspend fun getVariable(name : String) : String {
        return naoqiData.qiChatVariables[name]?.async()?.value.await()
    }

    suspend fun gotoBookmark(name: String, topic: Int=mainTopic){
        val bookmark = naoqiData.qiTopics[topic]?.async()?.bookmarks.await()[name]
        naoqiData.discuss?.async()?.goToBookmarkedOutputUtterance(bookmark).await()
    }


}
