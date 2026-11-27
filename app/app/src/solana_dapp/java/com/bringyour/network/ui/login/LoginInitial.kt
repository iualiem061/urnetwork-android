package com.bringyour.network.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bringyour.network.ui.components.URInlineErrorText
import com.bringyour.network.ui.components.entrance
import com.bringyour.network.ui.components.tabletForm

/**
 * The initial sign-in screen: this build offers the instant account only.
 * The carousel and the single Create Instant Account action stay centered;
 * every other sign-in method (sso, wallets, auth code, seed phrase, email)
 * was removed with the custom build.
 */
@Composable
fun LoginInitial(
    navController: NavController,
    loginViewModel: LoginViewModel,
) {

    val onInstantAccountCreate: () -> Unit = {
        navController.navigate("create-network-instant")
    }

    Scaffold { innerPadding ->

        // mobile + tablet
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Column(
                modifier = Modifier
                    .tabletForm()
                    .imePadding()
            ) {
                Box(modifier = Modifier.entrance(0)) {
                    OnboardingCarousel()
                }

                Spacer(modifier = Modifier.height(64.dp))

                // the login stack rule (LoginStack.kt): the instant account is
                // the only sign-in this build offers
                Box(modifier = Modifier.entrance(1)) {
                    LoginStack(
                        full = listOf(
                            instantAccountLoginMethod(onClick = onInstantAccountCreate)
                        ),
                        tiles = emptyList(),
                        enabled = true
                    )
                }

                loginViewModel.loginError?.let { loginError ->
                    Spacer(modifier = Modifier.height(16.dp))
                    URInlineErrorText(loginError)
                }
            }

        }
    }
}
