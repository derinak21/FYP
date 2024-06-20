package com.example.breathein.network

import android.content.Context
import com.example.breathein.R
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class RetrofitClient {

    companion object {
        private const val BASE_URL = "https://18.169.68.55:8443/"

        private var context: Context? = null

        private val trustManager: X509TrustManager by lazy {
            checkNotNull(context) { "Context must be set before accessing trustManager" }
            createTrustManager(context!!)
        }

        private val sslContext: SSLContext by lazy {
            SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(trustManager), null)
            }
        }

        private val loggingInterceptor: HttpLoggingInterceptor by lazy {
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY // Set the desired log level
            }
        }


        private val okHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .addInterceptor(loggingInterceptor) // Add your interceptors as needed
                .build()
        }

        private val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
        }

        fun <T> createService(serviceClass: Class<T>): T {
            return retrofit.create(serviceClass)
        }

        fun setContext(appContext: Context) {
            context = appContext
        }

        private fun createTrustManager(context: Context): X509TrustManager {
            val certificateInputStream: InputStream = context.resources.openRawResource(R.raw.server_certificate)

            val certificate = CertificateFactory.getInstance("X.509").generateCertificate(certificateInputStream)

            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(null, null)
            keyStore.setCertificateEntry("server", certificate)

            val trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            trustManagerFactory.init(keyStore)

            val trustManagers = trustManagerFactory.trustManagers
            return trustManagers[0] as X509TrustManager
        }
    }
}
