package org.fptn.vpn.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fptn.vpn.R
import org.fptn.vpn.ui.components.HtmlText
import org.fptn.vpn.ui.theme.Hint
import org.fptn.vpn.ui.theme.Primary
import org.fptn.vpn.ui.theme.Secondary
import org.fptn.vpn.ui.theme.White
import org.fptn.vpn.ui.theme.White10
import org.fptn.vpn.ui.theme.Yellow

@Composable
fun LoginScreen(
    errorText: String,
    htmlLabel: String,
    onLogin: (String) -> Unit,
) {
    var tokenInput by remember { mutableStateOf(TextFieldValue("")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Primary)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.icon_account_circle_100),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 48.dp)
                .size(160.dp)
        )

        HtmlText(
            html = htmlLabel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
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
                Text(
                    text = stringResource(R.string.paste_your_token),
                    color = Hint,
                    fontSize = 16.sp
                )
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

        Button(
            onClick = { onLogin(tokenInput.text) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Secondary,
                contentColor = Primary,
            ),
            shape = RoundedCornerShape(100.dp),
        ) {
            Text(
                text = stringResource(R.string.login_button),
                fontSize = 18.sp
            )
        }

        if (errorText.isNotEmpty()) {
            Text(
                text = errorText,
                color = Yellow,
                modifier = Modifier.padding(top = 16.dp, start = 5.dp, end = 20.dp, bottom = 20.dp)
            )
        }
    }
}
