@file:OptIn(ExperimentalMaterial3Api::class) // Opt-in for the entire file

package dict.nick.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // For preview instantiation
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController // For preview instantiation
import dict.nick.data.model.DefinitionEntry
import dict.nick.data.model.WordDetail
import dict.nick.navigation.AppDestinations
import dict.nick.ui.theme.DictAppTheme
import dict.nick.ui.viewmodel.DictionaryViewModel


@Composable
fun WordDetailScreen(
    word: String,
    navController: NavController,
    dictionaryViewModel: DictionaryViewModel
) {
    val wordDetailState by dictionaryViewModel.selectedWordDetail.collectAsState()
    val isLoading by dictionaryViewModel.isLoadingDetail.collectAsState()

    // Fetch detail when the word changes or screen is first composed with the word
    LaunchedEffect(key1 = word) { // Use key1 explicitly
        dictionaryViewModel.fetchWordDetail(word)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                wordDetailState?.let { detail ->
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = detail.word.replaceFirstChar { char ->
                                    if (char.isLowerCase()) char.titlecase() else char.toString()
                                },
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(detail.definitions) { definitionEntry ->
                            DefinitionItem(
                                definitionEntry = definitionEntry,
                                onWordClick = { clickedWord ->
                                    // To prevent re-navigating to the same word if already on its detail page
                                    if (clickedWord.lowercase() != word.lowercase()) {
                                        navController.navigate("${AppDestinations.WORD_DETAIL_SCREEN}/$clickedWord")
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                } ?: run {
                    // Only show "not found" if not loading and detail is null
                    if (!isLoading) {
                        Text(
                            "Word not found: $word",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DefinitionItem(definitionEntry: DefinitionEntry, onWordClick: (String) -> Unit) {
    Column {
        Text(
            text = definitionEntry.partOfSpeech.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase() else char.toString()
            },
            style = MaterialTheme.typography.titleMedium,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = definitionEntry.definition,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )

        definitionEntry.examples?.takeIf { it.isNotEmpty() }?.let { examples ->
            Text(
                "Examples:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            examples.forEach { example ->
                Text(
                    text = "• $example",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                )
            }
        }

        definitionEntry.synonyms?.takeIf { it.isNotEmpty() }?.let { synonyms ->
            Text(
                "Synonyms:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            ClickableWordsRow(words = synonyms, onWordClick = onWordClick)
        }

        definitionEntry.antonyms?.takeIf { it.isNotEmpty() }?.let { antonyms ->
            Text(
                "Antonyms:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            ClickableWordsRow(words = antonyms, onWordClick = onWordClick)
        }
        Divider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun ClickableWordsRow(words: List<String>, onWordClick: (String) -> Unit) {
    val annotatedString = buildAnnotatedString {
        words.forEachIndexed { index, word ->
            pushStringAnnotation(tag = "word_tag", annotation = word) // Use the word itself as the annotation
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(word)
            }
            pop() // Important to pop the annotation
            if (index < words.size - 1) {
                append(", ")
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp), // Adjust line height as needed
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "word_tag", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onWordClick(annotation.item) // annotation.item contains the word string
                }
        }
    )
}


@Preview(showBackground = true, name = "Word Detail Light (No Data)")
@Composable
fun WordDetailScreenLightPreview_NoData() {
    DictAppTheme(darkTheme = false) {
        val mockNavController = rememberNavController()
        val mockDictionaryViewModel: DictionaryViewModel = viewModel() // Uses default ViewModel
        WordDetailScreen(
            word = "example",
            navController = mockNavController,
            dictionaryViewModel = mockDictionaryViewModel
        )
    }
}

@Preview(showBackground = true, name = "Word Detail Light (With Data)")
@Composable
fun WordDetailScreenLightPreview_WithData() {
    DictAppTheme(darkTheme = false) {
        val mockNavController = rememberNavController()
        val mockDictionaryViewModel: DictionaryViewModel = viewModel()
        // Simulate data for preview using the new function
        mockDictionaryViewModel.setPreviewData(
            "example",
            WordDetail(
                word = "example",
                definitions = listOf(
                    DefinitionEntry(
                        partOfSpeech = "noun",
                        definition = "A thing characteristic of its kind or illustrating a general rule. This is a longer definition to see how it wraps and looks within the given space constraints and styling, ensuring good readability.",
                        examples = listOf(
                            "This is an example sentence for preview.",
                            "Another example to show list formatting."
                        ),
                        synonyms = listOf("sample", "model", "instance", "specimen", "illustration"),
                        antonyms = listOf("anomaly", "exception", "deviation")
                    ),
                    DefinitionEntry(
                        partOfSpeech = "verb",
                        definition = "To be a typical example of.",
                        examples = listOf("This exemplifies good coding practice."),
                        synonyms = listOf("exemplify", "typify", "illustrate"),
                        antonyms = emptyList()
                    )
                )
            )
        )
        WordDetailScreen(
            word = "example",
            navController = mockNavController,
            dictionaryViewModel = mockDictionaryViewModel
        )
    }
}