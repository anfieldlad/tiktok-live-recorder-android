package com.stillhere.app.ui.ledger

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/**
 * `.field` — a mono uppercase label above a line, not a box.
 *
 * BasicTextField rather than OutlinedTextField: Material's field brings its own
 * container, radius and floating label, all three of which fight this design.
 */
@Composable
fun LedgerField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = LedgerType.label, color = Ledger.Dim)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ledger.Ink),
            cursorBrush = SolidColor(Ledger.SeriesInk),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = Ledger.Rule)
                }
                inner()
            },
        )
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(thickness = RuleWidth, color = Ledger.Rule)
    }
}
