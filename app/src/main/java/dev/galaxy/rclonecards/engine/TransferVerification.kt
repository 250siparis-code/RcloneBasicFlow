package dev.galaxy.rclonecards.engine

import org.json.JSONArray

object TransferVerification {
    // Bare directory copy/sync commands have no filters, renaming or config overrides.
    // More complex commands retain rclone's own verification semantics.
    fun destination(command: String): String? {
        val args = runCatching { ShellWords.parse(command) }.getOrNull()?.toMutableList() ?: return null
        if (args.firstOrNull()?.substringAfterLast('/') == "rclone") args.removeAt(0)
        if (args.size != 3 || args[0] !in setOf("copy", "sync")) return null
        return args[2].takeIf { Regex("^[A-Za-z0-9._-]+:.*").matches(it) }
    }

    fun validate(expected: Map<String, Long?>, listing: JSONArray): String? {
        val files = (0 until listing.length()).map { listing.getJSONObject(it) }
            .groupBy { it.optString("Path") }
        for ((path, size) in expected) {
            val matches = files[path].orEmpty()
            if (matches.isEmpty()) return "Destination file missing: $path"
            if (matches.size != 1) return "Destination has duplicate names: $path"
            if (size == null) return "Destination size could not be confirmed: $path"
            if (matches.single().optLong("Size", -1) != size) return "Destination size mismatch: $path"
        }
        return null
    }
}
