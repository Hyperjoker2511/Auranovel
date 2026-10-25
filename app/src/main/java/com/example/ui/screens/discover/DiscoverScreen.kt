package com.example.ui.screens.discover

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Novel
import com.example.data.model.ScraperExtension
import com.example.ui.theme.ArcaneGold
import com.example.ui.theme.ArcaneGreen
import com.example.ui.theme.ArcanePrimary
import com.example.ui.theme.ArcaneSecondary

@Composable
fun DiscoverScreen(
    featuredNovels: List<Novel>,
    installedExtensions: List<ScraperExtension>,
    searchResults: List<Novel>,
    isSearching: Boolean,
    onSearch: (extension: ScraperExtension?, query: String) -> Unit,
    onNovelClick: (Novel) -> Unit,
    onAddFavorite: (Novel) -> Unit,
    onOpenExtensions: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("All") }
    var selectedExtensionId by remember { mutableStateOf<String?>(null) }

    val genres = listOf("All", "Fantasy", "Action", "Cultivation", "LitRPG", "Romance", "Sci-Fi", "Steampunk", "Mystery")

    val filteredNovels = remember(featuredNovels, selectedGenre) {
        if (selectedGenre == "All") featuredNovels else {
            featuredNovels.filter { it.genres.contains(selectedGenre, ignoreCase = true) }
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Discover & Explore",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Browse trending light novels & multi-source web fiction",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalIconButton(
                        onClick = onOpenExtensions,
                        modifier = Modifier.testTag("discover_extensions_shortcut")
                    ) {
                        Icon(
                            Icons.Default.Extension,
                            contentDescription = "Manage Extensions",
                            tint = ArcanePrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Global Multi-Source Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { query ->
                        searchQuery = query
                        val targetExt = installedExtensions.firstOrNull { it.id == selectedExtensionId }
                        onSearch(targetExt, query)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discover_search_bar"),
                    placeholder = { Text("Search title, author, or keyword across sources...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = ArcanePrimary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                onSearch(null, "")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcanePrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                // Source selector filter row if extensions installed
                if (installedExtensions.isNotEmpty() && searchQuery.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedExtensionId == null,
                                onClick = {
                                    selectedExtensionId = null
                                    onSearch(null, searchQuery)
                                },
                                label = { Text("All Sources") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArcanePrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = ArcanePrimary
                                )
                            )
                        }
                        items(installedExtensions) { ext ->
                            FilterChip(
                                selected = selectedExtensionId == ext.id,
                                onClick = {
                                    selectedExtensionId = ext.id
                                    onSearch(ext, searchQuery)
                                },
                                label = { Text(ext.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArcanePrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = ArcanePrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (searchQuery.isNotBlank()) {
            // Search Results Mode
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ArcanePrimary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Searching community repositories...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.SearchOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No results found for '$searchQuery'",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try different keywords or check installed extension repositories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Found ${searchResults.size} novels",
                            style = MaterialTheme.typography.labelLarge,
                            color = ArcanePrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(searchResults) { novel ->
                        SearchResultNovelCard(novel = novel, onClick = { onNovelClick(novel) })
                    }
                }
            }
        } else {
            // Standard Discover Feed
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Hero Featured Carousel
                if (featuredNovels.isNotEmpty()) {
                    item {
                        FeaturedHeroSpotlight(
                            novel = featuredNovels.first(),
                            onReadClick = { onNovelClick(featuredNovels.first()) }
                        )
                    }
                }

                // 2. Genre Filter Pills
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Explore by Genre",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(genres) { genre ->
                                val isSelected = selectedGenre == genre
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) ArcanePrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .clickable { selectedGenre = genre }
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Trending Shelf (Horizontal Cards with Rank Badges)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = ArcaneGold,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Trending This Week",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Top Rated",
                                style = MaterialTheme.typography.labelMedium,
                                color = ArcanePrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            itemsIndexed(filteredNovels) { index, novel ->
                                TrendingNovelCard(
                                    rank = index + 1,
                                    novel = novel,
                                    onClick = { onNovelClick(novel) }
                                )
                            }
                        }
                    }
                }

                // 4. Popular Across Community Repositories
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoStories,
                                    contentDescription = null,
                                    tint = ArcaneSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Popular Web Novels",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            filteredNovels.take(5).forEach { novel ->
                                DiscoverNovelRowCard(
                                    novel = novel,
                                    onClick = { onNovelClick(novel) },
                                    onFavorite = { onAddFavorite(novel) }
                                )
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun FeaturedHeroSpotlight(
    novel: Novel,
    onReadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(220.dp)
            .testTag("featured_hero_spotlight")
            .clickable { onReadClick() },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = novel.coverUrl,
                contentDescription = novel.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Cyber Aura Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xCC0B0E14),
                                Color(0xFA0B0E14)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Spotlight badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = ArcanePrimary.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "FEATURED SENSATION",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${novel.author} • ${novel.genres}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = ArcaneGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${novel.userRating} rating",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${novel.totalChapters} Chapters",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Button(
                        onClick = onReadClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArcanePrimary),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Read Now", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingNovelCard(
    rank: Int,
    novel: Novel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(ArcanePrimary.copy(alpha = 0.3f), Color.Transparent)))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                AsyncImage(
                    model = novel.coverUrl,
                    contentDescription = novel.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Rank badge
                Surface(
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    color = if (rank == 1) ArcaneGold else if (rank <= 3) ArcanePrimary else Color(0xCC000000),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = novel.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DiscoverNovelRowCard(
    novel: Novel,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = novel.coverUrl,
                contentDescription = novel.title,
                modifier = Modifier
                    .size(width = 65.dp, height = 90.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = novel.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = novel.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = ArcaneGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${novel.userRating}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${novel.totalChapters} Ch.",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            IconButton(onClick = onFavorite) {
                Icon(
                    if (novel.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (novel.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SearchResultNovelCard(
    novel: Novel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = novel.coverUrl,
                contentDescription = novel.title,
                modifier = Modifier
                    .size(width = 55.dp, height = 75.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = novel.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = novel.author,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ArcanePrimary.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Source: ${novel.sourceId.removePrefix("ext_")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ArcanePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
