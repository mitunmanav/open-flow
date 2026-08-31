package app.openflow.ui.dictionary

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import app.openflow.text.PairImport
import app.openflow.ui.a11y.Dimen
import app.openflow.ui.components.ButtonVariant
import app.openflow.ui.components.OpenButton
import app.openflow.ui.components.OpenTextField
import app.openflow.ui.theme.SecUi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun PairImportBlock(
    testPrefix: String,
    onImport: suspend (String) -> PairImport.Outcome,
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var paste by remember { mutableStateOf("") }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = runCatching {
            ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull().orEmpty()
        if (text.isBlank()) {
            Toast.makeText(ctx, "Empty file", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val out = onImport(text)
            Toast.makeText(
                ctx,
                "Added ${out.added} · skip ${out.skipped} · conflict ${out.conflicts}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)) {
        Text(
            "CSV: heard,replace — one pair per line",
            style = MaterialTheme.typography.bodySmall,
            color = SecUi.muted,
            softWrap = true
        )
        OpenTextField(
            value = paste,
            onValueChange = { paste = it },
            label = "Paste from,to",
            placeholder = "wisper,Wispr",
            singleLine = false,
            minLines = 2,
            modifier = Modifier.testTag("${testPrefix}_import_paste")
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimen.GAP_SM)
        ) {
            OpenButton(
                text = "File",
                fill = false,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testPrefix}_import_file"),
                variant = ButtonVariant.Outlined,
                onClick = { pick.launch("text/*") }
            )
            OpenButton(
                text = "Paste",
                fill = false,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testPrefix}_import_paste_go"),
                enabled = paste.isNotBlank(),
                onClick = {
                    scope.launch {
                        val out = onImport(paste)
                        Toast.makeText(
                            ctx,
                            "Added ${out.added} · skip ${out.skipped} · conflict ${out.conflicts}",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (out.added > 0) paste = ""
                    }
                }
            )
        }
    }
}
