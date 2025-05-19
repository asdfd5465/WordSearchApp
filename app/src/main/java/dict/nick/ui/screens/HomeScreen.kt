package dict.nick.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import dict.nick.R
import dict.nick.navigation.AppDestinations
import dict.nick.ui.theme.DictAppTheme
import dict.nick.ui.viewmodel.DictionaryViewModel
import dict.nick.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val isDarkTheme by themeViewModel.isDarkTheme.collectAsState(initial = true) // Initial can be from system

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
                        keyboardController?.hide()
                        navController.navigate("${AppDestinations.WORD_DETAIL_SCREEN}/${searchQuery.text.trim()}")
                        dictionaryViewModel.clearPredictions() // Clear predictions after search
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
                                    searchQuery = TextFieldValue(prediction) // Update search box
                                    keyboardController?.hide()
                                    navController.navigate("${AppDestinations.WORD_DETAIL_SCREEN}/$prediction")
                                    dictionaryViewModel.clearPredictions()
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

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    DictAppTheme {
        // HomeScreen(rememberNavController(), viewModel(), viewModel()) // Simplified for preview
    }
}
