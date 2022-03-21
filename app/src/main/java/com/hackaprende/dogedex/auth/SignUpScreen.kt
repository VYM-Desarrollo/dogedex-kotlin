package com.hackaprende.dogedex.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hackaprende.dogedex.R
import com.hackaprende.dogedex.composables.AuthField
import com.hackaprende.dogedex.composables.BackNavigationIcon

@Composable
fun SignUpScreen() {
    Scaffold(
        topBar = { SignUpScreenToolbar() }
    ) {
        Content()
    }
}

@Composable
private fun Content() {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 32.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AuthField(
            label = stringResource(id = R.string.email),
            modifier = Modifier
                .fillMaxWidth(),
            email = email.value,
            onTextChanged = { email.value = it }
        )

        AuthField(
            label = stringResource(id = R.string.password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            email = password.value,
            onTextChanged = { password.value = it },
            visualTransformation = PasswordVisualTransformation()
        )

        AuthField(
            label = stringResource(id = R.string.confirm_password),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            email = confirmPassword.value,
            onTextChanged = { confirmPassword.value = it },
            visualTransformation = PasswordVisualTransformation()
        )

        Button(modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
            onClick = {  }) {
            Text(
                stringResource(R.string.sign_up),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SignUpScreenToolbar() {
    TopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        backgroundColor = Color.Red,
        contentColor = Color.White,
        navigationIcon = { BackNavigationIcon { } }
    )
}

