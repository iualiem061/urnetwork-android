package com.bringyour.network.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.bringyour.network.R
import com.bringyour.network.ui.components.ButtonStyle
import com.bringyour.network.ui.components.FoldSafeDialog
import com.bringyour.network.ui.components.URButton
import com.bringyour.network.ui.components.URInlineErrorText
import com.bringyour.network.ui.components.URSwitch
import com.bringyour.network.ui.components.URTextInput
import com.bringyour.network.ui.components.URTextInputLabel
import com.bringyour.network.ui.components.entrance
import com.bringyour.network.ui.components.pressScale
import com.bringyour.network.ui.components.tabletReadableColumn

import com.bringyour.network.ui.theme.Pink
import com.bringyour.network.ui.theme.TopBarTitleTextStyle
import com.bringyour.network.ui.theme.AppTheme
import kotlin.math.min

/**
 * Account > Settings > VLESS: the VLESS server of this network space
 * (VlessSettingsViewModel).
 */
@Composable
fun VlessSettingsScreen(
    navController: NavController,
    viewModel: VlessSettingsViewModel = hiltViewModel(),
) {
    VlessSettingsScaffold(
        onBack = { navController.popBackStack() },
        viewModel = viewModel,
    )
}

/**
 * The same editor over the login screen, opened from its network settings
 * ("Change Network API") for the active network space before sign-in. Its
 * view model outlives one opening, so each opening loads the space's settings
 * again.
 */
@Composable
fun VlessSettingsDialog(
    onDismiss: () -> Unit,
    viewModel: VlessSettingsViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    // a screen shown as a dialog: beside a fold, it fills one side
    FoldSafeDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        fullScreen = true,
    ) {
        VlessSettingsScaffold(
            onBack = onDismiss,
            viewModel = viewModel,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VlessSettingsScaffold(
    onBack: () -> Unit,
    viewModel: VlessSettingsViewModel,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(id = R.string.vless),
                        style = TopBarTitleTextStyle
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppTheme.bg
                ),
            )
        },
        containerColor = AppTheme.bg
    ) { innerPadding ->
        VlessSettingsForm(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * The editor: the switch, the share link, the server, the transport and the
 * security, then only the fields that apply to them (VlessForm), and Save.
 * Every field stays editable while VLESS is off, since off settings save as
 * typed.
 */
@Composable
private fun VlessSettingsForm(
    viewModel: VlessSettingsViewModel,
    modifier: Modifier = Modifier,
) {

    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val form = viewModel.form
    val editable = viewModel.editable

    var link by remember { mutableStateOf(TextFieldValue()) }

    Column(
        modifier = modifier
            .tabletReadableColumn()
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 16.dp)),
    ) {

        Text(
            stringResource(id = R.string.vless_settings_description),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.textMuted,
        )

        // the switch's 48 dp touch target fills the rest, above and below
        Spacer(modifier = Modifier.height(10.dp))

        VlessSwitchRow(
            text = stringResource(id = R.string.vless_enabled),
            checked = form.enabled,
            enabled = editable,
            toggle = { viewModel.update { it.copy(enabled = !it.enabled) } },
        )

        Spacer(modifier = Modifier.height(10.dp))

        // the relay library: the saved servers, and the two ways one joins
        val showToast: (Int) -> Unit = { resId ->
            Toast.makeText(context, resId, Toast.LENGTH_SHORT).show()
        }
        VlessServersCard(
            servers = viewModel.servers,
            activeServerId = viewModel.activeServerId,
            importing = viewModel.importing,
            editable = editable,
            onActivate = { server ->
                viewModel.activate(
                    server,
                    onActivated = { showToast(R.string.vless_server_activated_toast) },
                    onFailed = { showToast(R.string.vless_server_activate_failed_toast) },
                )
            },
            onRemove = { server ->
                viewModel.removeServer(server)
                showToast(R.string.vless_server_removed_toast)
            },
            onSaveCurrent = {
                viewModel.saveCurrent(
                    onSaved = { showToast(R.string.vless_server_added_toast) },
                    onFailed = { showToast(R.string.vless_server_save_current_failed) },
                )
            },
            onImport = { text ->
                viewModel.importFrom(text) { result ->
                    when (result) {
                        is VlessImportResult.Added -> {
                            Toast.makeText(
                                context,
                                context.getString(
                                    R.string.vless_server_import_added,
                                    result.servers.size,
                                ),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        VlessImportResult.NoneReadable ->
                            showToast(R.string.vless_server_import_none)
                        VlessImportResult.FetchFailed ->
                            showToast(R.string.vless_server_import_failed)
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        /**
         * The share link: a pasted link fills the whole form, and the form
         * copies out as one
         */
        URTextInput(
            value = link,
            onValueChange = {
                link = it
                viewModel.clearLinkError()
            },
            label = stringResource(id = R.string.vless_link),
            placeholder = stringResource(id = R.string.vless_link_hint),
            keyboardOptions = vlessKeyboardOptions(KeyboardType.Uri, ImeAction.Done),
            onDone = { viewModel.applyLink(link.text) },
            enabled = editable,
        )

        URButton(
            onClick = {
                // the clipboard goes into the box; with nothing on it, the
                // box's own text is read
                val clip = clipboardManager.getText()?.text.orEmpty().trim()
                if (clip.isNotEmpty()) {
                    link = TextFieldValue(clip, TextRange(clip.length))
                }
                viewModel.applyLink(clip.ifEmpty { link.text })
            },
            style = ButtonStyle.SECONDARY,
            enabled = editable,
        ) { style ->
            Text(stringResource(id = R.string.vless_paste_link), style = style)
        }

        Spacer(modifier = Modifier.height(12.dp))

        URButton(
            onClick = {
                val shareLink = viewModel.shareLink()
                if (shareLink.isNotEmpty()) {
                    clipboardManager.setText(AnnotatedString(shareLink))
                    Toast.makeText(
                        context,
                        R.string.vless_link_copied,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            style = ButtonStyle.SECONDARY,
            // only settings that validate have a link
            enabled = editable && viewModel.validationErrorId.isEmpty(),
        ) { style ->
            Text(stringResource(id = R.string.vless_copy_link), style = style)
        }

        if (viewModel.linkErrorId.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            URInlineErrorText(stringResource(id = vlessErrorResId(viewModel.linkErrorId)))
        }

        Spacer(modifier = Modifier.height(32.dp))

        VlessTextField(
            value = form.name,
            onValueChange = { value -> viewModel.update { it.copy(name = value) } },
            label = stringResource(id = R.string.name_label),
            enabled = editable,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )

        VlessTextField(
            value = form.address,
            onValueChange = { value -> viewModel.update { it.copy(address = value) } },
            label = stringResource(id = R.string.vless_server_address),
            enabled = editable,
            keyboardOptions = vlessKeyboardOptions(KeyboardType.Uri),
        )

        VlessTextField(
            value = form.port,
            onValueChange = { value -> viewModel.update { it.copy(port = vlessPortText(value)) } },
            label = stringResource(id = R.string.vless_port),
            enabled = editable,
            keyboardOptions = vlessKeyboardOptions(KeyboardType.Number),
        )

        VlessTextField(
            value = form.id,
            onValueChange = { value -> viewModel.update { it.copy(id = value) } },
            label = stringResource(id = R.string.vless_user_id),
            enabled = editable,
        )

        VlessPicker(
            label = stringResource(id = R.string.transport),
            options = viewModel.networks,
            selected = form.network,
            optionLabelResId = ::vlessNetworkLabelResId,
            onSelect = { value -> viewModel.update { it.copy(network = value) } },
            enabled = editable,
        )

        VlessPicker(
            label = stringResource(id = R.string.vless_security),
            options = viewModel.securities,
            selected = form.security,
            optionLabelResId = ::vlessSecurityLabelResId,
            onSelect = { value -> viewModel.update { it.copy(security = value) } },
            enabled = editable,
        )

        if (form.showsFlow) {
            VlessPicker(
                label = stringResource(id = R.string.vless_flow),
                options = viewModel.flows,
                selected = form.flow,
                optionLabelResId = ::vlessFlowLabelResId,
                onSelect = { value -> viewModel.update { it.copy(flow = value) } },
                enabled = editable,
            )
        }

        if (form.showsServerName) {
            VlessTextField(
                value = form.serverName,
                onValueChange = { value -> viewModel.update { it.copy(serverName = value) } },
                label = stringResource(id = R.string.vless_server_name),
                enabled = editable,
                keyboardOptions = vlessKeyboardOptions(KeyboardType.Uri),
            )

            VlessPicker(
                label = stringResource(id = R.string.vless_fingerprint),
                options = viewModel.fingerprints,
                selected = form.fingerprint,
                optionLabelResId = ::vlessFingerprintLabelResId,
                onSelect = { value -> viewModel.update { it.copy(fingerprint = value) } },
                enabled = editable,
            )
        }

        if (form.showsTlsOptions) {
            VlessTextField(
                value = form.alpn,
                onValueChange = { value -> viewModel.update { it.copy(alpn = value) } },
                label = stringResource(id = R.string.vless_alpn),
                enabled = editable,
            )

            VlessSwitchRow(
                text = stringResource(id = R.string.vless_allow_insecure),
                checked = form.allowInsecure,
                enabled = editable,
                toggle = { viewModel.update { it.copy(allowInsecure = !it.allowInsecure) } },
            )

            // the switch's 48 dp touch target fills the rest
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (form.showsRealityKeys) {
            VlessTextField(
                value = form.publicKey,
                onValueChange = { value -> viewModel.update { it.copy(publicKey = value) } },
                label = stringResource(id = R.string.vless_public_key),
                enabled = editable,
            )

            VlessTextField(
                value = form.shortId,
                onValueChange = { value -> viewModel.update { it.copy(shortId = value) } },
                label = stringResource(id = R.string.vless_short_id),
                enabled = editable,
            )
        }

        if (form.showsHttpOptions) {
            VlessTextField(
                value = form.path,
                onValueChange = { value -> viewModel.update { it.copy(path = value) } },
                label = stringResource(id = R.string.vless_path),
                enabled = editable,
                keyboardOptions = vlessKeyboardOptions(KeyboardType.Uri),
            )

            VlessTextField(
                value = form.host,
                onValueChange = { value -> viewModel.update { it.copy(host = value) } },
                label = stringResource(id = R.string.vless_host_header),
                enabled = editable,
                keyboardOptions = vlessKeyboardOptions(KeyboardType.Uri),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        URButton(
            onClick = {
                viewModel.save(
                    onSaved = {
                        Toast.makeText(
                            context,
                            R.string.vless_settings_saved,
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                )
            },
            enabled = editable,
            isProcessing = viewModel.saving,
        ) { style ->
            Text(stringResource(id = R.string.save), style = style)
        }

        val errorId = vlessFormErrorId(
            enabled = form.enabled,
            validationErrorId = viewModel.validationErrorId,
            saveErrorId = viewModel.saveErrorId,
        )
        if (errorId.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            URInlineErrorText(stringResource(id = vlessErrorResId(errorId)))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * The keyboard of a server field: an address, an id or a key, which a
 * correction or a capital would break.
 */
private fun vlessKeyboardOptions(
    keyboardType: KeyboardType = KeyboardType.Ascii,
    imeAction: ImeAction = ImeAction.Next,
) = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = keyboardType,
    imeAction = imeAction,
)

/**
 * One text field of the form. The form holds strings, so this keeps the
 * cursor while typing, and a value changed from outside -- a load, a pasted
 * link, a filtered port -- replaces the text with the cursor at its end.
 */
@Composable
private fun VlessTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    enabled: Boolean,
    keyboardOptions: KeyboardOptions = vlessKeyboardOptions(),
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val shown = if (field.text == value) {
        field
    } else {
        TextFieldValue(value, TextRange(value.length))
    }

    URTextInput(
        value = shown,
        onValueChange = {
            field = it
            onValueChange(it.text)
        },
        label = label,
        keyboardOptions = keyboardOptions,
        enabled = enabled,
    )
}

/**
 * A picker of the sdk's option values. An option shows its label, or the value
 * itself where it has none (the tls fingerprints).
 */
@Composable
private fun VlessPicker(
    label: String,
    options: List<String>,
    selected: String,
    optionLabelResId: (String) -> Int?,
    onSelect: (String) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {

        URTextInputLabel(text = label)

        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        enabled = enabled,
                        onClickLabel = label,
                        role = Role.DropdownList,
                    ) {
                        expanded = true
                    }
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    vlessOptionText(selected, optionLabelResId),
                    style = TextStyle(color = if (enabled) Color.LightGray else AppTheme.textFaint),
                )
                Icon(
                    Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AppTheme.textMuted,
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                containerColor = AppTheme.bgTinted,
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                vlessOptionText(option, optionLabelResId),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        onClick = {
                            expanded = false
                            onSelect(option)
                        },
                    )
                }
            }
        }

        HorizontalDivider(color = AppTheme.textFaint, thickness = 1.dp)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun vlessOptionText(option: String, optionLabelResId: (String) -> Int?): String =
    optionLabelResId(option)?.let { stringResource(id = it) } ?: option

@Composable
private fun VlessSwitchRow(
    text: String,
    checked: Boolean,
    enabled: Boolean,
    toggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.textPrimary,
            modifier = Modifier.weight(1f),
        )
        // the switch's touch target adds 6 dp of its own
        Spacer(modifier = Modifier.width(10.dp))
        URSwitch(
            checked = checked,
            enabled = enabled,
            toggle = toggle,
        )
    }
}

/**
 * The relay library card: the saved servers with the active one badged, and
 * the two ways a server joins -- the current form saved as one, or links and
 * subscriptions imported together. Rows answer a tap with the app's bounce
 * and settle in with the shared cascade; the card grows and shrinks softly
 * as servers join and leave.
 */
@Composable
private fun VlessServersCard(
    servers: List<VlessServer>,
    activeServerId: String?,
    importing: Boolean,
    editable: Boolean,
    onActivate: (VlessServer) -> Unit,
    onRemove: (VlessServer) -> Unit,
    onSaveCurrent: () -> Unit,
    onImport: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.bgElevated, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = 0.9f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            .entrance(1),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(id = R.string.vless_servers_title),
                style = MaterialTheme.typography.titleMedium,
                color = AppTheme.textPrimary,
            )
            AnimatedVisibility(visible = servers.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AppTheme.bgTinted,
                ) {
                    Text(
                        servers.size.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppTheme.textMuted,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            stringResource(id = R.string.vless_server_activate_note),
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.textFaint,
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (servers.isEmpty()) {
            Text(
                stringResource(id = R.string.vless_servers_empty),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.textMuted,
                modifier = Modifier.entrance(2),
            )
        } else {
            Column {
                servers.forEachIndexed { index, server ->
                    VlessServerRow(
                        server = server,
                        active = server.id == activeServerId,
                        editable = editable,
                        onActivate = { onActivate(server) },
                        onRemove = { onRemove(server) },
                        modifier = Modifier.entrance(min(index + 1, 6)),
                    )
                    if (index < servers.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        URButton(
            onClick = onSaveCurrent,
            style = ButtonStyle.SECONDARY,
            enabled = editable,
        ) { style ->
            Text(stringResource(id = R.string.vless_server_add_current), style = style)
        }

        Spacer(modifier = Modifier.height(12.dp))

        var importText by remember { mutableStateOf(TextFieldValue()) }
        URTextInput(
            value = importText,
            onValueChange = {
                importText = it
            },
            label = stringResource(id = R.string.vless_server_import_label),
            placeholder = stringResource(id = R.string.vless_server_import_hint),
            keyboardOptions = vlessKeyboardOptions(KeyboardType.Uri, ImeAction.Done),
            onDone = {
                onImport(importText.text)
                importText = TextFieldValue()
            },
            enabled = editable && !importing,
        )

        URButton(
            onClick = {
                onImport(importText.text)
                importText = TextFieldValue()
            },
            style = ButtonStyle.SECONDARY,
            enabled = editable && !importing && importText.text.isNotBlank(),
            isProcessing = importing,
        ) { style ->
            Text(stringResource(id = R.string.vless_server_add_button), style = style)
        }
    }
}

/**
 * One saved server of the library: its name and address on the tap that
 * activates it, the active badge riding in beside the name, and the quiet
 * remove button at the row's end.
 */
@Composable
private fun VlessServerRow(
    server: VlessServer,
    active: Boolean,
    editable: Boolean,
    onActivate: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val press = remember { MutableInteractionSource() }
    val removePress = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) AppTheme.bgTinted else Color.Transparent)
            .pressScale(press, scaleDown = 0.97f)
            .clickable(
                interactionSource = press,
                indication = null,
                enabled = editable,
                onClickLabel = server.name,
            ) { onActivate() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    server.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) Pink else AppTheme.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                AnimatedVisibility(visible = active) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Pink.copy(alpha = 0.16f),
                        ) {
                            Text(
                                stringResource(id = R.string.vless_server_active),
                                style = MaterialTheme.typography.labelSmall,
                                color = Pink,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
            Text(
                server.address,
                style = MaterialTheme.typography.bodySmall.copy(
                    textDirection = TextDirection.Ltr,
                ),
                color = AppTheme.textFaint,
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .pressScale(removePress, scaleDown = 0.88f)
                .clickable(
                    interactionSource = removePress,
                    indication = null,
                    enabled = editable,
                ) { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = stringResource(id = R.string.vless_server_remove),
                tint = AppTheme.textFaint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
