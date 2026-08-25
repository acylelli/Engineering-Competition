package com.watchsafety.guardian.ui.safezone

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.skt.tmap.TMapView

import com.watchsafety.guardian.ui.components.GuardianTopBar
import com.watchsafety.guardian.ui.map.TmapMapView
import com.watchsafety.guardian.ui.theme.TrustBlue
import com.watchsafety.guardian.ui.theme.TrustBlueContainer
import com.watchsafety.guardian.ui.theme.WatchSafetyTheme

import kotlinx.coroutines.launch

import kotlin.math.roundToInt


@Composable
fun AddSafeZoneScreen(

    initialLatitude: Double,

    initialLongitude: Double,

    /*
     * 기존 HOME 안전구역 이름.
     *
     * null이면 아직 집이 지정되지 않은 상태.
     */
    existingHomeName: String?,

    onBack: () -> Unit,

    onSave: (
        name: String,
        radiusMeters: Int,
        latitude: Double,
        longitude: Double,
        isHome: Boolean,
    ) -> Unit,
) {

    var zoneName by
    rememberSaveable {
        mutableStateOf(
            "안전구역"
        )
    }


    /*
     * =====================================================
     * 집 지정
     * =====================================================
     */
    var isHome by
    rememberSaveable {
        mutableStateOf(
            false
        )
    }


    /*
     * =====================================================
     * 반경
     * =====================================================
     */
    var radius by
    rememberSaveable {
        mutableFloatStateOf(
            300f
        )
    }


    val previewZoomLevel =
        safeZonePreviewZoomLevel(
            radius
                .roundToInt()
        )


    var tMapView by
    remember {
        mutableStateOf<TMapView?>(
            null
        )
    }


    /*
     * =====================================================
     * 장소 / 주소 검색
     * =====================================================
     */

    var searchQuery by
    rememberSaveable {
        mutableStateOf(
            ""
        )
    }


    var searchResults by
    remember {
        mutableStateOf<
                List<TmapPoiSearchResult>
                >(
            emptyList()
        )
    }


    var isSearching by
    remember {
        mutableStateOf(
            false
        )
    }


    var searchError by
    remember {
        mutableStateOf<String?>(
            null
        )
    }


    var selectedSearchResult by
    remember {
        mutableStateOf<TmapPoiSearchResult?>(
            null
        )
    }


    val searchClient =
        remember {
            TmapPoiSearchClient()
        }


    val coroutineScope =
        rememberCoroutineScope()


    val keyboardController =
        LocalSoftwareKeyboardController
            .current


    val performSearch = {

        val keyword =
            searchQuery
                .trim()


        if (
            keyword.isNotBlank() &&
            !isSearching
        ) {

            coroutineScope.launch {

                isSearching =
                    true

                searchError =
                    null


                runCatching {

                    searchClient.search(
                        keyword
                    )

                }.onSuccess { results ->

                    searchResults =
                        results


                    if (
                        results.isEmpty()
                    ) {

                        searchError =
                            "검색 결과가 없습니다."
                    }

                }.onFailure { error ->

                    searchResults =
                        emptyList()


                    searchError =
                        when {

                            error.message
                                ?.contains(
                                    "401"
                                ) == true ||
                                    error.message
                                        ?.contains(
                                            "403"
                                        ) == true ->

                                "TMAP POI 검색 권한 또는 App Key 설정을 확인해주세요."


                            else ->

                                "장소 검색에 실패했습니다. 잠시 후 다시 시도해주세요."
                        }
                }


                isSearching =
                    false
            }
        }
    }


    Scaffold(

        topBar = {

            GuardianTopBar(

                title =
                    "안전구역 추가",

                onBack =
                    onBack,
            )
        },

        ) { innerPadding ->


        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        innerPadding
                    )
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .background
                    ),
        ) {


            /*
             * =================================================
             * 장소 / 주소 검색
             * =================================================
             */

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White
                        )
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 10.dp,
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    ),
            ) {

                OutlinedTextField(

                    value =
                        searchQuery,

                    onValueChange = { value ->

                        searchQuery =
                            value

                        selectedSearchResult =
                            null

                        searchError =
                            null
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    placeholder = {

                        Text(
                            text =
                                "장소·주소 검색 (예: 을지로입구역)"
                        )
                    },

                    leadingIcon = {

                        Icon(
                            imageVector =
                                Icons.Rounded.Search,

                            contentDescription =
                                null,

                            tint =
                                TrustBlue,
                        )
                    },

                    trailingIcon = {

                        if (
                            isSearching
                        ) {

                            CircularProgressIndicator(

                                modifier =
                                    Modifier.size(
                                        22.dp
                                    ),

                                strokeWidth =
                                    2.dp,
                            )

                        } else {

                            IconButton(

                                onClick = {

                                    keyboardController
                                        ?.hide()

                                    performSearch()
                                },

                                enabled =
                                    searchQuery
                                        .isNotBlank(),
                            ) {

                                Icon(

                                    imageVector =
                                        Icons.Rounded.Search,

                                    contentDescription =
                                        "검색",

                                    tint =
                                        TrustBlue,
                                )
                            }
                        }
                    },

                    singleLine =
                        true,

                    keyboardOptions =
                        KeyboardOptions(
                            imeAction =
                                ImeAction.Search
                        ),

                    keyboardActions =
                        KeyboardActions(

                            onSearch = {

                                keyboardController
                                    ?.hide()

                                performSearch()
                            }
                        ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                )


                if (
                    searchResults
                        .isNotEmpty()
                ) {

                    Card(

                        modifier =
                            Modifier
                                .fillMaxWidth(),

                        colors =
                            CardDefaults
                                .cardColors(
                                    containerColor =
                                        Color.White
                                ),

                        elevation =
                            CardDefaults
                                .cardElevation(
                                    defaultElevation =
                                        5.dp
                                ),

                        shape =
                            RoundedCornerShape(
                                14.dp
                            ),
                    ) {

                        Column {

                            searchResults
                                .forEachIndexed {
                                        index,
                                        result ->


                                    Column(

                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .clickable {

                                                    selectedSearchResult =
                                                        result

                                                    searchQuery =
                                                        result.name

                                                    searchResults =
                                                        emptyList()

                                                    searchError =
                                                        null

                                                    keyboardController
                                                        ?.hide()


                                                    tMapView
                                                        ?.setCenterPoint(
                                                            result.latitude,
                                                            result.longitude,
                                                        )


                                                    tMapView
                                                        ?.setZoomLevel(
                                                            previewZoomLevel
                                                        )
                                                }
                                                .padding(
                                                    horizontal =
                                                        14.dp,
                                                    vertical =
                                                        12.dp,
                                                ),
                                    ) {

                                        Text(

                                            text =
                                                result.name,

                                            fontWeight =
                                                FontWeight.SemiBold,

                                            style =
                                                MaterialTheme
                                                    .typography
                                                    .bodyMedium,
                                        )


                                        if (
                                            result.address
                                                .isNotBlank()
                                        ) {

                                            Spacer(
                                                modifier =
                                                    Modifier.height(
                                                        3.dp
                                                    )
                                            )


                                            Text(

                                                text =
                                                    result.address,

                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurfaceVariant,

                                                style =
                                                    MaterialTheme
                                                        .typography
                                                        .bodySmall,
                                            )
                                        }
                                    }


                                    if (
                                        index <
                                        searchResults.lastIndex
                                    ) {

                                        HorizontalDivider()
                                    }
                                }
                        }
                    }
                }


                if (
                    searchError != null
                ) {

                    Text(

                        text =
                            searchError.orEmpty(),

                        color =
                            MaterialTheme
                                .colorScheme
                                .error,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )
                }


                selectedSearchResult
                    ?.let { selected ->

                        Text(

                            text =
                                if (
                                    selected.address
                                        .isNotBlank()
                                ) {

                                    "선택 위치: ${selected.name} · ${selected.address}"

                                } else {

                                    "선택 위치: ${selected.name}"
                                },

                            color =
                                TrustBlue,

                            fontWeight =
                                FontWeight.Medium,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                    }


                Text(

                    text =
                        "검색 결과를 선택하거나 지도를 직접 움직여 중심 위치를 정하세요.",

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                )
            }


            /*
             * =================================================
             * TMAP
             * =================================================
             */

            Box(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(
                            300.dp
                        ),
            ) {

                TmapMapView(

                    latitude =
                        initialLatitude,

                    longitude =
                        initialLongitude,

                    zoomLevel =
                        previewZoomLevel,

                    showLocationMarker =
                        false,

                    followLocation =
                        false,

                    previewSafeZoneRadiusMeters =
                        radius
                            .roundToInt(),

                    onMapViewReady = { map ->

                        tMapView =
                            map
                    },

                    modifier =
                        Modifier
                            .fillMaxSize(),
                )


                Icon(

                    imageVector =
                        Icons.Rounded.LocationOn,

                    contentDescription =
                        "안전구역 중심 위치",

                    tint =
                        TrustBlue,

                    modifier =
                        Modifier
                            .align(
                                Alignment.Center
                            )
                            .offset(
                                y = (-22).dp
                            )
                            .size(
                                46.dp
                            ),
                )


                Text(

                    text =
                        "반경 ${
                            formatRadius(
                                radius.roundToInt()
                            )
                        }",

                    modifier =
                        Modifier
                            .align(
                                Alignment.BottomCenter
                            )
                            .padding(
                                bottom = 12.dp
                            )
                            .background(
                                Color.White.copy(
                                    alpha = 0.92f
                                ),
                                RoundedCornerShape(
                                    16.dp
                                )
                            )
                            .padding(
                                horizontal = 12.dp,
                                vertical = 7.dp,
                            ),

                    color =
                        TrustBlue,

                    fontWeight =
                        FontWeight.Bold,

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,
                )
            }


            /*
             * =================================================
             * 설정
             * =================================================
             */

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White,
                            RoundedCornerShape(
                                topStart = 26.dp,
                                topEnd = 26.dp,
                            )
                        )
                        .padding(
                            20.dp
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        14.dp
                    ),
            ) {

                Text(

                    text =
                        "구역 이름",

                    fontWeight =
                        FontWeight.SemiBold,

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium,
                )


                OutlinedTextField(

                    value =
                        zoneName,

                    onValueChange = {

                        zoneName =
                            it
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    leadingIcon = {

                        Icon(

                            imageVector =
                                Icons.Rounded.LocationOn,

                            contentDescription =
                                null,

                            tint =
                                TrustBlue,
                        )
                    },

                    singleLine =
                        true,

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                )


                /*
                 * =================================================
                 * 집으로 지정
                 * =================================================
                 */

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (
                                    isHome
                                ) {
                                    TrustBlueContainer
                                } else {
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant
                                        .copy(
                                            alpha = 0.45f
                                        )
                                },
                                RoundedCornerShape(
                                    16.dp
                                )
                            )
                            .clickable {

                                isHome =
                                    !isHome
                            }
                            .padding(
                                horizontal = 14.dp,
                                vertical = 12.dp,
                            ),

                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {

                    Box(

                        modifier =
                            Modifier
                                .size(
                                    40.dp
                                )
                                .background(
                                    Color.White,
                                    CircleShape
                                ),

                        contentAlignment =
                            Alignment.Center,
                    ) {

                        Icon(

                            imageVector =
                                Icons.Outlined.Home,

                            contentDescription =
                                null,

                            tint =
                                TrustBlue,

                            modifier =
                                Modifier.size(
                                    22.dp
                                ),
                        )
                    }


                    Column(

                        modifier =
                            Modifier
                                .weight(
                                    1f
                                )
                                .padding(
                                    start = 12.dp,
                                    end = 8.dp,
                                ),
                    ) {

                        Text(

                            text =
                                "집으로 지정",

                            fontWeight =
                                FontWeight.SemiBold,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodyLarge,
                        )


                        Text(

                            text =
                                "워치의 '집으로 가기' 목적지로 사용합니다.",

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,

                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                        )
                    }


                    Switch(

                        checked =
                            isHome,

                        onCheckedChange = {

                            isHome =
                                it
                        },

                        colors =
                            SwitchDefaults.colors(

                                checkedThumbColor =
                                    Color.White,

                                checkedTrackColor =
                                    TrustBlue,
                            ),
                    )
                }


                /*
                 * 이미 기존 HOME이 있는 경우
                 */
                if (
                    isHome &&
                    !existingHomeName
                        .isNullOrBlank()
                ) {

                    Text(

                        text =
                            "현재 '$existingHomeName'이(가) 집으로 지정되어 있습니다. " +
                                    "저장하면 기존 집 지정은 자동으로 해제됩니다.",

                        color =
                            TrustBlue,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                    )
                }


                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                ) {

                    Text(

                        text =
                            "반경",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium,
                    )


                    Text(

                        text =
                            formatRadius(
                                radius.roundToInt()
                            ),

                        color =
                            TrustBlue,

                        fontWeight =
                            FontWeight.Bold,
                    )
                }


                Slider(

                    value =
                        radius,

                    onValueChange = {

                        radius =
                            it
                    },

                    valueRange =
                        100f..1000f,

                    steps =
                        8,
                )


                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        ),
                ) {

                    listOf(
                        100,
                        300,
                        500,
                        1000,
                    ).forEach { preset ->

                        RadiusPresetButton(

                            radius =
                                preset,

                            selected =
                                radius.roundToInt() ==
                                        preset,

                            onClick = {

                                radius =
                                    preset.toFloat()
                            },

                            modifier =
                                Modifier.weight(
                                    1f
                                ),
                        )
                    }
                }


                Button(

                    onClick = {

                        val center =
                            tMapView
                                ?.getCenterPoint()


                        val selectedLatitude =
                            center
                                ?.getLatitude()
                                ?: initialLatitude


                        val selectedLongitude =
                            center
                                ?.getLongitude()
                                ?: initialLongitude


                        onSave(

                            zoneName.trim(),

                            radius.roundToInt(),

                            selectedLatitude,

                            selectedLongitude,

                            isHome,
                        )
                    },

                    modifier =
                        Modifier
                            .fillMaxWidth(),

                    enabled =
                        zoneName
                            .isNotBlank(),

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    TrustBlue
                            ),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),
                ) {

                    Text(

                        text =
                            "안전구역 저장하기",

                        modifier =
                            Modifier
                                .padding(
                                    vertical = 7.dp
                                ),

                        style =
                            MaterialTheme
                                .typography
                                .labelLarge,
                    )
                }
            }
        }
    }
}


@Composable
private fun RadiusPresetButton(

    radius: Int,

    selected: Boolean,

    onClick: () -> Unit,

    modifier: Modifier = Modifier,
) {

    val label =
        formatRadius(
            radius
        )


    if (
        selected
    ) {

        Button(

            onClick =
                onClick,

            modifier =
                modifier,

            contentPadding =
                PaddingValues(
                    horizontal = 4.dp
                ),

            colors =
                ButtonDefaults
                    .buttonColors(
                        containerColor =
                            TrustBlue
                    ),
        ) {

            Text(
                text =
                    label
            )
        }

    } else {

        OutlinedButton(

            onClick =
                onClick,

            modifier =
                modifier,

            contentPadding =
                PaddingValues(
                    horizontal = 4.dp
                ),
        ) {

            Text(
                text =
                    label
            )
        }
    }
}


private fun safeZonePreviewZoomLevel(
    radiusMeters: Int,
): Int =

    when {

        radiusMeters <= 300 ->
            16

        radiusMeters <= 500 ->
            15

        else ->
            14
    }


private fun formatRadius(
    radiusMeters: Int,
): String =

    if (
        radiusMeters >= 1000
    ) {

        "${radiusMeters / 1000}km"

    } else {

        "${radiusMeters}m"
    }


@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
)
@Composable
private fun AddSafeZonePreview() {

    WatchSafetyTheme {

        AddSafeZoneScreen(

            initialLatitude =
                37.5665,

            initialLongitude =
                126.9780,

            existingHomeName =
                "우리집",

            onBack =
                {},

            onSave = {
                    _,
                    _,
                    _,
                    _,
                    _ ->

            },
        )
    }
}