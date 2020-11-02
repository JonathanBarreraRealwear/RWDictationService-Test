package com.realwear.rw_dictationservice

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService
import android.util.Log
import com.azure.identity.ClientSecretCredentialBuilder
import com.azure.security.keyvault.secrets.SecretClientBuilder
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Custom subclass of RecognitionService to handle dictation.
 */
class DictationService : RecognitionService() {
    private val sExecutorService = Executors.newCachedThreadPool()
    private lateinit var subscriptionKey: String

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "jonathan on create")

        val keyvaultUrl = "https://clouddictation-keyvault.vault.azure.net/"
        val clientSecretCredential = ClientSecretCredentialBuilder()
            .clientId("99ea6d24-d7f2-4e65-8cf1-6f6949d5aed1")
            .tenantId("3fb8c4c4-8d65-442d-8093-6d8790fc6b85")
            .clientSecret("jVf2~AZVCzoz-.1j_n5Lc_bJT89~sqkSAK")
            .build()
        val secretClient = SecretClientBuilder()
            .vaultUrl(keyvaultUrl)
            .credential(clientSecretCredential)
            .buildClient()

        subscriptionKey = secretClient.getSecret("DictationKey").value
    }

    override fun onStartListening(intent: Intent, callback: Callback) {
        val speechConfig =
            SpeechConfig.fromSubscription(subscriptionKey, SpeechRegion)
        val audioInput = AudioConfig.fromDefaultMicrophoneInput()
        val speechRecognizer = SpeechRecognizer(speechConfig, audioInput)
        val task: Future<SpeechRecognitionResult> = speechRecognizer.recognizeOnceAsync()
        callback.readyForSpeech(Bundle())

        speechRecognizer.recognizing.addEventListener { _: Any?, speechRecognitionResultEventArgs: SpeechRecognitionEventArgs ->
            val text = speechRecognitionResultEventArgs.result.text
            val bundle = bundleResults(text)
            callback.partialResults(bundle)
        }

        setOnTaskCompletedListener(task,
            object : OnTaskCompletedListener<SpeechRecognitionResult> {
                override fun onCompleted(taskResult: SpeechRecognitionResult) {
                    callback.endOfSpeech()
                    val text = taskResult.text
                    val bundle = bundleResults(text)
                    callback.results(bundle)
                }
            })
    }

    /**
     * Method for bundling dictation results [text].
     * @return Bundle with text results in string array list.
     */
    private fun bundleResults(text: String): Bundle {
        val bundle = Bundle()
        val arrayList = ArrayList<String>()
        arrayList.add(text)
        bundle.putStringArrayList(
            android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
            arrayList
        )
        return bundle
    }

    override fun onStopListening(p0: Callback?) {
        Log.d(TAG, "onStopListening called.")
    }

    override fun onCancel(p0: Callback?) {
        Log.d(TAG, "onCancel called.")
    }

    /**
     * Method to set an OnTaskCompletedListener on a Future task.
     */
    private fun <T> setOnTaskCompletedListener(
        task: Future<T>,
        listener: OnTaskCompletedListener<T>
    ) {
        sExecutorService.submit(
            Callable<Any?> {
                val result = task.get()
                listener.onCompleted(result)
                null
            })
    }

    /**
     * A custom listener for task result.
     */
    private interface OnTaskCompletedListener<T> {
        fun onCompleted(taskResult: T)
    }

    companion object {
        private val TAG = DictationService::class.java.simpleName
        private const val SpeechRegion = "westus2"
    }
}
