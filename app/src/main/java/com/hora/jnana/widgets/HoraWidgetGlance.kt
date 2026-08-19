package com.hora.jnana.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.hora.jnana.CacheManager
import com.hora.jnana.DataStoreManager
import com.hora.jnana.api.HoraApiService
import com.hora.jnana.data.AuthRepository
import com.hora.jnana.repository.HoraRepository
import com.hora.jnana.utils.TranslationUtils
import com.hora.jnana.workers.HoraUpdateWorker
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first

class RefreshActionCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val workRequest = OneTimeWorkRequestBuilder<HoraUpdateWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "HoraUpdateManual",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
}

class HoraWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cache = CacheManager(context)
        val dataStore = DataStoreManager(context)
        val authRepository = AuthRepository(context)
        val lang = dataStore.langFlow.first()
        val apiBase = dataStore.apiBaseFlow.first()
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        
        val api = HoraApiService.create(
            authRepository = authRepository,
            onSessionExpired = { authRepository.notifySessionExpired() },
            moshi = moshi,
            baseUrl = apiBase
        )
        val repo = HoraRepository(api, context, moshi)
        
        val hJson = cache.readJson("hora.json")
        val pJson = cache.readJson("panchanga.json")
        val mJson = cache.readJson("muhurta.json")
        val dJson = cache.readJson("day.json")
        
        val state = repo.mergeToState(pJson, mJson, dJson, hJson)

        provideContent {
            GlanceTheme {
                HoraWidgetContent(state, lang)
            }
        }
    }

    @Composable
    private fun HoraWidgetContent(state: com.hora.jnana.ui.screens.PanchangaState, lang: String) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.surface)
                .padding(8.dp)
                .clickable(actionRunCallback<RefreshActionCallback>())
        ) {
            // Header
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.horaSymbol} ${state.hora}",
                    style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onSurface)
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = state.remaining,
                    style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant)
                )
            }

            Spacer(modifier = GlanceModifier.height(2.dp))

            // Sub-header
            val endsLabel = TranslationUtils.translate("Ends", lang)
            val nextLabel = TranslationUtils.translate("Next", lang)
            Text(
                text = "$endsLabel ${state.horaEnds}   •   $nextLabel ${state.horaNext}",
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant)
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Grid using Rows
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    WidgetInfoItem(TranslationUtils.translate("Ayana", lang), state.ayana, GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    WidgetInfoItem(TranslationUtils.translate("Tithi", lang), state.tithi, GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    WidgetInfoItem(TranslationUtils.translate("Rahu Kalam", lang), state.rahuKalam, GlanceModifier.defaultWeight())
                }
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    WidgetInfoItem(TranslationUtils.translate("Rutu", lang), state.rutu, GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    WidgetInfoItem(TranslationUtils.translate("Nakshatra", lang), state.nakshatra, GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    WidgetInfoItem(TranslationUtils.translate("Yamaganda", lang), state.yamaganda, GlanceModifier.defaultWeight())
                }
                
                Spacer(modifier = GlanceModifier.height(4.dp))
                
                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    WidgetInfoItem(TranslationUtils.translate("Masa", lang), state.masa, GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    WidgetInfoItem(TranslationUtils.translate("Sunrise", lang), "${state.sunrise} - ${state.sunset}", GlanceModifier.defaultWeight())
                    Spacer(modifier = GlanceModifier.width(4.dp))
                    WidgetInfoItem(TranslationUtils.translate("Abhijit", lang), state.abhijit, GlanceModifier.defaultWeight())
                }
            }

            Spacer(modifier = GlanceModifier.defaultWeight())

            // Footer
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🌙 ${state.moonRasi}",
                    style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text(
                    text = "☀️ ${state.sunRasi}",
                    style = TextStyle(fontSize = 11.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun WidgetInfoItem(label: String, value: String, modifier: GlanceModifier = GlanceModifier) {
        Column(modifier = modifier) {
            Text(
                text = label.uppercase(), 
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold, color = GlanceTheme.colors.secondary),
                maxLines = 1
            )
            Text(
                text = value, 
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurface),
                maxLines = 1
            )
        }
    }
}

class HoraWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HoraWidget()
}
