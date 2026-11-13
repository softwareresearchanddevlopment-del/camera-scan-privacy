package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.AdMobBanner
import com.example.ui.components.IosBottomNavigation
import com.example.ui.screens.*
import com.example.ui.theme.DocuFlowTheme
import com.example.ui.viewmodel.*
import com.google.android.gms.ads.MobileAds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK safely with emulator test device configuration
        try {
            val testDeviceIds = listOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR)
            val configuration = com.google.android.gms.ads.RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(configuration)
            MobileAds.initialize(this) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            val viewModel: DocuFlowViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            DocuFlowTheme(darkTheme = isDark) {
                DocuFlowApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DocuFlowApp(
    viewModel: DocuFlowViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val currentSubScreen by viewModel.currentSubScreen.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept back presses on sub-screens
    BackHandler(enabled = currentSubScreen != AppSubScreen.NONE) {
        viewModel.navigateBack()
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (currentSubScreen == AppSubScreen.NONE) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AdMobBanner(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (currentSubScreen == AppSubScreen.NONE) {
                IosBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { viewModel.selectTab(it) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = if (currentSubScreen == AppSubScreen.NONE) innerPadding.calculateTopPadding() else 0.dp
                )
        ) {
            when (currentSubScreen) {
                AppSubScreen.SCANNER -> ScannerScreen(viewModel = viewModel)
                AppSubScreen.PDF_MANAGER -> PdfManagerScreen(viewModel = viewModel)
                AppSubScreen.IMAGE_TO_PDF -> ImageToPdfScreen(viewModel = viewModel)
                AppSubScreen.IMAGE_TO_PPT -> ImageToPptScreen(viewModel = viewModel)
                AppSubScreen.CANVAS_EDITOR -> CanvasEditorScreen(viewModel = viewModel)
                AppSubScreen.PDF_READER -> PdfReaderScreen(viewModel = viewModel)
                AppSubScreen.PDF_EDITOR -> PdfEditorScreen(viewModel = viewModel)
                AppSubScreen.PDF_ORGANIZER -> PdfOrganizerScreen(viewModel = viewModel)
                AppSubScreen.PDF_MERGER -> PdfMergerScreen(viewModel = viewModel)
                AppSubScreen.PDF_SPLITTER -> PdfSplitterScreen(viewModel = viewModel)
                AppSubScreen.PDF_COMPRESSOR -> PdfCompressorScreen(viewModel = viewModel)
                AppSubScreen.PDF_CONVERTER -> PdfConverterScreen(viewModel = viewModel)
                AppSubScreen.OFFICE_VIEWER -> OfficeViewerScreen(viewModel = viewModel)
                AppSubScreen.SIGNATURE -> SignatureScreen(viewModel = viewModel)
                AppSubScreen.WATERMARK -> WatermarkScreen(viewModel = viewModel)
                AppSubScreen.CREATE_BLANK_PDF -> CreateBlankPdfScreen(viewModel = viewModel)
                AppSubScreen.NONE, AppSubScreen.TEMPLATES -> {
                    when (currentTab) {
                        AppTab.HOME -> HomeScreen(viewModel = viewModel)
                        AppTab.DOCUMENTS -> FilesScreen(viewModel = viewModel)
                        AppTab.CREATE -> CreateScreen(viewModel = viewModel)
                        AppTab.TOOLS -> ToolsScreen(viewModel = viewModel)
                        AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
