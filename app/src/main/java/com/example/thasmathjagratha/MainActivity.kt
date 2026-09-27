package com.example.thasmathjagratha

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.thasmathjagratha.navigation.AppNavigation
import com.example.thasmathjagratha.theme.ThasmathJagrathaTheme
import com.example.thasmathjagratha.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()
    private val bluetoothPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) mainViewModel.enableBluetoothAlertReceiver() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel.startLocalAlertReceiver(applicationContext)
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
        enableEdgeToEdge()
        setContent {
            ThasmathJagrathaTheme {
                AppNavigation(viewModel = mainViewModel)
            }
        }
    }
}
