package com.example.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isInitialized = false

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentParagraphIndex = MutableStateFlow(0)
    val currentParagraphIndex: StateFlow<Int> = _currentParagraphIndex.asStateFlow()

    private val _speed = MutableStateFlow(1.0f)
    val speed: StateFlow<Float> = _speed.asStateFlow()

    private val _pitch = MutableStateFlow(1.0f)
    val pitch: StateFlow<Float> = _pitch.asStateFlow()

    private var currentParagraphs: List<String> = emptyList()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            isInitialized = true
            setupProgressListener()
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isPlaying.value = true
                val index = utteranceId?.toIntOrNull() ?: return
                _currentParagraphIndex.value = index
            }

            override fun onDone(utteranceId: String?) {
                val index = utteranceId?.toIntOrNull() ?: return
                val nextIndex = index + 1
                if (nextIndex < currentParagraphs.size && _isPlaying.value) {
                    speakParagraph(nextIndex)
                } else {
                    _isPlaying.value = false
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isPlaying.value = false
            }
        })
    }

    fun startReading(paragraphs: List<String>, startIndex: Int = 0) {
        if (!isInitialized || paragraphs.isEmpty()) return
        currentParagraphs = paragraphs
        _isPlaying.value = true
        speakParagraph(startIndex.coerceIn(0, paragraphs.lastIndex))
    }

    private fun speakParagraph(index: Int) {
        if (index !in currentParagraphs.indices) {
            _isPlaying.value = false
            return
        }
        _currentParagraphIndex.value = index
        val text = currentParagraphs[index]
        tts?.setSpeechRate(_speed.value)
        tts?.setPitch(_pitch.value)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, index.toString())
    }

    fun pause() {
        tts?.stop()
        _isPlaying.value = false
    }

    fun resume() {
        if (currentParagraphs.isNotEmpty()) {
            _isPlaying.value = true
            speakParagraph(_currentParagraphIndex.value)
        }
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentParagraphIndex.value = 0
    }

    fun next() {
        val next = _currentParagraphIndex.value + 1
        if (next < currentParagraphs.size) {
            speakParagraph(next)
        }
    }

    fun previous() {
        val prev = (_currentParagraphIndex.value - 1).coerceAtLeast(0)
        speakParagraph(prev)
    }

    fun setSpeed(rate: Float) {
        _speed.value = rate.coerceIn(0.5f, 2.5f)
        tts?.setSpeechRate(_speed.value)
    }

    fun setPitch(pitch: Float) {
        _pitch.value = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(_pitch.value)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
