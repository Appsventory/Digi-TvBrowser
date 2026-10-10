package com.appsventory.digiapps.scour.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class TrackInfo(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

@Composable
fun TrackSelectionDialog(
    title: String,
    tracks: List<TrackInfo>,
    onSelect: (TrackInfo) -> Unit,
    onDismiss: () -> Unit,
    showOffOption: Boolean = false,
    onSelectOff: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E1E1E),
            tonalElevation = 8.dp,
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(0.9f)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    if (showOffOption && onSelectOff != null) {
                        item {
                            TrackItem(
                                label = "Off",
                                selected = tracks.none { it.isSelected },
                                onClick = {
                                    onSelectOff()
                                    onDismiss()
                                }
                            )
                        }
                    }
                    items(tracks) { track ->
                        TrackItem(
                            label = track.label,
                            selected = track.isSelected,
                            onClick = {
                                onSelect(track)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
private fun TrackItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFF3D5AFE) else Color(0xFF2A2A2A),
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
    ) {
        Text(
            text = label,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}
