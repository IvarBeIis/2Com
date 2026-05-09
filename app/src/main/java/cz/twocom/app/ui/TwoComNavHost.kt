package cz.twocom.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cz.twocom.feature.chat.ChatListScreen
import cz.twocom.feature.contacts.AddContactScreen
import cz.twocom.feature.onboarding.OnboardingScreen

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object ChatList : Screen("chat_list")
    object AddContact : Screen("add_contact")
    object Chat : Screen("chat/{peerId}") {
        fun createRoute(peerId: String) = "chat/$peerId"
    }
}

@Composable
fun TwoComNavHost() {
    val navController = rememberNavController()
    val vm: AppViewModel = hiltViewModel()
    val hasIdentity by vm.hasIdentity.collectAsState(initial = null)

    if (hasIdentity == null) return

    NavHost(
        navController = navController,
        startDestination = if (hasIdentity == true) Screen.ChatList.route else Screen.Onboarding.route,
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinished = {
                navController.navigate(Screen.ChatList.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onAddContact = { navController.navigate(Screen.AddContact.route) },
                onOpenChat = { peerId ->
                    navController.navigate(Screen.Chat.createRoute(peerId))
                },
            )
        }
        composable(Screen.AddContact.route) {
            AddContactScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Chat.route) { backStackEntry ->
            val peerId = backStackEntry.arguments?.getString("peerId") ?: return@composable
            cz.twocom.feature.chat.ChatScreen(
                peerId = peerId,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
