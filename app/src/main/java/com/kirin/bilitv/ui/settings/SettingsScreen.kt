package com.kirin.bilitv.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.kirin.bilitv.R
import com.kirin.bilitv.core.i18n.ChineseTextVariant
import com.kirin.bilitv.core.model.HomeSection
import com.kirin.bilitv.core.player.CodecCapability
import com.kirin.bilitv.core.player.PlaybackCodecPreference
import com.kirin.bilitv.core.player.PlaybackQualityPreference
import com.kirin.bilitv.core.settings.AppSettings
import com.kirin.bilitv.ui.focus.BiliFocusableSurface
import com.kirin.bilitv.ui.home.titleRes
import com.kirin.bilitv.ui.theme.BiliColors
import com.kirin.bilitv.ui.theme.BiliRadius
import com.kirin.bilitv.ui.theme.BiliSizing
import com.kirin.bilitv.ui.theme.BiliSpacing
import com.kirin.bilitv.ui.theme.BiliTypography
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs

private val SettingsBringIntoViewSpec = object : BringIntoViewSpec {
  override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
    val childEnd = offset + size
    return when {
      offset < 0f && childEnd > containerSize -> 0f
      offset < 0f -> offset
      childEnd > containerSize -> childEnd - containerSize
      else -> 0f
    }
  }
}

@Composable
fun SettingsScreen(
  settings: AppSettings,
  cacheSizeText: String,
  codecCapability: CodecCapability,
  firstItemFocusRequester: FocusRequester,
  onMoveLeftToNav: () -> Boolean,
  onLowSpecModeChange: (Boolean) -> Unit,
  onChineseTextVariantChange: (ChineseTextVariant) -> Unit,
  onClearCache: () -> Unit,
  onSeekPreviewSpritesEnabledChange: (Boolean) -> Unit,
  onPlaybackQualityPreferenceChange: (PlaybackQualityPreference) -> Unit,
  onPlaybackCodecPreferenceChange: (PlaybackCodecPreference) -> Unit,
  onAirJumpAssistantEnabledChange: (Boolean) -> Unit,
  onConfirmPlaybackExitChange: (Boolean) -> Unit,
  onAutoPlayNextEpisodeChange: (Boolean) -> Unit,
  onAutoPlayRelatedVideoChange: (Boolean) -> Unit,
  onAutoReturnHomeOnCompletionChange: (Boolean) -> Unit,
  onShowClockChange: (Boolean) -> Unit,
  onAutoConfirmOnFocusChange: (Boolean) -> Unit,
  onAutoRefreshOnSwitchChange: (Boolean) -> Unit,
  onHomeSectionEnabledChange: (HomeSection, Boolean) -> Unit,
) {
  val settingsListState = rememberLazyListState()
  val coroutineScope = rememberCoroutineScope()
  val density = LocalDensity.current
  val settingsRowFallbackHeightPx = with(density) {
    (BiliSizing.SettingsRowHeight + BiliSpacing.Md).roundToPx()
  }
  val settingsScrollInsetPx = with(density) {
    BiliSpacing.Md.roundToPx()
  }
  val settingFocusRequesters = remember {
    mapOf(
      SettingsItemPlaybackQuality to FocusRequester(),
      SettingsItemChineseTextVariant to FocusRequester(),
      SettingsItemClearCache to FocusRequester(),
      SettingsItemPlaybackCodec to FocusRequester(),
      SettingsItemSeekPreviewSprites to FocusRequester(),
      SettingsItemAirJumpAssistant to FocusRequester(),
      SettingsItemConfirmPlaybackExit to FocusRequester(),
      SettingsItemAutoPlayNextEpisode to FocusRequester(),
      SettingsItemAutoPlayRelatedVideo to FocusRequester(),
      SettingsItemAutoReturnHomeOnCompletion to FocusRequester(),
      SettingsItemShowClock to FocusRequester(),
      SettingsItemAutoConfirmOnFocus to FocusRequester(),
      SettingsItemAutoRefreshOnSwitch to FocusRequester(),
      SettingsItemLowSpec to FocusRequester(),
    )
  }
  var lastFocusedSettingItem by remember { mutableIntStateOf(SettingsItemPlaybackQuality) }
  var focusSettingJob by remember { mutableStateOf<Job?>(null) }

  fun focusSettingItem(itemIndex: Int, direction: Int = 0): Boolean {
    focusSettingJob?.cancel()
    focusSettingJob = coroutineScope.launch {
      settingsListState.scrollItemIntoComfortableView(
        index = if (itemIndex == SettingsItemPlaybackQuality) {
          SettingsItemPlaybackHeader
        } else {
          itemIndex
        },
        direction = direction,
        fallbackItemHeightPx = settingsRowFallbackHeightPx,
        edgeInsetPx = settingsScrollInsetPx,
      )
      withFrameNanos { }
      settingFocusRequesters[itemIndex]?.requestFocus()
    }
    return true
  }

  fun moveSettingFocus(itemIndex: Int, direction: Int): Boolean {
    val currentOrderIndex = SettingsFocusableItems.indexOf(itemIndex)
    val targetItem = SettingsFocusableItems.getOrNull(currentOrderIndex + direction) ?: return true
    return focusSettingItem(targetItem, direction)
  }

  Box(
    modifier = Modifier.fillMaxSize(),
  ) {
    SettingsEntryFocusTarget(
      focusRequester = firstItemFocusRequester,
      onFocused = {
        focusSettingItem(lastFocusedSettingItem)
      },
    )
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = BiliSpacing.Md),
      horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Xl),
    ) {
      SettingsBehaviorColumn(
        settings = settings,
        cacheSizeText = cacheSizeText,
        codecCapability = codecCapability,
        listState = settingsListState,
        focusRequesters = settingFocusRequesters,
        onSettingFocused = { itemIndex ->
          lastFocusedSettingItem = itemIndex
        },
        onMoveSettingFocus = ::moveSettingFocus,
        onMoveLeftToNav = onMoveLeftToNav,
        onLowSpecModeChange = onLowSpecModeChange,
        onChineseTextVariantChange = onChineseTextVariantChange,
        onClearCache = onClearCache,
        onSeekPreviewSpritesEnabledChange = onSeekPreviewSpritesEnabledChange,
        onPlaybackQualityPreferenceChange = onPlaybackQualityPreferenceChange,
        onPlaybackCodecPreferenceChange = onPlaybackCodecPreferenceChange,
        onAirJumpAssistantEnabledChange = onAirJumpAssistantEnabledChange,
        onConfirmPlaybackExitChange = onConfirmPlaybackExitChange,
        onAutoPlayNextEpisodeChange = onAutoPlayNextEpisodeChange,
        onAutoPlayRelatedVideoChange = onAutoPlayRelatedVideoChange,
        onAutoReturnHomeOnCompletionChange = onAutoReturnHomeOnCompletionChange,
        onShowClockChange = onShowClockChange,
        onAutoConfirmOnFocusChange = onAutoConfirmOnFocusChange,
        onAutoRefreshOnSwitchChange = onAutoRefreshOnSwitchChange,
        modifier = Modifier.weight(1f),
      )
      SettingsHomeSectionsColumn(
        settings = settings,
        onMoveLeftToSettings = { focusSettingItem(lastFocusedSettingItem) },
        onHomeSectionEnabledChange = onHomeSectionEnabledChange,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SettingsBehaviorColumn(
  settings: AppSettings,
  cacheSizeText: String,
  codecCapability: CodecCapability,
  listState: LazyListState,
  focusRequesters: Map<Int, FocusRequester>,
  onSettingFocused: (Int) -> Unit,
  onMoveSettingFocus: (Int, Int) -> Boolean,
  onMoveLeftToNav: () -> Boolean,
  onLowSpecModeChange: (Boolean) -> Unit,
  onChineseTextVariantChange: (ChineseTextVariant) -> Unit,
  onClearCache: () -> Unit,
  onSeekPreviewSpritesEnabledChange: (Boolean) -> Unit,
  onPlaybackQualityPreferenceChange: (PlaybackQualityPreference) -> Unit,
  onPlaybackCodecPreferenceChange: (PlaybackCodecPreference) -> Unit,
  onAirJumpAssistantEnabledChange: (Boolean) -> Unit,
  onConfirmPlaybackExitChange: (Boolean) -> Unit,
  onAutoPlayNextEpisodeChange: (Boolean) -> Unit,
  onAutoPlayRelatedVideoChange: (Boolean) -> Unit,
  onAutoReturnHomeOnCompletionChange: (Boolean) -> Unit,
  onShowClockChange: (Boolean) -> Unit,
  onAutoConfirmOnFocusChange: (Boolean) -> Unit,
  onAutoRefreshOnSwitchChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  CompositionLocalProvider(LocalBringIntoViewSpec provides SettingsBringIntoViewSpec) {
    LazyColumn(
      state = listState,
      modifier = modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = BiliSpacing.Xxl),
      verticalArrangement = Arrangement.spacedBy(BiliSpacing.Md),
    ) {
      item(key = "playback-header") {
        SettingsSectionTitle(text = stringResource(R.string.settings_playback_section))
      }
      item(key = "playback-quality") {
        val qualityOptions = remember { PlaybackQualityPreference.entries.toList() }
        val effectivePreference = settings.playbackQualityPreference
        SettingsOptionRow(
          title = stringResource(R.string.settings_playback_quality_title),
          description = stringResource(R.string.settings_playback_quality_description),
          value = effectivePreference.qualityLabel(),
          modifier = Modifier
            .focusRequester(focusRequesters.getValue(SettingsItemPlaybackQuality))
            .settingsBoundaryKeys(
              itemIndex = SettingsItemPlaybackQuality,
              onMoveSettingFocus = onMoveSettingFocus,
              onMoveLeftToNav = onMoveLeftToNav,
            ),
          onFocused = { onSettingFocused(SettingsItemPlaybackQuality) },
          onClick = {
            val currentIndex = qualityOptions.indexOf(effectivePreference).takeIf { it >= 0 } ?: 0
            onPlaybackQualityPreferenceChange(qualityOptions[(currentIndex + 1) % qualityOptions.size])
          },
        )
      }
      item(key = "playback-codec") {
        val codecOptions = remember(codecCapability) { codecCapability.playbackCodecOptions() }
        val configuredPreference = settings.playbackCodecPreference.takeIf { preference ->
          preference in codecOptions
        } ?: PlaybackCodecPreference.Auto
        val effectivePreference = if (settings.lowSpecMode) {
          PlaybackCodecPreference.H264
        } else {
          configuredPreference
        }
        SettingsOptionRow(
          title = stringResource(R.string.settings_playback_codec_title),
          description = stringResource(R.string.settings_playback_codec_description),
          value = effectivePreference.codecLabel(),
          modifier = Modifier
            .focusRequester(focusRequesters.getValue(SettingsItemPlaybackCodec))
            .settingsBoundaryKeys(
              itemIndex = SettingsItemPlaybackCodec,
              onMoveSettingFocus = onMoveSettingFocus,
              onMoveLeftToNav = onMoveLeftToNav,
            ),
          onFocused = { onSettingFocused(SettingsItemPlaybackCodec) },
          onClick = {
            val currentIndex = codecOptions.indexOf(configuredPreference).takeIf { it >= 0 } ?: 0
            onPlaybackCodecPreferenceChange(codecOptions[(currentIndex + 1) % codecOptions.size])
          },
        )
      }
      item(key = "seek-preview-sprites") {
        SettingsToggleRow(
        title = stringResource(R.string.settings_seek_preview_sprites_title),
        description = stringResource(R.string.settings_seek_preview_sprites_description),
        checked = settings.seekPreviewSpritesEnabled,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemSeekPreviewSprites))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemSeekPreviewSprites,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemSeekPreviewSprites) },
        onCheckedChange = onSeekPreviewSpritesEnabledChange,
      )
    }
    item(key = "air-jump-assistant") {
      SettingsToggleRow(
        title = stringResource(R.string.settings_air_jump_assistant_title),
        description = stringResource(R.string.settings_air_jump_assistant_description),
        checked = settings.airJumpAssistantEnabled,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemAirJumpAssistant))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemAirJumpAssistant,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemAirJumpAssistant) },
        onCheckedChange = onAirJumpAssistantEnabledChange,
      )
    }
    item(key = "confirm-playback-exit") {
      SettingsToggleRow(
        title = stringResource(R.string.settings_confirm_playback_exit_title),
        description = stringResource(R.string.settings_confirm_playback_exit_description),
        checked = settings.confirmPlaybackExit,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemConfirmPlaybackExit))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemConfirmPlaybackExit,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemConfirmPlaybackExit) },
        onCheckedChange = onConfirmPlaybackExitChange,
      )
    }
    item(key = "auto-play-next-episode") {
      SettingsToggleRow(
        title = stringResource(R.string.settings_auto_play_next_episode_title),
        description = stringResource(R.string.settings_auto_play_next_episode_description),
        checked = settings.autoPlayNextEpisode,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemAutoPlayNextEpisode))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemAutoPlayNextEpisode,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemAutoPlayNextEpisode) },
        onCheckedChange = onAutoPlayNextEpisodeChange,
      )
    }
    item(key = "auto-play-related-video") {
      SettingsToggleRow(
        title = stringResource(R.string.settings_auto_play_related_video_title),
        description = stringResource(R.string.settings_auto_play_related_video_description),
        checked = settings.autoPlayRelatedVideo,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemAutoPlayRelatedVideo))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemAutoPlayRelatedVideo,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemAutoPlayRelatedVideo) },
        onCheckedChange = onAutoPlayRelatedVideoChange,
      )
    }
    item(key = "auto-return-home-on-completion") {
      SettingsToggleRow(
        title = stringResource(R.string.settings_auto_return_home_on_completion_title),
        description = stringResource(R.string.settings_auto_return_home_on_completion_description),
        checked = settings.autoReturnHomeOnCompletion,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemAutoReturnHomeOnCompletion))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemAutoReturnHomeOnCompletion,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemAutoReturnHomeOnCompletion) },
        onCheckedChange = onAutoReturnHomeOnCompletionChange,
      )
    }
    item(key = "ui-header") {
      SettingsSectionTitle(
        text = stringResource(R.string.settings_interaction_section),
        modifier = Modifier.padding(top = BiliSpacing.Lg),
      )
    }
    item(key = "show-clock") {
      SettingsToggleRow(
        title = stringResource(R.string.settings_show_clock_title),
        description = stringResource(R.string.settings_show_clock_description),
        checked = settings.showClock,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemShowClock))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemShowClock,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemShowClock) },
        onCheckedChange = onShowClockChange,
      )
    }
    item(key = "auto-confirm-on-focus") {
      SettingsToggleRow(
        title = stringResource(R.string.settings_auto_confirm_on_focus_title),
        description = stringResource(R.string.settings_auto_confirm_on_focus_description),
        checked = settings.autoConfirmOnFocus,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemAutoConfirmOnFocus))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemAutoConfirmOnFocus,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemAutoConfirmOnFocus) },
        onCheckedChange = onAutoConfirmOnFocusChange,
      )
    }
    item(key = "auto-refresh-on-switch") {
      SettingsToggleRow(
        title = stringResource(R.string.settings_auto_refresh_on_switch_title),
        description = stringResource(R.string.settings_auto_refresh_on_switch_description),
        checked = settings.autoConfirmOnFocus && settings.autoRefreshOnSwitch,
        enabled = settings.autoConfirmOnFocus,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemAutoRefreshOnSwitch))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemAutoRefreshOnSwitch,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemAutoRefreshOnSwitch) },
        onCheckedChange = onAutoRefreshOnSwitchChange,
      )
    }
    item(key = "system-header") {
      SettingsSectionTitle(
        text = stringResource(R.string.settings_performance_section),
        modifier = Modifier.padding(top = BiliSpacing.Lg),
      )
    }
    item(key = "low-spec-mode") {
      SettingsToggleRow(
        title = stringResource(R.string.settings_low_spec_mode_title),
        description = stringResource(R.string.settings_low_spec_mode_description),
        checked = settings.lowSpecMode,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemLowSpec))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemLowSpec,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
        ),
        onFocused = { onSettingFocused(SettingsItemLowSpec) },
        onCheckedChange = onLowSpecModeChange,
      )
    }
    item(key = "clear-cache") {
      SettingsActionRow(
        title = stringResource(R.string.settings_clear_cache_title),
        description = stringResource(R.string.settings_clear_cache_description),
        value = cacheSizeText,
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemClearCache))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemClearCache,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemClearCache) },
        onClick = onClearCache,
      )
    }
    item(key = "chinese-text-variant") {
      val languageOptions = remember { ChineseTextVariant.entries.toList() }
      val effectiveVariant = settings.chineseTextVariant
      SettingsOptionRow(
        title = stringResource(R.string.settings_language_title),
        description = stringResource(R.string.settings_language_description),
        value = effectiveVariant.languageLabel(),
        modifier = Modifier
          .focusRequester(focusRequesters.getValue(SettingsItemChineseTextVariant))
          .settingsBoundaryKeys(
            itemIndex = SettingsItemChineseTextVariant,
            onMoveSettingFocus = onMoveSettingFocus,
            onMoveLeftToNav = onMoveLeftToNav,
          ),
        onFocused = { onSettingFocused(SettingsItemChineseTextVariant) },
        onClick = {
          val currentIndex = languageOptions.indexOf(effectiveVariant).takeIf { it >= 0 } ?: 0
          onChineseTextVariantChange(languageOptions[(currentIndex + 1) % languageOptions.size])
        },
      )
    }
  }
  }
}

@Composable
private fun SettingsEntryFocusTarget(
  focusRequester: FocusRequester,
  onFocused: () -> Unit,
) {
  Box(
    modifier = Modifier
      .size(BiliSpacing.Xs)
      .focusRequester(focusRequester)
      .onFocusChanged { focusState ->
        if (focusState.isFocused) {
          onFocused()
        }
      }
      .focusTarget(),
  )
}

@Composable
private fun SettingsSectionTitle(
  text: String,
  modifier: Modifier = Modifier,
) {
  Text(
    text = text,
    color = BiliColors.TextSecondary,
    fontSize = BiliTypography.SectionTitle,
    fontWeight = FontWeight.Bold,
    modifier = modifier,
  )
}

@Composable
private fun SettingsHomeSectionsColumn(
  settings: AppSettings,
  onMoveLeftToSettings: () -> Boolean,
  onHomeSectionEnabledChange: (HomeSection, Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(BiliSpacing.Md),
  ) {
    Text(
      text = stringResource(R.string.settings_home_sections_section),
      color = BiliColors.TextSecondary,
      fontSize = BiliTypography.SectionTitle,
      fontWeight = FontWeight.Bold,
    )
    Text(
      text = stringResource(R.string.settings_home_sections_description),
      color = BiliColors.TextSecondary,
      fontSize = BiliTypography.BodySmall,
    )
    LazyVerticalGrid(
      columns = GridCells.Fixed(BiliSizing.SettingsHomeSectionColumns),
      modifier = Modifier
        .fillMaxWidth()
        .height(BiliSizing.SettingsHomeSectionGridHeight),
      horizontalArrangement = Arrangement.spacedBy(BiliSpacing.Md),
      verticalArrangement = Arrangement.spacedBy(BiliSpacing.Md),
      userScrollEnabled = false,
    ) {
      itemsIndexed(HomeSection.DefaultOrder, key = { _, section -> section.key }) { index, section ->
        HomeSectionChip(
          title = stringResource(section.titleRes()),
          selected = section in settings.enabledHomeSections,
          modifier = Modifier.settingsSectionChipBoundaryKeys(
            index = index,
            columns = BiliSizing.SettingsHomeSectionColumns,
            onMoveLeftToSettings = onMoveLeftToSettings,
          ),
          onClick = {
            onHomeSectionEnabledChange(section, section !in settings.enabledHomeSections)
          },
        )
      }
    }
  }
}

@Composable
private fun HomeSectionChip(
  title: String,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit,
) {
  BiliFocusableSurface(
    scaleOnFocus = false,
    shape = RoundedCornerShape(BiliRadius.Pill),
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.SettingsChipHeight),
  ) {
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = title,
        color = if (selected) BiliColors.BiliPink else BiliColors.TextSecondary,
        fontSize = BiliTypography.BodySmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      )
    }
  }
}

private fun Modifier.settingsBoundaryKeys(
  itemIndex: Int,
  onMoveSettingFocus: (Int, Int) -> Boolean,
  onMoveLeftToNav: () -> Boolean,
): Modifier {
  return onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) {
      return@onPreviewKeyEvent false
    }
    when (event.key) {
      Key.DirectionUp -> onMoveSettingFocus(itemIndex, -1)
      Key.DirectionDown -> onMoveSettingFocus(itemIndex, 1)
      Key.DirectionLeft -> onMoveLeftToNav()
      else -> false
    }
  }
}

private fun Modifier.settingsSectionChipBoundaryKeys(
  index: Int,
  columns: Int,
  onMoveLeftToSettings: () -> Boolean,
): Modifier {
  return onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) {
      return@onPreviewKeyEvent false
    }
    when (event.key) {
      Key.DirectionUp -> index < columns
      Key.DirectionLeft -> if (index % columns == 0) onMoveLeftToSettings() else false
      else -> false
    }
  }
}

private suspend fun LazyListState.scrollItemIntoComfortableView(
  index: Int,
  direction: Int,
  fallbackItemHeightPx: Int,
  edgeInsetPx: Int,
) {
  val totalItems = layoutInfo.totalItemsCount
  if (totalItems <= 0) {
    return
  }

  val safeIndex = index.coerceIn(0, totalItems - 1)
  val layout = layoutInfo
  val viewportTop = layout.viewportStartOffset + edgeInsetPx
  val viewportBottom = layout.viewportEndOffset - edgeInsetPx
  val focusedItem = layout.visibleItemsInfo.firstOrNull { item -> item.index == safeIndex }

  if (focusedItem != null) {
    val itemTop = focusedItem.offset
    val itemBottom = itemTop + focusedItem.size
    val scrollDelta = when {
      itemTop < viewportTop -> itemTop - viewportTop
      itemBottom > viewportBottom -> itemBottom - viewportBottom
      else -> 0
    }
    if (abs(scrollDelta) <= 1) {
      return
    }
    scroll {
      scrollBy(scrollDelta.toFloat())
    }
    return
  }

  val viewportHeight = layout.viewportEndOffset - layout.viewportStartOffset
  val itemHeightPx = layout.visibleItemsInfo.firstOrNull()?.size ?: fallbackItemHeightPx
  val maxTop = (viewportHeight - itemHeightPx - edgeInsetPx).coerceAtLeast(edgeInsetPx)
  val desiredTop = when {
    direction > 0 -> maxTop
    direction < 0 -> edgeInsetPx
    else -> ((viewportHeight - itemHeightPx) / 2).coerceIn(edgeInsetPx, maxTop)
  }
  scrollToItem(safeIndex, scrollOffset = -desiredTop)
}

private const val SettingsItemPlaybackHeader = 0
private const val SettingsItemPlaybackQuality = 1
private const val SettingsItemPlaybackCodec = 2
private const val SettingsItemSeekPreviewSprites = 3
private const val SettingsItemAirJumpAssistant = 4
private const val SettingsItemConfirmPlaybackExit = 5
private const val SettingsItemAutoPlayNextEpisode = 6
private const val SettingsItemAutoPlayRelatedVideo = 7
private const val SettingsItemAutoReturnHomeOnCompletion = 8
private const val SettingsItemShowClock = 10
private const val SettingsItemAutoConfirmOnFocus = 11
private const val SettingsItemAutoRefreshOnSwitch = 12
private const val SettingsItemLowSpec = 14
private const val SettingsItemClearCache = 15
private const val SettingsItemChineseTextVariant = 16

private val SettingsFocusableItems = listOf(
  SettingsItemPlaybackQuality,
  SettingsItemPlaybackCodec,
  SettingsItemSeekPreviewSprites,
  SettingsItemAirJumpAssistant,
  SettingsItemConfirmPlaybackExit,
  SettingsItemAutoPlayNextEpisode,
  SettingsItemAutoPlayRelatedVideo,
  SettingsItemAutoReturnHomeOnCompletion,
  SettingsItemShowClock,
  SettingsItemAutoConfirmOnFocus,
  SettingsItemAutoRefreshOnSwitch,
  SettingsItemLowSpec,
  SettingsItemClearCache,
  SettingsItemChineseTextVariant,
)

@Composable
private fun SettingsOptionRow(
  title: String,
  description: String,
  value: String,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
  onClick: () -> Unit,
) {
  BiliFocusableSurface(
    scaleOnFocus = false,
    shape = RoundedCornerShape(BiliRadius.Panel),
    onClick = onClick,
    onFocused = onFocused,
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.SettingsRowHeight),
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = BiliSpacing.Lg, end = BiliSpacing.Xl),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = title,
          color = BiliColors.TextPrimary,
          fontSize = BiliTypography.Body,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = description,
          color = BiliColors.TextSecondary,
          fontSize = BiliTypography.BodySmall,
          modifier = Modifier.padding(top = BiliSpacing.Xs),
        )
      }
      Text(
        text = value,
        color = BiliColors.BiliPink,
        fontSize = BiliTypography.Body,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.End,
        modifier = Modifier
          .padding(start = BiliSpacing.Lg)
          .width(BiliSizing.SettingsCodecValueWidth),
      )
    }
  }
}

@Composable
private fun SettingsActionRow(
  title: String,
  description: String,
  value: String,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
  onClick: () -> Unit,
) {
  SettingsOptionRow(
    title = title,
    description = description,
    value = value,
    modifier = modifier,
    onFocused = onFocused,
    onClick = onClick,
  )
}

@Composable
private fun SettingsToggleRow(
  title: String,
  description: String,
  checked: Boolean,
  enabled: Boolean = true,
  modifier: Modifier = Modifier,
  onFocused: () -> Unit = {},
  onCheckedChange: (Boolean) -> Unit,
) {
  BiliFocusableSurface(
    scaleOnFocus = false,
    shape = RoundedCornerShape(BiliRadius.Panel),
    onClick = {
      if (enabled) {
        onCheckedChange(!checked)
      }
    },
    onFocused = onFocused,
    modifier = modifier
      .fillMaxWidth()
      .height(BiliSizing.SettingsRowHeight),
  ) {
    Row(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = BiliSpacing.Lg),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = title,
          color = if (enabled) BiliColors.TextPrimary else BiliColors.TextTertiary,
          fontSize = BiliTypography.Body,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = description,
          color = if (enabled) BiliColors.TextSecondary else BiliColors.TextTertiary,
          fontSize = BiliTypography.BodySmall,
          modifier = Modifier.padding(top = BiliSpacing.Xs),
        )
      }
      Box(
        modifier = Modifier.padding(start = BiliSpacing.Lg),
      ) {
        Switch(
          checked = checked,
          enabled = enabled,
          onCheckedChange = {
            if (enabled) {
              onCheckedChange(it)
            }
          },
          colors = SwitchDefaults.colors(
            checkedTrackColor = BiliColors.BiliPink,
            checkedThumbColor = BiliColors.TextPrimary,
            checkedBorderColor = BiliColors.BiliPink,
          ),
          modifier = Modifier.focusProperties {
            canFocus = false
          },
        )
      }
    }
  }
}

private fun CodecCapability.playbackCodecOptions(): List<PlaybackCodecPreference> {
  return buildList {
    add(PlaybackCodecPreference.Auto)
    if (supportsAv1) add(PlaybackCodecPreference.Av1)
    if (supportsH265) add(PlaybackCodecPreference.H265)
    if (supportsH264) add(PlaybackCodecPreference.H264)
  }
}

@Composable
private fun ChineseTextVariant.languageLabel(): String {
  return when (this) {
    ChineseTextVariant.Simplified -> stringResource(R.string.settings_language_simplified)
    ChineseTextVariant.HongKong -> stringResource(R.string.settings_language_hong_kong)
    ChineseTextVariant.Taiwan -> stringResource(R.string.settings_language_taiwan)
  }
}

@Composable
private fun PlaybackQualityPreference.qualityLabel(): String {
  return when (this) {
    PlaybackQualityPreference.Highest -> stringResource(R.string.settings_playback_quality_highest)
    PlaybackQualityPreference.Q1080 -> stringResource(R.string.settings_playback_quality_1080)
    PlaybackQualityPreference.Q720 -> stringResource(R.string.settings_playback_quality_720)
    PlaybackQualityPreference.Q480 -> stringResource(R.string.settings_playback_quality_480)
  }
}

@Composable
private fun PlaybackCodecPreference.codecLabel(): String {
  return when (this) {
    PlaybackCodecPreference.Auto -> stringResource(R.string.settings_playback_codec_auto)
    PlaybackCodecPreference.H264 -> stringResource(R.string.settings_playback_codec_h264)
    PlaybackCodecPreference.H265 -> stringResource(R.string.settings_playback_codec_h265)
    PlaybackCodecPreference.Av1 -> stringResource(R.string.settings_playback_codec_av1)
  }
}
