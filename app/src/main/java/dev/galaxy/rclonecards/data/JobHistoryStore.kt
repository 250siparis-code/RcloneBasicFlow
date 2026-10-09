package dev.galaxy.rclonecards.data

import android.content.Context
import dev.galaxy.rclonecards.model.JobHistoryEntry
import dev.galaxy.rclonecards.model.JobState
import dev.galaxy.rclonecards.model.JobStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object JobHistoryStore {
    private const val PREFS = "rclone_cards_history"
    private const val KEY = "history"
    private const val KEEP_MS = 72L * 60L * 60L * 1000L
    private lateinit var ctx: Context
    private val _entries = MutableStateFlow<List<JobHistoryEntry>>(emptyList())
    val entries: StateFlow<List<JobHistoryEntry>> = _entries.asStateFlow()

    fun init(context: Context) {
        if (::ctx.isInitialized) return
        ctx=context.applicationContext
        _entries.value=prune(load())
        save()
    }

    @Synchronized fun record(state: JobState, command: String) {
        if (state.status !in setOf(JobStatus.COMPLETED,JobStatus.ERROR,JobStatus.STOPPED)) return
        val end=state.finishedAtMillis ?: System.currentTimeMillis()
        val e=JobHistoryEntry(
            id="${state.cardId}:$end", cardId=state.cardId, title=state.title, command=command,
            status=state.status, progressPercent=state.progressPercent, bytes=state.bytes,
            totalBytes=state.totalBytes, transfers=state.transfers, totalTransfers=state.totalTransfers,
            startedAtMillis=state.startedAtMillis, finishedAtMillis=end, exitCode=state.exitCode,
            lastError=state.lastError
        )
        _entries.value=prune(listOf(e)+_entries.value.filterNot{it.id==e.id}).take(500)
        save()
    }

    @Synchronized fun pruneNow(){ _entries.value=prune(_entries.value); save() }
    @Synchronized fun clear(){ _entries.value=emptyList(); prefs().edit().remove(KEY).apply() }

    fun latestStates(): Map<String,JobState> {
        val out=linkedMapOf<String,JobState>()
        for(e in _entries.value.sortedByDescending{it.finishedAtMillis}){
            if(out.containsKey(e.cardId)) continue
            out[e.cardId]=JobState(
                cardId=e.cardId,title=e.title,status=e.status,
                progressPercent=if(e.status==JobStatus.COMPLETED)100 else e.progressPercent,
                bytes=e.bytes,totalBytes=e.totalBytes,transfers=e.transfers,totalTransfers=e.totalTransfers,
                lastError=e.lastError,exitCode=e.exitCode,startedAtMillis=e.startedAtMillis,
                finishedAtMillis=e.finishedAtMillis
            )
        }
        return out
    }

    private fun prune(x:List<JobHistoryEntry>) =
        x.filter{it.finishedAtMillis >= System.currentTimeMillis()-KEEP_MS}.sortedByDescending{it.finishedAtMillis}

    private fun prefs()=ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE)
    private fun save(){
        val a=JSONArray()
        _entries.value.forEach{e->a.put(JSONObject().apply{
            put("id",e.id);put("cardId",e.cardId);put("title",e.title);put("command",e.command)
            put("status",e.status.name);put("progress",e.progressPercent);put("bytes",e.bytes)
            put("totalBytes",e.totalBytes);put("transfers",e.transfers);put("totalTransfers",e.totalTransfers)
            put("start",e.startedAtMillis ?: JSONObject.NULL);put("end",e.finishedAtMillis)
            put("exit",e.exitCode ?: JSONObject.NULL);put("error",e.lastError ?: JSONObject.NULL)
        }})
        prefs().edit().putString(KEY,a.toString()).apply()
    }
    private fun load():List<JobHistoryEntry>{
        val raw=prefs().getString(KEY,null) ?: return emptyList()
        return runCatching{
            val a=JSONArray(raw)
            buildList{
                for(i in 0 until a.length()){
                    val o=a.getJSONObject(i)
                    val st=runCatching{JobStatus.valueOf(o.optString("status","ERROR"))}.getOrDefault(JobStatus.ERROR)
                    add(JobHistoryEntry(
                        id=o.optString("id"),cardId=o.optString("cardId"),title=o.optString("title","Task"),
                        command=o.optString("command"),status=st,progressPercent=o.optInt("progress"),
                        bytes=o.optLong("bytes"),totalBytes=o.optLong("totalBytes"),transfers=o.optLong("transfers"),
                        totalTransfers=o.optLong("totalTransfers"),
                        startedAtMillis=if(o.isNull("start"))null else o.optLong("start"),
                        finishedAtMillis=o.optLong("end"),exitCode=if(o.isNull("exit"))null else o.optInt("exit"),
                        lastError=if(o.isNull("error"))null else o.optString("error")
                    ))
                }
            }
        }.getOrElse{emptyList()}
    }
}
