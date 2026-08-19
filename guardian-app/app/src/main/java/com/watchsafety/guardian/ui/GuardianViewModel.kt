package com.watchsafety.guardian.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

import com.watchsafety.guardian.data.GuardianRepository
import com.watchsafety.guardian.data.GuardianRepositoryProvider

import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.NotificationSettings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.launch


/*
 * =========================================================
 * 메인 UI 상태
 * =========================================================
 */

data class GuardianUiState(

    val snapshot:
    GuardianSnapshot,

    val isRefreshing:
    Boolean = false,
)


/*
 * =========================================================
 * 페어링 상태
 * =========================================================
 */

data class PairingUiState(

    val isLoading:
    Boolean = false,

    val isSuccess:
    Boolean = false,

    val errorMessage:
    String? = null,
)


/*
 * =========================================================
 * GuardianViewModel
 * =========================================================
 */

class GuardianViewModel(

    private val repository:
    GuardianRepository,

    ) : ViewModel() {


    /*
     * =====================================================
     * 새로고침
     * =====================================================
     */

    private val isRefreshing =
        MutableStateFlow(
            false
        )


    /*
     * =====================================================
     * 페어링
     * =====================================================
     */

    private val _pairingUiState =
        MutableStateFlow(
            PairingUiState()
        )


    val pairingUiState:
            StateFlow<PairingUiState> =
        _pairingUiState


    /*
     * =====================================================
     * 새 SOS
     * =====================================================
     *
     * 이전처럼 ViewModel에서 Snapshot을 비교하지 않는다.
     *
     * SupabaseGuardianRepository에서
     * 실제 새로운 SOS를 감지한 순간
     * 이벤트가 전달된다.
     */

    val newSosEvent:
            Flow<Unit> =
        repository
            .newSosEvent


    /*
     * =====================================================
     * 메인 상태
     * =====================================================
     */

    val uiState:
            StateFlow<GuardianUiState> =

        combine(

            repository.snapshot,

            isRefreshing,

            ) { snapshot, refreshing ->


            GuardianUiState(

                snapshot =
                    snapshot,

                isRefreshing =
                    refreshing,
            )

        }.stateIn(

            scope =
                viewModelScope,

            started =
                SharingStarted
                    .WhileSubscribed(
                        5_000
                    ),

            initialValue =
                GuardianUiState(

                    repository
                        .snapshot
                        .value
                ),
        )


    /*
     * =====================================================
     * 상태 새로고침
     * =====================================================
     */

    fun refreshStatus() {

        viewModelScope.launch {

            isRefreshing.value =
                true


            try {

                runCatching {

                    repository
                        .refreshStatus()
                }

            } finally {

                isRefreshing.value =
                    false
            }
        }
    }


    /*
     * =====================================================
     * 귀가 요청
     * =====================================================
     */

    fun sendReturnHomeRequest() {

        launchRepositoryAction {

            repository
                .sendReturnHomeRequest()
        }
    }


    /*
     * =====================================================
     * 안전구역 ON/OFF
     * =====================================================
     */

    fun setSafeZoneEnabled(

        zoneId:
        String,

        enabled:
        Boolean,

        ) {

        launchRepositoryAction {

            repository
                .setSafeZoneEnabled(
                    zoneId,
                    enabled,
                )
        }
    }


    /*
     * =====================================================
     * 안전구역 추가
     * =====================================================
     */

    fun addSafeZone(

        name:
        String,

        radiusMeters:
        Int,

        ) {

        launchRepositoryAction {

            repository
                .addSafeZone(
                    name,
                    radiusMeters,
                )
        }
    }


    /*
     * =====================================================
     * 알림 설정
     * =====================================================
     */

    fun updateNotificationSettings(
        settings:
        NotificationSettings,
    ) {

        launchRepositoryAction {

            repository
                .updateNotificationSettings(
                    settings
                )
        }
    }


    /*
     * =====================================================
     * 워치 연결
     * =====================================================
     */

    fun redeemPairingCode(
        code:
        String,
    ) {


        if (
            _pairingUiState
                .value
                .isLoading
        ) {

            return
        }


        val normalizedCode =

            code.filter {

                it.isDigit()
            }


        if (
            normalizedCode.length != 6
        ) {

            _pairingUiState.value =

                PairingUiState(

                    errorMessage =
                        "6자리 연결 코드를 입력해주세요."
                )


            return
        }


        viewModelScope.launch {


            _pairingUiState.value =

                PairingUiState(

                    isLoading =
                        true
                )


            runCatching {

                repository
                    .redeemPairingCode(
                        normalizedCode
                    )


            }.onSuccess {


                _pairingUiState.value =

                    PairingUiState(

                        isSuccess =
                            true
                    )


            }.onFailure { error ->


                val message =

                    when {


                        error.message
                            ?.contains(
                                "expired",
                                ignoreCase =
                                    true
                            ) == true -> {


                            "코드가 만료되었거나 올바르지 않습니다."
                        }


                        error.message
                            ?.contains(
                                "already paired",
                                ignoreCase =
                                    true
                            ) == true -> {


                            "이미 다른 보호자와 연결된 워치입니다."
                        }


                        else -> {


                            "워치 연결에 실패했습니다.\n코드를 다시 확인해주세요."
                        }
                    }


                _pairingUiState.value =

                    PairingUiState(

                        errorMessage =
                            message
                    )
            }
        }
    }


    /*
     * =====================================================
     * 페어링 상태 초기화
     * =====================================================
     */

    fun resetPairingState() {

        _pairingUiState.value =
            PairingUiState()
    }


    /*
     * =====================================================
     * Repository 공통 실행
     * =====================================================
     */

    private fun launchRepositoryAction(
        action:
        suspend () -> Unit,
    ) {

        viewModelScope.launch {

            runCatching {

                action()
            }
        }
    }


    /*
     * =====================================================
     * Factory
     * =====================================================
     */

    companion object {

        fun factory():
                ViewModelProvider.Factory =

            object :
                ViewModelProvider.Factory {


                @Suppress(
                    "UNCHECKED_CAST"
                )

                override fun <T : ViewModel>
                        create(
                    modelClass:
                    Class<T>,
                ): T {


                    require(

                        modelClass
                            .isAssignableFrom(

                                GuardianViewModel::
                                class.java
                            )
                    )


                    return GuardianViewModel(

                        GuardianRepositoryProvider
                            .create()

                    ) as T
                }
            }
    }
}