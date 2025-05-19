package dict.nick.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dict.nick.data.model.DefinitionEntry
import dict.nick.data.model.WordDetail
import dict.nick.navigation.AppDestinations

import dict.nick.ui.viewmodel.DictionaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordDetailScreen(
    word: String,
    navController: NavController,
    dictionaryViewModel: DictionaryViewModel
) {
    val wordDetailState by dictionaryViewModel.selectedWordDetail.collectAsState()
    val isLoading by dictionaryViewModel.isLoadingDetail.collectAsState()

    LaunchedEffect(word) {
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
                                text = detail.word.replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(detail.definitions) { definitionEntry ->
                            DefinitionItem(
                                definitionEntry = definitionEntry,
                                onWordClick = { clickedWord ->
                                    navController.navigate("${AppDestinations.WORD_DETAIL_SCREEN}/$clickedWord")
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                } ?: run {
                    if (!isLoading) { // Only show "not found" if not loading
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
            text = definitionEntry.partOfSpeech.replaceFirstChar { it.titlecase() },
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
            Text("Examples:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            examples.forEach { example ->
                Text(
                    text = "• $example",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        definitionEntry.synonyms?.takeIf { it.isNotEmpty() }?.let { synonyms ->
            Text("Synonyms:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            ClickableWordsRow(words = synonyms, onWordClick = onWordClick)

        }

        definitionEntry.antonyms?.takeIf { it.isNotEmpty() }?.let { antonyms ->
            Text("Antonyms:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            ClickableWordsRow(words = antonyms, onWordClick = onWordClick)
        }
        Divider(modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
fun ClickableWordsRow(words: List<String>, onWordClick: (String) -> Unit) {
    val annotatedString = buildAnnotatedString {
        words.forEachIndexed { index, word ->
            pushStringAnnotation(tag = "word_tag", annotation = word) // Use word as annotation
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Medium
                )
            ) {
                append(word)
            }
            pop()
            if (index < words.size - 1) {
                append(", ")
            }
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
        onClick = { offset ->
            annotatedString.getStringAnnotations(tag = "word_tag", start = offset, end = offset)
                .firstOrNull()?.let { annotation ->
                    onWordClick(annotation.item) // annotation.item is the word
                }
        }
    )
}
