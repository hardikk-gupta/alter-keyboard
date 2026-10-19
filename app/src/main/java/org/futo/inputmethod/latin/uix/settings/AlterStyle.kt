package org.futo.inputmethod.latin.uix.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.altermind.voice.ui.AlterColors

// ALTER Keyboard: the app's own screens follow the ALTER app's settings design (DESIGN_SYSTEM.md in the ALTER repo,
// "App redesign, pass 1"): system sans type on a 12/14/16/20/26sp scale, grouped sections (a 12sp label above one
// borderless 20dp SlateGrayCard surface), rows with a 36dp tinted icon tile, 16sp titles and 12sp subtitles.
object AlterType {
    val PageHeading = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 34.sp)
    val RowTitle = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp)
    val Supporting = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val Meta = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp)
}

/** A settings section: an optional 12sp label above one borderless 20dp SlateGrayCard surface. */
@Composable
fun AlterSettingsGroup(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        if (title != null) {
            Text(title, style = AlterType.Meta, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(AlterColors.SlateGrayCard), content = content)
    }
}

/** ALTER's 36dp row icon tile: 6% foreground fill, 10dp corners, 20dp icon. */
@Composable
fun AlterIconTile(icon: Painter) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
    }
}
