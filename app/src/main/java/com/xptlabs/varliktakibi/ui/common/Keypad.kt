package com.xptlabs.varliktakibi.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xptlabs.varliktakibi.ui.theme.AppColors

/**
 * Miktar/fiyat girişi için özel tuş takımı. Sistem klavyesi yerine kullanılıyor
 * çünkü iOS tasarımı böyle ve sayısal girişte "," ile "." karışıklığını
 * tamamen ortadan kaldırıyor.
 */
@Composable
fun Keypad(
    onDigit: (String) -> Unit,
    onComma: () -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(",", "0", BACKSPACE)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    Key(
                        label = key,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (key) {
                                BACKSPACE -> onBackspace()
                                "," -> onComma()
                                else -> onDigit(key)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Key(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.subtleFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (label == BACKSPACE) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Sil",
                tint = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = label,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private const val BACKSPACE = "⌫"

/**
 * Tuş takımından gelen girdiyi metne uygular. Ondalık hane sınırı türe göre
 * değişiyor: kriptoda 8 hane gerekirken tutarda 2 yetiyor.
 */
object KeypadInput {

    fun appendDigit(current: String, digit: String, maxDecimals: Int): String {
        val decimals = current.substringAfter(',', "")
        if (current.contains(',') && decimals.length >= maxDecimals) return current
        // Baştaki gereksiz sıfırı yut: "0" + "5" → "5", ama "0," + "5" → "0,5".
        if (current == "0") return digit
        return current + digit
    }

    fun appendComma(current: String): String = when {
        current.contains(',') -> current
        current.isEmpty() -> "0,"
        else -> "$current,"
    }

    fun backspace(current: String): String = current.dropLast(1)
}
