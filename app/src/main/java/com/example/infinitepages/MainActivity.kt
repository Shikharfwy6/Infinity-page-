package com.example.infinitepages

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

// ---------- DATA ----------
data class PageData(
    val id: Long,
    val text: String = "",
    val color: Color = Color.White
)

// ---------- VIEWMODEL ----------
class PagesViewModel : ViewModel() {
    private var nextId = 1L
    val pages = mutableStateListOf(PageData(nextId++))

    fun addPage() {
        // Naya page hamesha neeche (same direction) add hota hai, pichle page ka color le leta hai
        val lastColor = pages.lastOrNull()?.color ?: Color.White
        pages.add(PageData(id = nextId++, color = lastColor))
    }

    fun updateText(id: Long, text: String) {
        val i = pages.indexOfFirst { it.id == id }
        if (i >= 0) pages[i] = pages[i].copy(text = text)
    }

    fun updateColor(id: Long, color: Color) {
        val i = pages.indexOfFirst { it.id == id }
        if (i >= 0) pages[i] = pages[i].copy(color = color)
    }

    fun deletePage(id: Long) {
        if (pages.size > 1) pages.removeAll { it.id == id }
    }
}

// ---------- ACTIVITY ----------
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme { PagesScreen() }
        }
    }
}

private val pageColors = listOf(
    Color.White, Color(0xFFFFF9C4), Color(0xFFFFE0B2), Color(0xFFFFCDD2),
    Color(0xFFE1BEE7), Color(0xFFBBDEFB), Color(0xFFC8E6C9), Color(0xFFD7CCC8),
    Color(0xFF263238), Color.Black
)

// ---------- MAIN SCREEN ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagesScreen(vm: PagesViewModel = viewModel()) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Pages (${vm.pages.size})") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    vm.addPage()
                    scope.launch { listState.animateScrollToItem(vm.pages.lastIndex) }
                },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add page") }
            )
        }
    ) { padding ->
        // LazyColumn = sirf screen par dikhne wale pages compose hote hain -> 1000+ pages par bhi lag nahi
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFB0B0B0))
                .padding(padding),
            contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(vm.pages, key = { _, p -> p.id }) { index, page ->
                PageItem(
                    index = index,
                    page = page,
                    onText = { vm.updateText(page.id, it) },
                    onColor = { vm.updateColor(page.id, it) },
                    onDelete = { vm.deletePage(page.id) }
                )
            }
        }
    }
}

// ---------- SINGLE A4 PAGE ----------
@Composable
fun PageItem(
    index: Int,
    page: PageData,
    onText: (String) -> Unit,
    onColor: (Color) -> Unit,
    onDelete: () -> Unit
) {
    var field by remember(page.id) { mutableStateOf(TextFieldValue(page.text)) }
    var showColors by remember { mutableStateOf(false) }
    val textColor = if (page.color.luminance() > 0.5f) Color.Black else Color.White

    Column {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Page ${index + 1}", color = Color.White, modifier = Modifier.weight(1f))
            IconButton(onClick = { showColors = true }) {
                Icon(Icons.Default.Palette, "Color", tint = Color.White)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.White)
            }
        }

        // A4 ratio = 210 : 297
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(210f / 297f)
                .shadow(6.dp, RoundedCornerShape(2.dp))
                .background(page.color)
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            BasicTextField(
                value = field,
                onValueChange = {
                    field = it
                    if (it.text != page.text) onText(it.text)
                },
                textStyle = TextStyle(color = textColor, fontSize = 16.sp, lineHeight = 24.sp),
                cursorBrush = SolidColor(textColor),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showColors) {
        AlertDialog(
            onDismissRequest = { showColors = false },
            confirmButton = { TextButton(onClick = { showColors = false }) { Text("OK") } },
            title = { Text("Page color") },
            text = {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pageColors.forEach { c ->
                        Box(
                            Modifier
                                .size(44.dp)
                                .background(c, CircleShape)
                                .border(
                                    BorderStroke(if (c == page.color) 3.dp else 1.dp, Color.Gray),
                                    CircleShape
                                )
                                .clickable { onColor(c) }
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    content: @Composable () -> Unit
) = androidx.compose.foundation.layout.FlowRow(
    horizontalArrangement = horizontalArrangement,
    verticalArrangement = verticalArrangement
) { content() }
