package com.example.watchsafety.navigation

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.util.Log

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import java.util.Locale


class NavigationVoiceManager(
    context: Context
) : TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? =
        null


    private val _isReady =
        MutableStateFlow(
            false
        )


    val isReady: StateFlow<Boolean> =
        _isReady.asStateFlow()


    init {

        textToSpeech =
            TextToSpeech(
                context.applicationContext,
                this
            )
    }


    override fun onInit(
        status: Int
    ) {

        if (
            status !=
            TextToSpeech.SUCCESS
        ) {

            Log.e(
                TAG,
                "TTS 초기화 실패 status=$status"
            )

            return
        }


        val tts =
            textToSpeech
                ?: return


        val languageResult =
            tts.setLanguage(
                Locale.KOREAN
            )


        if (
            languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {

            Log.e(
                TAG,
                "한국어 TTS 사용 불가 result=$languageResult"
            )

            return
        }


        /*
         * 고령 사용자에게 너무 빠르지 않도록 약간 느리게.
         */
        tts.setSpeechRate(
            0.85f
        )


        tts.setPitch(
            1.0f
        )


        tts.setAudioAttributes(
            AudioAttributes
                .Builder()
                .setUsage(
                    AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SPEECH
                )
                .build()
        )


        _isReady.value =
            true


        Log.d(
            TAG,
            "한국어 TTS 준비 완료"
        )
    }


    fun speak(
        message: String
    ) {

        if (
            !_isReady.value
        ) {

            Log.w(
                TAG,
                "TTS 준비 전 speak 요청: $message"
            )

            return
        }


        Log.d(
            TAG,
            "음성 안내: $message"
        )


        textToSpeech?.speak(
            message,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "navigation_${System.currentTimeMillis()}"
        )
    }


    fun stopSpeaking() {

        textToSpeech?.stop()
    }


    fun shutdown() {

        _isReady.value =
            false


        textToSpeech?.stop()
        textToSpeech?.shutdown()


        textToSpeech =
            null


        Log.d(
            TAG,
            "TTS 종료"
        )
    }


    companion object {

        private const val TAG =
            "NavigationVoice"
    }
}
