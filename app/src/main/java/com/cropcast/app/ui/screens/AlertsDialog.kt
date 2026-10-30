package com.cropcast.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cropcast.app.data.model.AlertEvent
import com.cropcast.app.ui.localization.tr
import java.text.DateFormat
import java.util.Date

/** The bell: newest alerts first, unread ones in bold with a Mark read button. */
@Composable
fun AlertsDialog(alerts: List<AlertEvent>, onAcknowledge: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Notifications")) },
        text = {
            if (alerts.isEmpty()) Text(tr("No active alerts"), color = MaterialTheme.colorScheme.onSurfaceVariant)
            else LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(alerts.sortedByDescending { it.timestamp }, key = { it.id }) { alert ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                tr(alert.message),
                                fontWeight = if (alert.acknowledged) FontWeight.Normal else FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(alert.timestamp)),
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!alert.acknowledged) TextButton(onClick = { onAcknowledge(alert.id) }, modifier = Modifier.padding(start = 8.dp)) {
                            Text(tr("Mark read"))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Close")) } }
    )
}
