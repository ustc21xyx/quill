package com.quill.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Api
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quill.R
import com.quill.ui.chat.ChatScreen
import com.quill.ui.components.ConversationDrawer
import com.quill.ui.components.QuillBottomBar
import com.quill.ui.components.QuillTopBar
import com.quill.ui.models.ModelEditScreen
import com.quill.ui.personas.PersonaEditScreen
import com.quill.ui.providers.ProviderEditScreen
import com.quill.ui.providers.ProviderListScreen
import com.quill.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

object Routes {
    const val CHAT_NEW = "chat_new"
    const val CHAT = "chat/{conversationId}"
    const val PROVIDERS = "providers"
    const val PROVIDER_EDIT = "providers/edit/{providerId}"
    const val MODELS = "models"
    const val MODEL_EDIT = "models/edit/{modelId}"
    const val PERSONA_EDIT = "personas/edit/{personaId}"
    const val SETTINGS = "settings"

    fun chat(conversationId: String) = "chat/$conversationId"
    fun providerEdit(providerId: String) = "providers/edit/$providerId"
    fun modelEdit(modelId: String) = "models/edit/$modelId"
    fun modelEditWithProvider(providerId: String) = "models/edit/new?providerId=$providerId"
    fun personaEdit(personaId: String) = "personas/edit/$personaId"
}

enum class TopLevelRoute(
    val route: String,
    val labelRes: Int,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector,
) {
    Chat(Routes.CHAT_NEW, R.string.nav_chat, Icons.Outlined.ChatBubbleOutline, Icons.Filled.ChatBubble),
    Providers(Routes.PROVIDERS, R.string.nav_api, Icons.Outlined.Api, Icons.Filled.Api),
    Settings(Routes.SETTINGS, R.string.nav_settings, Icons.Outlined.Settings, Icons.Filled.Settings),
}

@Composable
fun QuillNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val topLevelRoutes = listOf(Routes.CHAT_NEW, Routes.PROVIDERS, Routes.SETTINGS)
    val isChatRoute = currentRoute == Routes.CHAT_NEW || currentRoute == Routes.CHAT
    val showBottomBar = currentRoute in topLevelRoutes || currentRoute == Routes.CHAT
    val showTopBar = currentRoute in listOf(Routes.PROVIDERS, Routes.SETTINGS)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isChatRoute,
        drawerContent = {
            ConversationDrawer(
                onConversationClick = { id ->
                    scope.launch { drawerState.close() }
                    navController.navigate(Routes.chat(id)) {
                        popUpTo(Routes.CHAT_NEW) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onNewChat = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Routes.CHAT_NEW) {
                        popUpTo(Routes.CHAT_NEW) { inclusive = true }
                    }
                },
                onDeleteConversation = { deletedId ->
                    val currentConversationId = navBackStackEntry?.arguments?.getString("conversationId")
                    if (currentConversationId == deletedId) {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.CHAT_NEW) {
                            popUpTo(Routes.CHAT_NEW) { inclusive = true }
                        }
                    }
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                if (showTopBar) {
                    QuillTopBar()
                }
            },
            bottomBar = {
                if (showBottomBar) {
                    QuillBottomBar(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Routes.CHAT_NEW,
                modifier = Modifier.padding(innerPadding),
                enterTransition = { fadeIn(tween(150)) },
                exitTransition = { fadeOut(tween(100)) },
                popEnterTransition = { fadeIn(tween(150)) },
                popExitTransition = { fadeOut(tween(100)) },
            ) {
                composable(Routes.CHAT_NEW) {
                    ChatScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                    )
                }
                composable(
                    route = Routes.CHAT,
                    arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
                ) {
                    ChatScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                    )
                }
                composable(Routes.PROVIDERS) {
                    ProviderListScreen(
                        onAddProvider = {
                            navController.navigate(Routes.providerEdit("new"))
                        },
                        onEditProvider = { id ->
                            navController.navigate(Routes.providerEdit(id))
                        },
                        onAddModel = { providerId ->
                            navController.navigate(Routes.modelEditWithProvider(providerId))
                        },
                        onEditModel = { modelId ->
                            navController.navigate(Routes.modelEdit(modelId))
                        },
                    )
                }
                composable(
                    route = Routes.PROVIDER_EDIT,
                    arguments = listOf(navArgument("providerId") { type = NavType.StringType }),
                ) {
                    ProviderEditScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = "${Routes.MODEL_EDIT}?providerId={providerId}",
                    arguments = listOf(
                        navArgument("modelId") { type = NavType.StringType },
                        navArgument("providerId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        },
                    ),
                ) {
                    ModelEditScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = Routes.PERSONA_EDIT,
                    arguments = listOf(navArgument("personaId") { type = NavType.StringType }),
                ) {
                    PersonaEditScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        onEditPersona = { id ->
                            navController.navigate(Routes.personaEdit(id))
                        },
                        onAddPersona = {
                            navController.navigate(Routes.personaEdit("new"))
                        },
                    )
                }
            }
        }
    }
}
