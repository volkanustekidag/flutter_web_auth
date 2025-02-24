package com.linusu.flutter_web_auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class FlutterWebAuthPlugin: FlutterPlugin, MethodCallHandler {

    private var context: Context? = null
    private var channel: MethodChannel? = null

    companion object {
        val callbacks = mutableMapOf<String, Result>()
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        // FlutterPluginBinding üzerinden context ve messenger'ı alıyoruz
        context = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "flutter_web_auth")
        channel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        // Detach olduğunda kaynakları temizliyoruz
        context = null
        channel = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "authenticate" -> {
                val url = Uri.parse(call.argument<String>("url"))
                val callbackUrlScheme = call.argument<String>("callbackUrlScheme")!!
                val preferEphemeral = call.argument<Boolean>("preferEphemeral")!!

                callbacks[callbackUrlScheme] = result

                val intent = CustomTabsIntent.Builder().build()
                val keepAliveIntent = Intent(context, KeepAliveService::class.java)

                intent.intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                if (preferEphemeral) {
                    intent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                }
                intent.intent.putExtra("android.support.customtabs.extra.KEEP_ALIVE", keepAliveIntent)

                intent.launchUrl(context!!, url)
            }
            "cleanUpDanglingCalls" -> {
                callbacks.forEach { (_, danglingResultCallback) ->
                    danglingResultCallback.error("CANCELED", "User canceled login", null)
                }
                callbacks.clear()
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }
}
