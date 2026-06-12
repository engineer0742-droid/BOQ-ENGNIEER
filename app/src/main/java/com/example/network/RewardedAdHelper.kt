package com.example.network

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * High-Performance AdMob Rewarded Ad Helper Class.
 * Features automated pre-loading, error resilience, and secure callbacks.
 */
class RewardedAdHelper(
    private val context: Context,
    private val adUnitId: String = "ca-app-pub-7461854901165079/2039054137",
    private val onRewardEarned: (Int) -> Unit,
    private val onAdFailedToLoad: (String) -> Unit = {},
    private val onAdFailedToShow: (String) -> Unit = {},
    private val onAdLoaded: () -> Unit = {}
) {
    private var rewardedAd: RewardedAd? = null
    private var isAdLoading = false

    companion object {
        private const val TAG = "RewardedAdHelper"
    }

    init {
        loadAd()
    }

    fun isAdLoaded(): Boolean {
        return rewardedAd != null
    }

    fun loadAd() {
        if (isAdLoading || rewardedAd != null) {
            Log.d(TAG, "Ad load skipped: already loading/loaded.")
            return
        }
        isAdLoading = true
        Log.d(TAG, "Starting background pre-loading for AdUnitId: $adUnitId")
        
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "Ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    isAdLoading = false
                    onAdFailedToLoad(loadAdError.message)
                }

                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Ad loaded successfully and cached.")
                    rewardedAd = ad
                    isAdLoading = false
                    onAdLoaded()
                    
                    // Setup FullScreenContentCallback for lifecycle events
                    rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdClicked() {
                            Log.d(TAG, "Ad clicked.")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Ad dismissed by user.")
                            rewardedAd = null
                            // Automate background pre-loading of next ad instantly
                            loadAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            Log.e(TAG, "Ad failed to show: ${adError.message}")
                            rewardedAd = null
                            onAdFailedToShow(adError.message)
                            // Automate background pre-loading to retry
                            loadAd()
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "Ad impression logged.")
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Ad is now showing full screen.")
                        }
                    }
                }
            }
        )
    }

    fun showAd(activity: Activity) {
        val ad = rewardedAd
        if (ad != null) {
            Log.d(TAG, "Showing rewarded ad...")
            ad.show(activity, OnUserEarnedRewardListener { rewardItem ->
                Log.d(TAG, "User completed watching the ad! Reward earned: 2 credits.")
                // Exact +2 credits as requested by the user
                onRewardEarned(2)
            })
        } else {
            Log.w(TAG, "Show request failed: Ad not initialized or cached yet. Pre-loading now.")
            loadAd()
            // Provide descriptive fallback error so UI can display message or alternative action
            onAdFailedToShow("جاري تحضير وتحميل حركة الإعلان في الخلفية... يرجى المحاولة بعد قليل أو تفعيل الإعلانات التجريبية.")
        }
    }
}
