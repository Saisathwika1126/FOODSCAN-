package com.example.foodscan.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.foodscan.api.OpenFoodFactsApi
import com.example.foodscan.data.models.Product
import com.example.foodscan.data.models.NutritionFacts
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class ProductRepository(private val context: Context) {

    private val TAG = "ProductRepo"

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("User-Agent", "FOODSCAN-Android/1.0")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://world.openfoodfacts.org/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(OpenFoodFactsApi::class.java)

    suspend fun getProductByBarcode(barcode: String): Result<Product> {
        Log.d(TAG, "🔍 Fetching barcode: $barcode")

        if (!isNetworkAvailable()) {
            return Result.failure(Exception("No internet connection"))
        }

        return try {
            val response = api.getProduct(barcode)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.status == 1 && body.product != null) {
                    Log.d(TAG, "✅ Product found!")
                    Result.success(body.product.toProduct(barcode))
                } else {
                    Log.w(TAG, "❌ Product not found. Status: ${body?.status}")
                    val minimalProduct = Product(
                        barcode = barcode,
                        productName = "Product Not Found",
                        brand = "Unknown",
                        ingredients = emptyList(),
                        allergens = emptyList(),
                        imageUrl = "",
                        nutritionFacts = NutritionFacts(),
                        quantity = ""
                    )
                    Result.failure(ProductNotFoundException("Product not found", minimalProduct))
                }
            } else {
                Log.e(TAG, "❌ API error: ${response.code()}")
                Result.failure(Exception("API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception: ${e.message}")
            Result.failure(e)
        }
    }
}
