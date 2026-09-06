package com.xptlabs.varliktakibi.core.ext

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * Gün bazlı tarih matematiği. Anlık görüntüler ve grafik serileri "gün
 * başlangıcı epoch ms" ile anahtarlanıyor; hepsi tek yerden üretiliyor ki
 * cihaz saat dilimi değişse bile aynı gün aynı anahtara düşsün.
 */
object Days {

    val zone: ZoneId get() = ZoneId.systemDefault()

    fun today(): Long = of(LocalDate.now(zone))

    /** Verilen epoch ms'in yerel gün başlangıcı. */
    fun startOf(epochMillis: Long): Long =
        of(Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate())

    fun of(date: LocalDate): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toLocalDate(day: Long): LocalDate =
        Instant.ofEpochMilli(day).atZone(zone).toLocalDate()

    fun plus(day: Long, days: Long): Long = of(toLocalDate(day).plusDays(days))

    /** [from, to] aralığındaki gün başlangıçları (dahil). */
    fun range(from: Long, to: Long): List<Long> {
        if (from > to) return emptyList()
        val result = mutableListOf<Long>()
        var cursor = toLocalDate(from)
        val end = toLocalDate(to)
        while (!cursor.isAfter(end)) {
            result += of(cursor)
            cursor = cursor.plusDays(1)
        }
        return result
    }
}

/**
 * PostgREST timestamptz'i ayrıştırır. Kesirli saniyeli ("...T10:00:00.123456Z")
 * ve kesirsiz biçimlerin ikisi de gelir; ayrıca offset "+00:00" ya da "Z"
 * olabilir. Çözemezse null döner — bozuk tek satır tüm listeyi düşürmesin.
 */
fun String.parseTimestampOrNull(): Instant? =
    runCatching { OffsetDateTime.parse(this).toInstant() }
        .recoverCatching { Instant.parse(this) }
        .getOrElse { if (it is DateTimeParseException) null else throw it }
