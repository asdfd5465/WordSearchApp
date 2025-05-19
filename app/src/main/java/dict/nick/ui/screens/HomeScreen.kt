@file:OptIn(ExperimentalMaterial3Api::class) // Opt-in for the entire file

package dict.nick.ui.screens

// NO 'import androidx.compose.material3.ExperimentalMaterial3Api' here

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
// ExperimentalMaterial3Api is implicitly available due to @file:OptIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import dict.nick.R
import dict.nick.navigation.AppDestinations
import dict.nick.ui.theme.DictAppTheme
import dict.nick.ui.viewmodel.DictionaryViewModel
import dict.nick.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

// No need for @OptIn on the function if @file:OptIn is used
@Composable
fun HomeScreen(
    navController: NavController,
    dictionaryViewModel: DictionaryViewModel,
    themeViewModel: ThemeViewModel
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    val predictions by dictionaryViewModel.wordPredictions.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState(initial = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dictionary App") },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch { themeViewModel.toggleTheme() }
                    }) {
                        Icon(
                            painter = painterResource(
                                id = if (isDarkTheme) R.drawable.ic_light_mode else R.drawable.ic_dark_mode
                            ),
                            contentDescription = "Toggle Theme"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    dictionaryViewModel.updateSearchQuery(it.text)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Type word here") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search Icon") },
                trailingIcon = {
                    if (searchQuery.text.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = TextFieldValue("")
                            dictionaryViewModel.clearPredictions()
                            dictionaryViewModel.updateSearchQuery("")
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear Search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (searchQuery.text.isNotBlank()) {
                        val searchText = searchQuery.text.trim()
                        keyboardController?.hide()
                        dictionaryViewModel.clearPredictions()
                        searchQuery = TextFieldValue("") 
                        dictionaryViewModel.updateSearchQuery("") 
                        navController.navigate("${AppDestinations.WORD_DETAIL_SCREEN}/$searchText")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = searchQuery.text.isNotBlank()
            ) {
                Text("Search")
            }

            if (predictions.isNotEmpty() && searchQuery.text.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Suggestions:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(predictions) { prediction ->
                        Text(
                            text = prediction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    keyboardController?.hide()
                                    searchQuery = TextFieldValue(prediction)
                                    dictionaryViewModel.clearPredictions()
                                    navController.navigate("${AppDestinations.WORD_DETAIL_SCREEN}/$prediction")
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Home Screen Light")
@Composable
fun HomeScreenLightPreview() {
    DictAppTheme(darkTheme = false) {
        val mockNavController = rememberNavController()
        val mockDictionaryViewModel: DictionaryViewModel = viewModel()
        val mockThemeViewModel: ThemeViewModel = viewModel()
        HomeScreen(
            navController = mockNavController,
            dictionaryViewModel = mockDictionaryViewModel,
            themeViewModel = mockThemeViewModel
        )
    }
}

@Preview(showBackground = true, name = "Home Screen Dark")
@Composable
fun HomeScreenDarkPreview() {
    DictAppTheme(darkTheme = true) {
        val mockNavController = rememberNavController()
        val mockDictionaryViewModel: DictionaryViewModel = viewModel()
        val mockThemeViewModel: ThemeViewModel = viewModel()
        HomeScreen(
            navController = mockNavController,
            dictionaryViewModel = mockDictionaryViewModel,
            themeViewModel = mockThemeViewModel
        )
    }
}