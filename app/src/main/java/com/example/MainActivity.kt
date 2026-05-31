package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.presentation.MainApp
import com.example.presentation.PeerLinkViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val appContainer = (application as PeerLinkApplication).appContainer
        val viewModel: PeerLinkViewModel = viewModel(
          factory = PeerLinkViewModel.provideFactory(appContainer)
        )
        MainApp(viewModel)
      }
    }
  }
}

