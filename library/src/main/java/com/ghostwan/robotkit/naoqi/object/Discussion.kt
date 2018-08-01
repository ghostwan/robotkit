package com.ghostwan.robotkit.naoqi.`object`

import android.content.Context
import android.support.annotation.RawRes
import com.aldebaran.qi.sdk.`object`.conversation.*
import com.ghostwan.robotkit.`object`.Action
import com.ghostwan.robotkit.ext.getLocalizedRaw
import com.ghostwan.robotkit.ext.sha512
import com.ghostwan.robotkit.naoqi.NaoqiRobot
import com.ghostwan.robotkit.naoqi.ext.await
import com.ghostwan.robotkit.util.infoLog
import com.ghostwan.robotkit.util.warningLog
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap

/**
 * Created by erwan on 12/03/2018.
 */
@Serializable
data class Data(var variables: Map<String, String>, @Optional var bookmark: String? = null) {
    constructor() : this(HashMap(), null)
}

//Move in a specific extension
data class NAOqiData(var qiChatbot: QiChatbot? = null,
                     var qiChatVariables: HashMap<String, QiChatVariable>,
                     var qiChatExecutors: HashMap<String, QiChatExecutor>?,
                     var qiTopics : Map<Int, Topic>) {
    constructor() : this(null, HashMap(), HashMap(), HashMap()  )
}

/**
 * Object that handle discussion data.
 *
 * This is useful for power use of the discuss API:
 * - The Discussion can be save and restore
 * - Specific variable can be retrieved
 * - Bookmarks and variables can be followed
 * - Possibility to go to a specific bookmark while the discussion is running
 */
class Discussion{
    var id: String = ""
    var mainTopic : Int
    var topics = HashMap<Int, String>()
    var data : Data = Data()
    var naoqiData : NAOqiData = NAOqiData()
    val locale : Locale?

    private var onBookmarkReached :((String) -> Unit)? = null
    private var onVariableChanged :((String, String) -> Unit)? = null

    /**
     * Construct a Discussion object that will handle the discussion data
     *
     * @param context Android context
     * @param topicsRes Android resource id of the topics to load in the discussion, the first one is the main one.
     * The topic content depend of the locale
     * @param locale The locale of the discussion, if it's null, use the one of the device.
     */
    constructor(context: Context, @RawRes vararg topicsRes: Int, locale : Locale?=null) {
        this.locale = locale
        mainTopic = topicsRes[0]
        for (integer in topicsRes) {
            var content = context.getLocalizedRaw(integer, locale)
            id += context.getString(integer).substringAfterLast("/")
            content = addBookmarks(content, "(\\s*proposal:)(.*)", "rk-p")
            content = addBookmarks(content, "(\\s*u\\d+:\\([^)]*\\))(.*)", "rk-u")
            this.topics[integer] = content
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

    internal suspend fun prepare(qiChatbot: QiChatbot, topics : Map<Int, Topic>) : String?{
        naoqiData.qiChatbot = qiChatbot
        naoqiData.qiChatVariables.clear()
        naoqiData.qiTopics = topics

        qiChatbot.async().setExecutors(naoqiData.qiChatExecutors).await()

        getVariables().forEach {key ->
            val qichatvar =  qiChatbot.async()?.variable(key).await()
            naoqiData.qiChatVariables[key] = qichatvar
            qichatvar.async().addOnValueChangedListener {
                onVariableChanged?.invoke(key, it)
            }.await()
            if(key in data.variables) {
                qichatvar.async().setValue(data.variables[key]).await()
            }
        }

        qiChatbot.async().addOnBookmarkReachedListener {
            data.bookmark = it.name
            onBookmarkReached?.invoke(it.name)
        }.await()
        return data.bookmark
    }

    /**
     * Set the listener which be called when a bookmark is reached in the discussion
     *
     * @param onBookmarkReached the callback called when a bookmark is reached with the name of the bookmark reached
     */
    fun setOnBookmarkReached(onBookmarkReached: ((name : String) -> Unit)? = null){
        this.onBookmarkReached = onBookmarkReached
    }

    /**
     * Set the listener which be called when a variable change
     *
     * @param onVariableChanged the callback called when a variable change with the name of the variable and its value
     */
    fun setOnVariableChanged(onVariableChanged: ((name: String, value: String) -> Unit)? = null){
        this.onVariableChanged = onVariableChanged
    }

    /**
     * Get the list of variable's name in this discussion
     *
     * @return the list of variables
     */
    fun getVariables(): List<String> {
        val reg = "(\\\$\\w+)".toRegex()
        return reg.findAll(topics.values.joinToString { it })
                .map { it.groups[1]?.value?.replace("\$", "") }
                .distinct().map { it!! }.toList()
    }

    private fun getFilename() = "$id.data"

    /**
     * Save discussion data; variables and last bookmark reached
     * @param context Android Context
     */
    suspend fun saveData(context: Context) {
        data.variables = getVariables().map {
            val variable = naoqiData.qiChatbot?.async()?.variable(it).await()
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
        infoLog("Data saved : $json")
    }

    /**
     * Restore discussion data
     *
     * @param context Android Context
     * @param restoreVariable Does it restore the discussion variables, yes by default.
     * @param restoreState Does it restore the discussion state (ie goes back to the last bookmark reached), yes by default.
     *
     * @return true if the discussion is restored, false otherwise (bookmark or variables can't be restored)
     */
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
            infoLog("get json data : $json")
            if(restoreState) {
                if (dataLocal.bookmark != null) {
                    infoLog("state ${dataLocal.bookmark} restored")
                    data.bookmark = dataLocal.bookmark
                }
                else {
                    return false
                }
            }
            if(restoreVariable) {
                if (dataLocal.variables.isNotEmpty()) {
                    infoLog("variables ${dataLocal.variables} restored")
                    data.variables = dataLocal.variables
                }
                else {
                    return false
                }

            }
            true
        } catch (e: FileNotFoundException) {
            false
        }
    }

    /**
     * Clear discussion data
     */
    fun clearData() {
        val file = File(getFilename());
        if(file.delete())
            infoLog("File ${getFilename()} deleted")
        else
            warningLog("Fail to delete ${getFilename()}!")
        data = Data()
    }

    /**
     * Set the value of a discussion variable
     *
     * @param name Name of the variable
     * @param value Value of the variable to set
     */
    suspend fun setVariable(name : String , value : String) {
        naoqiData.qiChatVariables[name]?.async()?.setValue(value).await()
    }

    /**
     * Get a discussion variable
     *
     * @param name Name of the variable to get
     * @return the value of the variable
     */
    suspend fun getVariable(name : String) : String {
        return naoqiData.qiChatVariables[name]?.async()?.value.await()
    }

    /**
     * Go to a specific bookmark
     *
     * @param name Name of the bookmark to go to
     * @param topic Android resource id of the topic where is the bookmark, by default use the main topic
     */
    suspend fun gotoBookmark(name: String, topic: Int=mainTopic,
                             importance: AutonomousReactionImportance=AutonomousReactionImportance.HIGH,
                             validity: AutonomousReactionValidity=AutonomousReactionValidity.IMMEDIATE
    ){
        val bookmark = naoqiData.qiTopics[topic]?.async()?.bookmarks.await()[name]
        naoqiData.qiChatbot?.async()?.goToBookmark(bookmark, importance, validity).await()
    }

    /**
     * set a QiChat executor
     *
     * @param robot The Naoqi Robot that will execute this action
     * @param name Name of the executor
     * @param onStopExecute Function call when the execution need to stop
     * @param onExecute Function call when the execution start
     */
    suspend fun setExecutor(robot: NaoqiRobot, name: String, onStopExecute: (suspend ()-> Unit)={robot.stopAllBut(Action.DISCUSSING)}, onExecute: (suspend (params: List<String>?) -> Unit)) {

        naoqiData.qiChatExecutors?.set(name, object : RKQiChatExecutor(robot.services.serializer) {
            override fun runWith(params: List<String>) {
                runBlocking {
                    onExecute(params)
                }
            }

            override fun stop() {
                runBlocking {
                    onStopExecute()
                }
            }

        })
    }


}
