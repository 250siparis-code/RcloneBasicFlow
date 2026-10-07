package dev.galaxy.rclonecards.engine

/**
 * Küçük bir shell-word ayrıştırıcısı.
 *
 * Amaç gerçek bir shell çalıştırmak değil; kullanıcının Termux'ta yazdığı biçime yakın
 * `rclone ...` komutunu güvenli biçimde argv listesine çevirmektir. Tek/çift tırnakları,
 * ters eğik çizgi kaçışlarını ve `\\` + satır sonu devamını destekler.
 */
object ShellWords {
    fun parse(input: String): List<String> {
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escape = false
        var tokenStarted = false

        fun flush() {
            if (tokenStarted || current.isNotEmpty()) {
                out += current.toString()
                current.clear()
                tokenStarted = false
            }
        }

        var i = 0
        while (i < input.length) {
            val c = input[i]

            if (escape) {
                // Shell'deki "\\\n" gibi satır devamını tek komut olarak kabul et.
                if (c == '\n') {
                    escape = false
                    i++
                    continue
                }
                if (c == '\r' && i + 1 < input.length && input[i + 1] == '\n') {
                    escape = false
                    i += 2
                    continue
                }
                current.append(c)
                tokenStarted = true
                escape = false
                i++
                continue
            }

            when {
                c == '\\' && quote != '\'' -> {
                    escape = true
                    tokenStarted = true
                }
                quote != null && c == quote -> {
                    quote = null
                    tokenStarted = true
                }
                quote == null && (c == '\'' || c == '"') -> {
                    quote = c
                    tokenStarted = true
                }
                quote == null && c.isWhitespace() -> flush()
                else -> {
                    current.append(c)
                    tokenStarted = true
                }
            }
            i++
        }

        if (escape) current.append('\\')
        require(quote == null) { "Kapanmamış tırnak işareti var" }
        flush()
        return out
    }
}
