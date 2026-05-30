package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    HisabMainScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabMainScreen(
    modifier: Modifier = Modifier,
    model: HisabViewModel = viewModel()
) {
    val results by model.calculationResult.collectAsStateWithLifecycle()
    val error by model.errorMessage.collectAsStateWithLifecycle()
    val savedLocs by model.savedLocations.collectAsStateWithLifecycle()

    val yearH by model.inputYear.collectAsStateWithLifecycle()
    val monthH by model.inputMonth.collectAsStateWithLifecycle()

    val latStr by model.inputLatitudeStr.collectAsStateWithLifecycle()
    val lonStr by model.inputLongitudeStr.collectAsStateWithLifecycle()
    val altStr by model.inputAltitudeStr.collectAsStateWithLifecycle()
    val tzStr by model.inputTimeZoneStr.collectAsStateWithLifecycle()
    val selectedLocName by model.selectedLocationName.collectAsStateWithLifecycle()

    var showAddLocDialog by remember { mutableStateOf(false) }
    var locationNameInput by remember { mutableStateOf("") }

    // Navigation tab index: 0 = Dasbor, 1 = Parameter, 2 = Rumus
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            CustomBottomBar(
                currentTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header block rendered globally at top of screen for visual branding context
                HeaderBlock(
                    tzStr = tzStr,
                    altStr = altStr,
                    locationName = selectedLocName
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Optional calculation error banner
                error?.let { err ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Peringatan",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Dynamic display of active Tab view
                when (selectedTab) {
                    0 -> {
                        // TAB 0: DASBOR / CORE COGNITIVE DASHBOARD
                        results?.let { result ->
                            // 1. Primary Highlight Result Card
                            PrimaryResultCard(result = result)

                            // 2. Crescent visual display & MABIMS verdict badge stacked safely
                            CrescentAndVerdictSection(result = result)

                            // 3. Technical Metrics 2x3 Grid
                            GridDataSection(result = result)
                        } ?: run {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    1 -> {
                        // TAB 1: PARAMETER / MANUAL ADJUSTMENTS & POS LISTS
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // Location selecting Chips bar
                            PosQuickBarSection(
                                savedLocs = savedLocs,
                                selectedLocName = selectedLocName,
                                onLocSelected = { model.setLocationValues(it) },
                                onLocDeleted = { model.deleteLocation(it) },
                                onAddClick = {
                                    locationNameInput = ""
                                    showAddLocDialog = true
                                }
                            )

                            // Core input parameter configuration form card
                            InputParameterFormCard(
                                monthH = monthH,
                                yearH = yearH,
                                latStr = latStr,
                                lonStr = lonStr,
                                altStr = altStr,
                                tzStr = tzStr,
                                onMonthChange = {
                                    model.inputMonth.value = it
                                    model.performCalculation()
                                },
                                onYearChange = {
                                    model.inputYear.value = it
                                    model.performCalculation()
                                },
                                onLatChange = {
                                    model.inputLatitudeStr.value = it
                                    model.performCalculation()
                                },
                                onLonChange = {
                                    model.inputLongitudeStr.value = it
                                    model.performCalculation()
                                },
                                onAltChange = {
                                    model.inputAltitudeStr.value = it
                                    model.performCalculation()
                                },
                                onTzChange = {
                                    model.inputTimeZoneStr.value = it
                                    model.performCalculation()
                                }
                            )

                            // Highlighted Action CTA
                            Button(
                                onClick = {
                                    locationNameInput = ""
                                    showAddLocDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .height(56.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "SIMPAN JADI POS OBSERVASI BARU",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // TAB 2: RUMUS & PESANTREN REFERENCE
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            // Noble Hadith card
                            HadithQuoteCard()

                            // Scholastic Pesantren description metadata
                            PesantrenMetadataCard()

                            // Math matrices
                            results?.let { result ->
                                TransparencyFormulaCard(result = result)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
                
                // Fine signature footer
                Text(
                    text = "Padepokan Pasantren An-Nirwana Falak • Versi Kontemporer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }
        }
    }

    // Modal dialog to store custom observer point coordinates
    if (showAddLocDialog) {
        Dialog(onDismissRequest = { showAddLocDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Text(
                        text = "Tambah Pos Observasi Baru",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = locationNameInput,
                        onValueChange = { locationNameInput = it },
                        label = { Text("Nama Lokasi / Pos") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Akan merekam parameter koordinat saat ini:\n• Lintang: $latStr°\n• Bujur: $lonStr°\n• Ketinggian: $altStr mdpl\n• Zona Waktu: UT+$tzStr",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showAddLocDialog = false }) {
                            Text("Batal")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (locationNameInput.isNotBlank()) {
                                    model.saveCurrentLocation(locationNameInput)
                                    showAddLocDialog = false
                                }
                            }
                        ) {
                            Text("Simpan")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderBlock(
    tzStr: String,
    altStr: String,
    locationName: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRubElHizb(
                    center = Offset(size.width - 50.dp.toPx(), 45.dp.toPx()),
                    size = 70.dp.toPx(),
                    color = Color(0xFFD4AF37).copy(alpha = 0.15f)
                )
                drawRubElHizb(
                    center = Offset(30.dp.toPx(), size.height - 25.dp.toPx()),
                    size = 40.dp.toPx(),
                    color = Color(0xFFD4AF37).copy(alpha = 0.08f)
                )
            }
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF064E3B), // Emerald-900
                        Color(0xFF022C22)  // Emerald-950
                    )
                )
            )
            .padding(vertical = 18.dp, horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hisab Falak Tadqiqi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "AN-NIRWANA CIPARAHU",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF34D399), // Emerald 400
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🔭", fontSize = 18.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable row of real-time parameter badges formatted exactly like HTML proposal
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                BadgeItem(label = "Mode fx-991ES")
                BadgeItem(label = "UT +$tzStr")
                BadgeItem(label = "${altStr} mdpl")
                BadgeItem(label = locationName)
            }
        }
    }
}

@Composable
fun BadgeItem(label: String) {
    Box(
        modifier = Modifier
            .background(
                Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(100.dp)
            )
            .border(
                BorderStroke(0.5.dp, Color.White.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(100.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFE2E8F0),
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun CustomBottomBar(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabs = listOf(
                TabItem("Dashboard", "🔭", 0),
                TabItem("Parameter", "📐", 1),
                TabItem("Rumus", "📜", 2)
            )

            tabs.forEach { tab ->
                val isActive = currentTab == tab.index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTabSelected(tab.index) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("tab_${tab.index}")
                ) {
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(MaterialTheme.colorScheme.secondary)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    } else {
                        Spacer(modifier = Modifier.height(7.dp))
                    }

                    Text(
                        text = tab.icon,
                        fontSize = 18.sp,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

data class TabItem(val label: String, val icon: String, val index: Int)

@Composable
fun PrimaryResultCard(result: HisabResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IJTIMA' AKHIR BULAN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "JD ${String.format("%.5f", result.ijtimaJD)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${result.yearHijri} H — ${result.monthNameHijri}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${result.conjunctionLocalDayName} ${result.conjunctionLocalPasaran}, ${result.conjunctionLocalDateStr}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Waktu Ijtima' (Lokal)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = result.conjunctionLocalTimeStr,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Ghurub Syamsi (Sunset)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = result.ghurubTimeStr,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CrescentAndVerdictSection(result: HisabResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Crescent canvas visualization
            CrescentMoonDisplay(result = result)

            // Meets criteria status banner
            MabimsVerdictCard(result = result)
        }
    }
}

@Composable
fun GridDataSection(result: HisabResult) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        val ageMin = ((result.moonAgeHours - result.moonAgeHours.toInt()) * 60).toInt()
        val durationSec = ((result.muktsulHilalMinutes - result.muktsulHilalMinutes.toInt()) * 60).toInt()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GridCard(
                label = "Irtifa’ Hilal Mar’i",
                value = HisabCalculator.formatDMS(result.topocentricMoonHeight_hc_prime),
                caption = "Toposentrik (${String.format("%.4f°", result.topocentricMoonHeight_hc_prime)})",
                modifier = Modifier.weight(1f)
            )
            GridCard(
                label = "Azimut Hilal",
                value = HisabCalculator.formatDMS(result.moonAzimuth),
                caption = "Titik Utara (${String.format("%.2f°", result.moonAzimuth)})",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GridCard(
                label = "Fraksi Illuminasi",
                value = String.format("%.4f%%", result.illuminatedFraction_Fl * 100.0),
                caption = "Nurul Hilal",
                modifier = Modifier.weight(1f)
            )
            GridCard(
                label = "Umur Hilal",
                value = String.format("%dj %dm", result.moonAgeHours.toInt(), ageMin),
                caption = "Sejak Conjunction",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GridCard(
                label = "Elongasi Toposentrik",
                value = HisabCalculator.formatDMS(result.elongationTopocentric_EloTopo),
                caption = "Elo Topo (${String.format("%.4f°", result.elongationTopocentric_EloTopo)})",
                modifier = Modifier.weight(1f)
            )
            GridCard(
                label = "Muktsul Hilal",
                value = String.format("%dm %ds", result.muktsulHilalMinutes.toInt(), durationSec),
                caption = "Durasi Di Atas Ufuk",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GridCard(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
fun PosQuickBarSection(
    savedLocs: List<FalakLocation>,
    selectedLocName: String,
    onLocSelected: (FalakLocation) -> Unit,
    onLocDeleted: (FalakLocation) -> Unit,
    onAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = "Pilih Pos Observasi:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(savedLocs) { loc ->
                val isActive = selectedLocName == loc.name
                InputChip(
                    selected = isActive,
                    onClick = { onLocSelected(loc) },
                    label = { Text(loc.name, maxLines = 1) },
                    trailingIcon = if (!loc.isDefault) {
                        {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onLocDeleted(loc) },
                                tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                            )
                        }
                    } else null,
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            item {
                AssistChip(
                    onClick = onAddClick,
                    label = { Text("Tambah Baru...") },
                    leadingIcon = { Icon(Icons.Default.Add, "Tambah", modifier = Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
fun InputParameterFormCard(
    monthH: Int,
    yearH: Int,
    latStr: String,
    lonStr: String,
    altStr: String,
    tzStr: String,
    onMonthChange: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    onLatChange: (String) -> Unit,
    onLonChange: (String) -> Unit,
    onAltChange: (String) -> Unit,
    onTzChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Pengaturan Input Parameter",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Month Selector Dropdown
                var dropdownExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1.3f)) {
                    OutlinedTextField(
                        value = "${monthH}. ${HisabCalculator.hijriMonths[monthH - 1]}",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Bulan Hijriah") },
                        trailingIcon = {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(Icons.Default.ArrowDropDown, "Pilih")
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        HisabCalculator.hijriMonths.forEachIndexed { index, mName ->
                            DropdownMenuItem(
                                text = { Text("${index + 1}. $mName") },
                                onClick = {
                                    onMonthChange(index + 1)
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Year input textfield
                OutlinedTextField(
                    value = yearH.toString(),
                    onValueChange = {
                        val yVal = it.toIntOrNull() ?: 1447
                        onYearChange(yVal)
                    },
                    label = { Text("Tahun H") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.7f)
                        .testTag("input_year")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = latStr,
                        onValueChange = onLatChange,
                        label = { Text("Lintang LS (deg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_lat")
                    )
                    val latD = latStr.toDoubleOrNull() ?: 0.0
                    Text(
                        text = "DMS: ${HisabCalculator.formatDMS(latD)} ${if (latD < 0) "LS" else "LU"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = lonStr,
                        onValueChange = onLonChange,
                        label = { Text("Bujur BT (deg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_lon")
                    )
                    val lonD = lonStr.toDoubleOrNull() ?: 0.0
                    Text(
                        text = "DMS: ${HisabCalculator.formatDMS(lonD)} ${if (lonD < 0) "BB" else "BT"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = altStr,
                    onValueChange = onAltChange,
                    label = { Text("Ketinggian mdpl") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_alt")
                )

                OutlinedTextField(
                    value = tzStr,
                    onValueChange = onTzChange,
                    label = { Text("Zona Waktu UT+") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_tz")
                )
            }
        }
    }
}

@Composable
fun HadithQuoteCard() {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "صُومُوا لِرُؤْيَتِهِ وَأَفْطِرُوا لِرُؤْيَتِهِ، فَإِنْ غُمَّ عَلَيْكُمْ فَأَكْمِلُوا عِدَّةَ شَعبانَ ثلاثينَ",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "\"Berpuasalah kamu karena melihat hilal dan berhari rayalah kamu karena melihatnya. Jika hilal tertutup awan, genapkanlah Sya'ban menjadi 30 hari.\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PesantrenMetadataCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "PADEPOKAN PASANTREN AN-NIRWANA",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Ahli Hisab Kontemporer Wal-Falaqiyah",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Alamat Lengkap:\nCikadongdong, Cikalil Rt/Rw 002/006 Ciparahu, Cihara, Lebak – Banten 42392\n\nMetode Hisab Kontemporer Tadqiqi disandarkan pada perhitungan astronomi akurat tinggi selaras kriteria MABIMS terbaru (Irtifa' 3° dan Elongasi 6.4°).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun TransparencyFormulaCard(result: HisabResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Deklarasi Transparansi Rumus (fx-991ES)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Text(
                text = "Konstanta & Variabel Conjunction (K, T, MT):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            FormulaRow(name = "HY", value = String.format("%.6f", result.HY))
            FormulaRow(name = "K", value = String.format("%.1f", result.K))
            FormulaRow(name = "T", value = String.format("%.6f", result.T_conjunction))
            FormulaRow(name = "JD Base", value = String.format("%.6f", result.JD_conjunction_base))
            FormulaRow(name = "MT Corrections", value = String.format("%.6f", result.MT_sum))
            FormulaRow(name = "Ijtima' (JD)", value = String.format("%.6f", result.ijtimaJD))

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Matriks Data Matahari Pasca Ghurub:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            FormulaRow(name = "T (Sunset)", value = String.format("%.8f", result.T_sunset))
            FormulaRow(name = "S", value = String.format("%.5f°", result.sunS))
            FormulaRow(name = "m (Anomaly)", value = String.format("%.5f°", result.sun_m))
            FormulaRow(name = "N", value = String.format("%.5f°", result.sunN))
            FormulaRow(name = "Q' (Obliquity)", value = String.format("%.6f°", result.sunQp))
            FormulaRow(name = "E (Eq of Time)", value = String.format("%.6f Jam", result.equationOfTime))
            FormulaRow(name = "S'", value = String.format("%.5f°", result.sunSp))
            FormulaRow(name = "δ (Declination)", value = String.format("%.5f°", result.sunDelta))
            FormulaRow(name = "PT (Right Asc)", value = String.format("%.5f°", result.sunPT_adj))
            FormulaRow(name = "Hour Angle T", value = String.format("%.5f°", result.sunHourAngleT))

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "Matriks Data Bulan:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            FormulaRow(name = "M (Moon)", value = String.format("%.5f°", result.moonM))
            FormulaRow(name = "A' (Anomaly)", value = String.format("%.5f°", result.moonA_prime))
            FormulaRow(name = "F (Lat Arg)", value = String.format("%.5f°", result.moonF))
            FormulaRow(name = "D (Elongation)", value = String.format("%.5f°", result.moonD))
            FormulaRow(name = "C sum", value = String.format("%.5f°", result.moonC))
            FormulaRow(name = "Mo (Long)", value = String.format("%.5f°", result.moonMo))
            FormulaRow(name = "Tc (Moon Hour Angle)", value = String.format("%.5f°", result.moonHourAngleTc))
            FormulaRow(name = "P (Distance)", value = String.format("%.1f km", result.moonDistance_P))
            FormulaRow(name = "PTc' (Moon PT)", value = String.format("%.5f°", result.moonPTc_prime))
            FormulaRow(name = "hc (Geocentric)", value = String.format("%.5f°", result.geocentricMoonHeight_hc))
            FormulaRow(name = "hc' (Topocentric)", value = String.format("%.5f°", result.topocentricMoonHeight_hc_prime))
        }
    }
}

// Custom Draw Rub El Hizb shape helper decoration
fun DrawScope.drawRubElHizb(center: Offset, size: Float, color: Color) {
    val half = size / 2
    drawRect(
        color = color,
        topLeft = Offset(center.x - half, center.y - half),
        size = Size(size, size),
        style = Stroke(width = 1.5.dp.toPx())
    )
    val path = Path().apply {
        moveTo(center.x, center.y - half * 1.414f)
        lineTo(center.x + half * 1.414f, center.y)
        lineTo(center.x, center.y + half * 1.414f)
        lineTo(center.x - half * 1.414f, center.y)
        close()
    }
    drawPath(path, color = color, style = Stroke(width = 1.5.dp.toPx()))
    drawCircle(color = color, radius = size * 0.18f, style = Stroke(width = 1.dp.toPx()))
}

// Custom Canvas Crescent Display
@Composable
fun CrescentMoonDisplay(
    modifier: Modifier = Modifier,
    result: HisabResult
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(140.dp)
            .clip(RoundedCornerShape(70.dp))
            .background(Brush.radialGradient(listOf(Color(0xFF132D20), Color(0xFF07140D))))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val starCoords = listOf(
                Offset(30.dp.toPx(), 40.dp.toPx()),
                Offset(110.dp.toPx(), 48.dp.toPx()),
                Offset(35.dp.toPx(), 105.dp.toPx()),
                Offset(115.dp.toPx(), 95.dp.toPx()),
                Offset(70.dp.toPx(), 22.dp.toPx()),
                Offset(75.dp.toPx(), 125.dp.toPx())
            )
            starCoords.forEach { pt ->
                drawCircle(color = Color(0x60FFE082), radius = 1.5.dp.toPx(), center = pt)
            }

            val centerPt = Offset(size.width / 2, size.height / 2)
            val outerRadius = 42.dp.toPx()

            drawCircle(
                color = Color(0xFF1D342B),
                radius = outerRadius,
                center = centerPt
            )

            val isLeftToRight = result.diffAzimuth_z > 0
            val illuminationRatio = result.illuminatedFraction_Fl.coerceIn(0.0, 1.0)
            val shadowOffset = outerRadius * (1.8f - (illuminationRatio.toFloat() * 1.5f))

            val moonColor = Color(0xFFFDD835) // Elegant Lunar Gold

            drawCircle(
                color = moonColor.copy(alpha = 0.12f),
                radius = outerRadius + 8.dp.toPx(),
                center = centerPt
            )

            drawCircle(color = moonColor, radius = outerRadius, center = centerPt)

            val shadowCenter = if (isLeftToRight) {
                Offset(centerPt.x - shadowOffset, centerPt.y)
            } else {
                Offset(centerPt.x + shadowOffset, centerPt.y)
            }
            drawCircle(
                color = Color(0xFF07140D),
                radius = outerRadius,
                center = shadowCenter
            )

            val lineY = size.height - 18.dp.toPx()
            drawLine(
                color = Color(0xFF3E2723), // horizon
                start = Offset(15.dp.toPx(), lineY),
                end = Offset(size.width - 15.dp.toPx(), lineY),
                strokeWidth = 2f
            )
        }
    }
}

@Composable
fun MabimsVerdictCard(result: HisabResult) {
    val meetAltitude = result.mavirAltitudeMeet
    val meetElongation = result.elongationMeet
    val conforms = result.isImkanuRukyat

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (conforms) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
        ),
        border = BorderStroke(
            1.dp,
            if (conforms) Color(0xFFA7F3D0) else Color(0xFFFCA5A5)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (conforms) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (conforms) Color(0xFF047857) else Color(0xFFB91C1C),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (conforms) "MEMENUHI KRITERIA MABIMS" else "BELUM MEMENUHI KRITERIA MABIMS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (conforms) Color(0xFF047857) else Color(0xFFB91C1C),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MetricValidationRow(
                    label = "Irtifa' Hilal (>= 3.0°)",
                    value = String.format("%.4f°", result.topocentricMoonHeight_hc_prime),
                    isValid = meetAltitude
                )
                MetricValidationRow(
                    label = "Elongasi Topo (>= 6.4°)",
                    value = String.format("%.4f°", result.elongationTopocentric_EloTopo),
                    isValid = meetElongation
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (conforms) {
                    "Kesimpulan: Awal bulan baru dimulai esok hari berdasarkan keputusan hisab kontemporer."
                } else {
                    "Kesimpulan: Sempurnakan bulan berjalan menjadi 30 hari (Istikmal/Awal bulan lusa)."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 4.dp)
            )
        }
    }
}

@Composable
fun MetricValidationRow(
    label: String,
    value: String,
    isValid: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Icon(
                imageVector = if (isValid) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = if (isValid) Color(0xFF047857) else Color(0xFFB91C1C),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isValid) Color(0xFF047857) else Color(0xFFB91C1C)
            )
        }
    }
}

@Composable
fun FormulaRow(name: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
