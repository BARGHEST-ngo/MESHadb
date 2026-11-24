// Copyright (c) BARGHEST
// SPDX-License-Identifier: AGPL-3.0-or-later

package com.barghest.mesh.ui.view.mesh

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.barghest.mesh.R
import com.barghest.mesh.ui.theme.MeshMono
import com.barghest.mesh.ui.theme.accent
import com.barghest.mesh.ui.theme.accentDim
import com.barghest.mesh.ui.theme.dim
import com.barghest.mesh.ui.theme.meshAmber
import com.barghest.mesh.ui.theme.meshAmberDim
import com.barghest.mesh.ui.theme.meshBg
import com.barghest.mesh.ui.theme.meshBorder
import com.barghest.mesh.ui.theme.meshCard
import com.barghest.mesh.ui.theme.meshGreen
import com.barghest.mesh.ui.theme.meshGreenDim
import com.barghest.mesh.ui.theme.meshCyan
import com.barghest.mesh.ui.theme.meshMonoText
import com.barghest.mesh.ui.theme.meshMuted
import com.barghest.mesh.ui.theme.meshText
import com.barghest.mesh.ui.theme.meshText2
import com.barghest.mesh.ui.util.AppVersion

@Composable
private fun HeroRings(active: Boolean) {
    val cs = MaterialTheme.colorScheme
    val c = if (active) cs.meshGreen else cs.meshMuted
    Box(Modifier.size(188.dp), contentAlignment = Alignment.Center) {
        if (active) {
            val t = rememberInfiniteTransition(label = "rings")
            repeat(3) { i ->
                val scale by t.animateFloat(
                    0.55f, 1.9f,
                    infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart, StartOffset(i * 900)),
                    label = "rs$i",
                )
                val fade by t.animateFloat(
                    0.9f, 0f,
                    infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart, StartOffset(i * 900)),
                    label = "rf$i",
                )
                Box(
                    Modifier
                        .size(96.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale; alpha = fade }
                        .clip(CircleShape)
                        .border(1.5.dp, c, CircleShape),
                )
            }
            val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(22000, easing = LinearEasing)), label = "rot")
            Canvas(Modifier.size(150.dp)) {
                rotate(rot) {
                    drawCircle(
                        color = c.copy(alpha = 0.4f),
                        radius = size.minDimension / 2f,
                        style = Stroke(1f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))),
                    )
                }
            }
        } else {
            Canvas(Modifier.size(150.dp)) {
                drawCircle(
                    color = cs.meshBorder,
                    radius = size.minDimension / 2f,
                    style = Stroke(1f.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f))),
                )
            }
        }
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(if (active) cs.meshGreen.dim(0.12f) else cs.meshCard)
                .border(1.5.dp, if (active) cs.meshGreen else cs.meshBorder, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MeshIcon(if (active) "lock" else "shield", size = 40.dp, color = c, stroke = 1.6f)
        }
    }
}

data class MeshHomeActions(
    val onStart: () -> Unit,
    val onEnd: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenLearn: () -> Unit,
    val onOpenAdb: () -> Unit,
)

@Composable
fun MeshHomeScreen(
    connected: Boolean,
    actions: MeshHomeActions,
    analyst: Analyst = MeshDefaults.analyst,
    adbReady: Boolean = false,
) {
    val cs = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(cs.meshBg)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Image(
                    painter = painterResource(R.drawable.mesh_wordmark),
                    contentDescription = "MESH",
                    colorFilter = ColorFilter.tint(cs.meshText),
                    modifier = Modifier.height(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("v${AppVersion.Short()}", color = cs.meshMuted, fontFamily = MeshMono, fontSize = 9.5.sp, modifier = Modifier.offset(y = 10.dp))
            }
            Box(Modifier.size(34.dp).clip(CircleShape).clickable(onClick = actions.onOpenSettings), contentAlignment = Alignment.Center) {
                MeshIcon("gear", size = 21.dp, color = cs.meshText2)
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HeroRings(connected)
            Spacer(Modifier.height(18.dp))
            Row(
                Modifier
                    .meshCard(
                        RoundedCornerShape(999.dp),
                        if (connected) cs.meshGreenDim else cs.meshCard,
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusDot(if (connected) cs.meshGreen else cs.meshMuted, size = 8.dp)
                Text(
                    if (connected) stringResource(R.string.mesh_status_connected_enc) else stringResource(R.string.mesh_status_not_connected),
                    color = if (connected) cs.meshGreen else cs.meshText2,
                    fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (connected) stringResource(R.string.mesh_home_conn_title) else stringResource(R.string.mesh_home_disc_title),
                color = cs.meshText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (connected) stringResource(R.string.mesh_home_conn_body) else stringResource(R.string.mesh_home_disc_body),
                color = cs.meshText2, fontSize = 13.5.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 300.dp),
            )
        }

        Column(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!connected) {
                MeshButton(stringResource(R.string.mesh_start_cta), actions.onStart, full = true, height = 56.dp, icon = "shield")
                AdbCard(adbReady, actions.onOpenAdb)
                MeshCard(onClick = actions.onOpenLearn) {
                    MeshRow(
                        icon = null,
                        title = stringResource(R.string.mesh_learn_card_title),
                        sub = stringResource(R.string.mesh_learn_card_sub),
                    )
                }
            } else {
                // Hidden until a real identity is available; MESHnet does not supply one yet.
                if (analyst.name.isNotBlank()) AnalystCard(analyst)
                SessionAdbCard(adbReady)
                MeshButton(stringResource(R.string.mesh_end_cta), actions.onEnd, full = true, height = 56.dp, icon = "power", variant = MeshButtonVariant.Danger)
            }
        }
    }
}

/** Analyst identity card: who is on the other end, and their mesh address. */
@Composable
private fun AnalystCard(a: Analyst) {
    val cs = MaterialTheme.colorScheme
    MeshCard {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Eyebrow(stringResource(R.string.mesh_analyst_label))
                Spacer(Modifier.height(4.dp))
                Text(
                    a.name.ifBlank { stringResource(R.string.mesh_analyst_placeholder) },
                    color = cs.meshText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            MonoText(a.ip, color = cs.meshText, fontSize = 14.sp)
        }
    }
}

/** Wireless-debugging (ADB) prerequisite card; the icon is amber while it still needs doing. */
@Composable
private fun AdbCard(ready: Boolean, onOpen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    MeshCard(onClick = onOpen, pad = 14.dp) {
        MeshRow(
            icon = if (ready) "check" else "wifi",
            title = if (ready) stringResource(R.string.mesh_adb_on_title) else stringResource(R.string.mesh_adb_off_title),
            sub = if (ready) stringResource(R.string.mesh_adb_on_sub) else stringResource(R.string.mesh_adb_off_sub),
            iconTint = if (ready) cs.meshGreen else cs.meshAmber,
            iconBg = null,
        )
    }
}

/** In-session wireless-debugging status, with a direct link to the system toggle. */
@Composable
private fun SessionAdbCard(ready: Boolean) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    MeshCard(pad = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MeshRow(
                icon = if (ready) "check" else "wifi",
                title = if (ready) stringResource(R.string.mesh_adb_on_title) else stringResource(R.string.mesh_adb_session_off_title),
                sub = if (ready) stringResource(R.string.mesh_adb_session_on_sub) else stringResource(R.string.mesh_adb_session_off_sub),
                iconTint = if (ready) cs.meshGreen else cs.meshAmber,
                iconBg = null,
                trailing = {},
            )
            Text(
                stringResource(R.string.mesh_adb_session_share_note),
                color = cs.meshText2, fontSize = 12.5.sp, lineHeight = 18.sp,
            )
            MeshButton(
                stringResource(R.string.mesh_adb_session_open),
                { openDeveloperSettings(context) },
                full = true,
                height = 46.dp,
                variant = MeshButtonVariant.Secondary,
            )
        }
    }
}
