package net.tenbo.hubble.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.NewInstanceFactory
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import net.tenbo.hubble.app.data.KeystoreVault
import net.tenbo.hubble.core.handshake.HandshakeState

/**
 * Single-activity Compose host. Wires the encrypted DB + Keystore vault into the
 * ViewModel, requests BLE permissions, and renders the current [Screen]. The Nearby
 * screen drives live advertising/scanning via the ViewModel's ProximityCoordinator.
 */
class MainActivity : ComponentActivity() {

    private lateinit var vm: HubbleViewModel
    private val idleState = MutableStateFlow<HandshakeState>(HandshakeState.Idle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val vault = KeystoreVault(applicationContext)
        // SQLCipher-encrypted at rest, keyed by a Keystore-wrapped random DB key
        // (process-wide singleton, shared with the background message worker).
        val db = net.tenbo.hubble.app.data.HubbleDb.get(applicationContext, vault)

        // Local message notifications: channel + permission + periodic background poll.
        net.tenbo.hubble.app.notify.Notifications.ensureChannel(applicationContext)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            applicationContext.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
            ) {}.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
        if (vault.hasIdentity()) scheduleMessagePoll()

        vm = ViewModelProvider(
            this,
            object : NewInstanceFactory() {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HubbleViewModel(
                        applicationContext, vault, db.friendDao(), db.postDao(),
                        db.encounterDao(), db.matchDao(), db.messageDao(), db.blockDao(),
                    ) as T
            },
        )[HubbleViewModel::class.java]
        vm.ensureOrchestrator()
        vm.loadFriends()
        vm.loadPosts()

        setContent {
            net.tenbo.hubble.app.ui.theme.HubbleTheme {
                Surface {
                    val screen by vm.screen.collectAsState()
                    val handshake by (vm.handshakeState ?: idleState).collectAsState()
                  Column(Modifier.fillMaxSize()) {
                   Box(Modifier.weight(1f)) {
                    when (screen) {
                        Screen.ONBOARDING ->
                            OnboardingFlow(vm.pendingPhrase) { vm.completeOnboarding(it) }

                        Screen.DISCOVERY -> {
                            val encounters by vm.encounters.collectAsState()
                            val matched by vm.justMatched.collectAsState()
                            LaunchedEffect(Unit) { vm.loadEncounters(); vm.initP2p() }
                            androidx.compose.foundation.layout.Box {
                                DiscoveryScreen(
                                    encounters = encounters,
                                    nowMs = System.currentTimeMillis(),
                                    onPass = { vm.passEncounter(it) },
                                    onLike = { vm.likeEncounter(it) },
                                    onNearby = { vm.goTo(Screen.NEARBY) },
                                    onMatches = { vm.loadMatches(); vm.goTo(Screen.MATCHES) },
                                    onProfile = { vm.goTo(Screen.PROFILE) },
                                )
                                matched?.let { m ->
                                    MatchCelebration(
                                        match = m,
                                        onMessage = { vm.dismissMatch(); vm.openChat(m.hubbleId) },
                                        onKeepLooking = { vm.dismissMatch() },
                                    )
                                }
                            }
                        }

                        Screen.MAP -> {
                            val cells by vm.heatmap.collectAsState()
                            LaunchedEffect(Unit) { vm.loadHeatmap() }
                            val locationPermission = androidx.activity.compose.rememberLauncherForActivityResult(
                                androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                            ) { granted -> if (granted) vm.postLocationBeacon() }
                            MapScreen(
                                cells = cells,
                                onCheckIn = {
                                    val granted = applicationContext.checkSelfPermission(
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (granted) vm.postLocationBeacon()
                                    else locationPermission.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION)
                                },
                                onBack = { vm.goTo(Screen.NEARBY) },
                            )
                        }

                        Screen.MATCHES -> {
                            val matches by vm.matches.collectAsState()
                            LaunchedEffect(Unit) { vm.loadMatches() }
                            MatchesScreen(
                                matches = matches,
                                onOpen = { vm.openChat(it) },
                                onBack = { vm.goTo(Screen.DISCOVERY) },
                            )
                        }

                        Screen.CHAT -> {
                            val match by vm.currentMatch.collectAsState()
                            val messages by vm.messages.collectAsState()
                            val typingFrom by vm.typingFrom.collectAsState()
                            ChatScreen(
                                match = match,
                                messages = messages,
                                typing = typingFrom != null && typingFrom == match?.hubbleId,
                                onSend = { vm.sendMessage(it) },
                                onSendVoice = { file, dur -> vm.sendVoice(file, dur) },
                                onTyping = { vm.notifyTyping() },
                                onUnmatch = { match?.hubbleId?.let { vm.unmatch(it) } },
                                onBlock = { match?.hubbleId?.let { vm.block(it) } },
                                onReport = { match?.hubbleId?.let { vm.report(it) } },
                                onBack = { vm.loadMatches(); vm.goTo(Screen.MATCHES) },
                            )
                        }

                        Screen.PROFILE -> {
                            val ctx = LocalContext.current
                            val pairingStatus by vm.pairingStatus.collectAsState()
                            val pairLauncher = rememberLauncherForActivityResult(
                                com.journeyapps.barcodescanner.ScanContract(),
                            ) { result ->
                                result.contents?.let { vm.pairDesktop(it) }
                            }
                            LaunchedEffect(pairingStatus) {
                                pairingStatus?.let {
                                    Toast.makeText(ctx, it, Toast.LENGTH_LONG).show()
                                    vm.dismissPairingStatus()
                                }
                            }
                            ProfileScreen(
                                profile = vm.currentProfile(),
                                crossedPaths = 0,
                                isSelf = true,
                                onEdit = { vm.goTo(Screen.PROFILE_EDIT) },
                                onStories = { vm.goTo(Screen.FEED) },
                                onSyncDevices = { vm.syncToMyDevices() },
                                onPairDesktop = {
                                    val opts = com.journeyapps.barcodescanner.ScanOptions().apply {
                                        setBeepEnabled(false)
                                        setOrientationLocked(false)
                                        setPrompt("Scan the desktop's pairing QR")
                                        setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
                                    }
                                    pairLauncher.launch(opts)
                                },
                                onBack = { vm.goTo(Screen.DISCOVERY) },
                            )
                        }

                        Screen.PROFILE_EDIT ->
                            ProfileEditScreen(initial = vm.currentProfile()) { vm.saveProfile(it) }

                        Screen.FEED -> {
                            val posts by vm.posts.collectAsState()
                            LaunchedEffect(Unit) { vm.loadPosts(); vm.sync(); vm.initP2p() }
                            FeedScreen(
                                posts = posts,
                                onCompose = { vm.goTo(Screen.COMPOSE) },
                                onNearby = { vm.goTo(Screen.NEARBY) },
                                onFriends = { vm.loadFriends(); vm.goTo(Screen.FRIENDS) },
                                onProfile = { vm.goTo(Screen.PROFILE) },
                            )
                        }

                        Screen.COMPOSE ->
                            ComposeScreen(
                                onPublish = { vm.publishPost(it) },
                                onCancel = { vm.goTo(Screen.FEED) },
                            )

                        Screen.NEARBY ->
                            WithBlePermissions(
                                granted = {
                                    LaunchedEffect(Unit) { vm.startProximity() }
                                    val nearby by vm.nearby.collectAsState()
                                    NearbyScreen(
                                        nearby = nearby,
                                        onTap = { vm.addNearby(it) },
                                        onMap = { vm.loadHeatmap(); vm.goTo(Screen.MAP) },
                                        onBack = { vm.goTo(Screen.DISCOVERY) },
                                    )
                                },
                                denied = { retry ->
                                    Column(
                                        Modifier.fillMaxSize().padding(24.dp),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text("Hubble needs Bluetooth & nearby-device permission to find people around you.")
                                        Button(onClick = retry, modifier = Modifier.padding(top = 16.dp)) {
                                            Text("Grant permission")
                                        }
                                    }
                                },
                            )

                        Screen.SAS_CONFIRM ->
                            SasConfirmScreen(
                                state = handshake,
                                onConfirm = { vm.confirmSas() },
                                onCancel = { vm.goTo(Screen.NEARBY) },
                            )

                        Screen.FRIENDS -> {
                            val friends by vm.friends.collectAsState()
                            FriendsScreen(friends) { vm.goTo(Screen.FEED) }
                        }
                    }
                   }
                   if (screen in TAB_SCREENS) {
                       HubbleBottomBar(current = screen, onSelect = { vm.goTo(it) })
                   }
                  }
                }
            }
        }
    }

    /**
     * Background message delivery: a ~15-min periodic poll for when the app is closed, plus an
     * immediate one-shot poll on open so you catch up on anything missed without waiting.
     */
    private fun scheduleMessagePoll() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()
        val wm = androidx.work.WorkManager.getInstance(applicationContext)

        val periodic = androidx.work.PeriodicWorkRequestBuilder<net.tenbo.hubble.app.notify.MessageWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES,
        ).setConstraints(constraints).build()
        wm.enqueueUniquePeriodicWork(
            "hubble-message-poll",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            periodic,
        )

        val now = androidx.work.OneTimeWorkRequestBuilder<net.tenbo.hubble.app.notify.MessageWorker>()
            .setConstraints(constraints)
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        wm.enqueueUniqueWork(
            "hubble-message-poll-now",
            androidx.work.ExistingWorkPolicy.REPLACE,
            now,
        )
    }
}
