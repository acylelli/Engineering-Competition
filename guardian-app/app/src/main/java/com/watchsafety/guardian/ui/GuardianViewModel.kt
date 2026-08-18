package com.watchsafety.guardian.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.watchsafety.guardian.data.GuardianRepository
import com.watchsafety.guardian.data.GuardianRepositoryProvider
import com.watchsafety.guardian.domain.model.GuardianSnapshot
import com.watchsafety.guardian.domain.model.NotificationSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


data class GuardianUiState(
    val snapshot: GuardianSnapshot,
    val isRefreshing: Boolean = false,
)


data class PairingUiState(

    val isLoading: Boolean =
        false,

    val isSuccess: Boolean =
        false,

    val errorMessage: String? =
        null,
)


class GuardianViewModel(
    private val repository: GuardianRepository,
) : ViewModel() {


    private val isRefreshing =
        MutableStateFlow(
            false
        )


    /*
     * -----------------------------------------------------
     * 워치 페어링 상태
     * -----------------------------------------------------
     */

    private val _pairingUiState =
        MutableStateFlow(
            PairingUiState()
        )

    val pairingUiState:
            StateFlow<PairingUiState> =
        _pairingUiState


    val uiState:
            StateFlow<GuardianUiState> =
        combine(

            repository.snapshot,

            isRefreshing,

            ) { snapshot, refreshing ->

            GuardianUiState(
                snapshot = snapshot,
                isRefreshing = refreshing,
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


    fun sendReturnHomeRequest() {

        launchRepositoryAction {

            repository
                .sendReturnHomeRequest()
        }
    }


    fun setSafeZoneEnabled(
        zoneId: String,
        enabled: Boolean,
    ) {

        launchRepositoryAction {

            repository
                .setSafeZoneEnabled(
                    zoneId,
                    enabled,
                )
        }
    }


    fun addSafeZone(
        name: String,
        radiusMeters: Int,
    ) {

        launchRepositoryAction {

            repository
                .addSafeZone(
                    name,
                    radiusMeters,
                )
        }
    }


    fun updateNotificationSettings(
        settings: NotificationSettings,
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
     * 워치 6자리 코드 연결
     * =====================================================
     */

    fun redeemPairingCode(
        code: String,
    ) {

        /*
         * 연속 클릭 방지
         */
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

            /*
             * 로딩 시작
             */
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

                /*
                 * DB에서는
                 * 잘못된 코드 / 만료 코드도
                 * 예외로 반환됨.
                 */
                val message =
                    when {

                        error.message
                            ?.contains(
                                "expired",
                                ignoreCase = true
                            ) == true ->

                            "코드가 만료되었거나 올바르지 않습니다."


                        error.message
                            ?.contains(
                                "already paired",
                                ignoreCase = true
                            ) == true ->

                            "이미 다른 보호자와 연결된 워치입니다."


                        else ->

                            "워치 연결에 실패했습니다.\n코드를 다시 확인해주세요."
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
     * 연결 화면을 다시 열 때
     * 이전 성공/오류 상태 제거
     */
    fun resetPairingState() {

        _pairingUiState.value =
            PairingUiState()
    }


    private fun launchRepositoryAction(
        action: suspend () -> Unit,
    ) {

        viewModelScope.launch {

            runCatching {

                action()
            }
        }
    }


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
                    modelClass: Class<T>,
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