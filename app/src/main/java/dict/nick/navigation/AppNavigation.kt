package dict.nick.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dict.nick.ui.screens.HomeScreen
import dict.nick.ui.screens.WordDetailScreen
import dict.nick.ui.viewmodel.DictionaryViewModel
import dict.nick.ui.viewmodel.ThemeViewModel

object AppDestinations {
    const val HOME_SCREEN = "home"
    const val WORD_DETAIL_SCREEN = "wordDetail"
}

@Composable
fun AppNavigation(themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    // Create ViewModelStoreOwner scoped to NavHost to share ViewModel
    val dictionaryViewModel: DictionaryViewModel = viewModel()

    NavHost(navController = navController, startDestination = AppDestinations.HOME_SCREEN) {
        composable(AppDestinations.HOME_SCREEN) {
            HomeScreen(
                navController = navController,
                dictionaryViewModel = dictionaryViewModel,
                themeViewModel = themeViewModel
            )
        }
        composable(
            route = "${AppDestinations.WORD_DETAIL_SCREEN}/{word}",
            arguments = listOf(navArgument("word") { type = NavType.StringType })
        ) { backStackEntry ->
            val word = backStackEntry.arguments?.getString("word") ?: ""
            WordDetailScreen(
                word = word,
                navController = navController,
                dictionaryViewModel = dictionaryViewModel
            )
        }
    }
}
