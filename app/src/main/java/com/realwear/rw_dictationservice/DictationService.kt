package com.realwear.rw_dictationservice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.util.Log
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

class DictationService : RecognitionService() {
    private lateinit var mSpeechRecognizer: SpeechRecognizer
    private lateinit var mCallback: Callback
    private val s_executorService = Executors.newCachedThreadPool()

    override fun onStartListening(intent: Intent, callback: Callback) {
        Log.d(TAG, "jonathan start listening")

        mCallback = callback
        val extras = intent.extras ?: Bundle()
        val dictationType = extras.get(EXTRA_DICTATION_TYPE)
        mCallback.readyForSpeech(extras)

        when (dictationType) {
            ASYNC_DICTATION -> startAsyncDictation()
            CONTINUOUS_DICTATION -> startContinuousDictation()
            else -> startAsyncDictation()
        }
    }

    /**
     * Method to start Microsoft dictation service.
     */
    private fun startAsyncDictation() {
        val speechConfig =
            SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion)
        val audioInput = AudioConfig.fromDefaultMicrophoneInput()
        mSpeechRecognizer = SpeechRecognizer(speechConfig, audioInput)
        val task: Future<SpeechRecognitionResult> = mSpeechRecognizer.recognizeOnceAsync()

        mSpeechRecognizer.recognizing.addEventListener { _: Any?, speechRecognitionResultEventArgs: SpeechRecognitionEventArgs ->
            val text = speechRecognitionResultEventArgs.result.text
            val bundle = Bundle()
            val arrayList = ArrayList<String>()
            arrayList.add(text)
            bundle.putStringArrayList(
                android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
                arrayList
            )
            mCallback.partialResults(bundle)
        }

        setOnTaskCompletedListener(task,
            object : OnTaskCompletedListener<SpeechRecognitionResult> {
                override fun onCompleted(taskResult: SpeechRecognitionResult) {
                    mCallback.endOfSpeech()

                    val text = taskResult.text
                    val bundle = Bundle()
                    val arrayList = ArrayList<String>()
                    arrayList.add(text)
                    bundle.putStringArrayList(
                        android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
                        arrayList
                    )
                    mCallback.results(bundle)
                }
            })
    }

    private fun startContinuousDictation() {
        val speechConfig =
            SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion)
        val audioInput = AudioConfig.fromDefaultMicrophoneInput()
        mSpeechRecognizer = SpeechRecognizer(speechConfig, audioInput)
        val content = java.util.ArrayList<String>()
        mSpeechRecognizer.recognizing.addEventListener { _, speechRecognitionEventArgs ->
            val text: String = speechRecognitionEventArgs.result.text
            content.add(text)

            val bundle = Bundle()
            bundle.putStringArrayList(
                android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
                content
            )
            mCallback.partialResults(bundle)

            content.removeAt(content.size - 1)
        }

        mSpeechRecognizer.recognized.addEventListener { _, speechRecognitionEventArgs ->
            val text: String = speechRecognitionEventArgs.result.text
            content.add(text)

            val bundle = Bundle()
            bundle.putStringArrayList(
                android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
                content
            )
            mCallback.results(bundle)
        }

        mSpeechRecognizer.startContinuousRecognitionAsync()
    }

    override fun onStopListening(p0: Callback?) {
        Log.d(TAG, "onStopListening called.")
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy called.")
        mSpeechRecognizer.stopContinuousRecognitionAsync()
        mSpeechRecognizer.close()

        super.onDestroy()
    }

    override fun onCancel(p0: Callback?) {
        Log.d(TAG, "onCancel called.")
    }

    private fun <T> setOnTaskCompletedListener(
        task: Future<T>,
        listener: OnTaskCompletedListener<T>
    ) {
        s_executorService.submit(
            Callable<Any?> {
                val result = task.get()
                listener.onCompleted(result)
                null
            })
    }

    private interface OnTaskCompletedListener<T> {
        fun onCompleted(taskResult: T)
    }

    companion object {
        private val TAG = DictationService::class.java.simpleName
        private const val SpeechSubscriptionKey = "226314cc4670432e87d9f80f805bffea"
        private const val SpeechRegion = "westus2"
        private const val EXTRA_DICTATION_TYPE = "extra_dictation_type"
        private const val ASYNC_DICTATION = 0
        private const val CONTINUOUS_DICTATION = 1
    }
}
