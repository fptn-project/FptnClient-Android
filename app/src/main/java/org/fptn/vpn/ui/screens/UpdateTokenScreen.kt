package org.fptn.vpn.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.ui.components.BottomNavBar
import org.fptn.vpn.ui.components.BottomNavTab
import org.fptn.vpn.ui.components.HtmlText
import org.fptn.vpn.ui.theme.Hint
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.Secondary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.White10
import org.fptn.vpn.ui.theme.Yellow

@Composable
fun UpdateTokenScreen(
    errorText: String,
    htmlLabel: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    var tokenInput by remember { mutableStateOf(TextFieldValue("")) }

    Scaffold(
        containerColor = Primary,
        bottomBar = {
            BottomNavBar(
                current = BottomNavTab.SETTINGS,
                onHome = onHome,
                onSettings = onSettings,
                context = context,
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Primary)
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.icon_account_circle_100),
                contentDescription = null,
                modifier = Modifier
                    .padding(top = 32.dp)
                    .size(120.dp)
            )

            HtmlText(
                html = htmlLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                textColor = White,
                textSizeSp = 16f,
            )

            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                placeholder = {
                    Text(stringResource(R.string.paste_your_token), color = Hint, fontSize = 16.sp)
                },
                trailingIcon = {
                    if (tokenInput.text.isNotEmpty()) {
                        IconButton(onClick = { tokenInput = TextFieldValue("") }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_baseline_close_24),
                                contentDescription = null,
                                tint = White
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedContainerColor = White10,
                    unfocusedContainerColor = White10,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = White,
                ),
                shape = RoundedCornerShape(20.dp),
                singleLine = true,
            )

            if (errorText.isNotEmpty()) {
                Text(
                    text = errorText,
                    color = Yellow,
                    modifier = Modifier.padding(top = 8.dp, start = 5.dp, end = 20.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = White10,
                        contentColor = White,
                    ),
                    shape = RoundedCornerShape(100.dp),
                ) {
                    Text(stringResource(R.string.cancel_button))
                }
                Button(
                    onClick = { onSave(tokenInput.text) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Secondary,
                        contentColor = Primary,
                    ),
                    shape = RoundedCornerShape(100.dp),
                ) {
                    Text(stringResource(R.string.save_button))
                }
            }
        }
    }
}
