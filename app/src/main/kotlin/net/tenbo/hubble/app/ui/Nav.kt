package net.tenbo.hubble.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.tenbo.hubble.app.ui.theme.Ash
import net.tenbo.hubble.app.ui.theme.MonoLabel
import net.tenbo.hubble.app.ui.theme.Paper
import net.tenbo.hubble.app.ui.theme.Rule
import net.tenbo.hubble.app.ui.theme.Signal
import net.tenbo.hubble.app.ui.theme.clickableNoRipple

/** The four top-level destinations that carry the bottom bar. */
val TAB_SCREENS = setOf(Screen.DISCOVERY, Screen.NEARBY, Screen.MATCHES, Screen.PROFILE)

private data class Tab(val label: String, val screen: Screen)

private val TABS = listOf(
    Tab("Paths", Screen.DISCOVERY),
    Tab("Nearby", Screen.NEARBY),
    Tab("Matches", Screen.MATCHES),
    Tab("You", Screen.PROFILE),
)

/**
 * Editorial bottom bar: four mono labels, the active one in signal blue with a beacon
 * dot beneath it. A single hairline rule separates it from the content — no icons,
 * keeping with the type-forward system.
 */
@Composable
fun HubbleBottomBar(current: Screen, onSelect: (Screen) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Paper)) {
        Rule()
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TABS.forEach { tab ->
                val active = tab.screen == current
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickableNoRipple { onSelect(tab.screen) }.padding(horizontal = 8.dp),
                ) {
                    Text(
                        tab.label.uppercase(),
                        style = MonoLabel.copy(fontSize = 12.sp),
                        color = if (active) Signal else Ash,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        Modifier.size(4.dp).clip(RoundedCornerShape(50))
                            .background(if (active) Signal else Color.Transparent),
                    )
                }
            }
        }
    }
}
