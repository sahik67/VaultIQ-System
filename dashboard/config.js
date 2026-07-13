/**
 * VAULTIQ Dashboard Configuration
 * Centralized configuration file - update this to change app-wide settings
 * 
 * IMPORTANT: Set these as environment variables before running the dashboard
 * Example: 
 *   export SUPABASE_URL="your_supabase_url"
 *   export SUPABASE_ANON_KEY="your_supabase_anon_key"
 *   export SUPABASE_PUBLISHABLE_KEY="your_supabase_publishable_key"
 *   export CLOUDINARY_CLOUD_NAME="your_cloudinary_cloud_name"
 */
const VAULTIQ_CONFIG = {
    // Application Name (Update this to change the app name everywhere)
    appName: "VAULTIQ",
    
    // Supabase Configuration - Read from environment variables
    supabaseUrl: process.env.SUPABASE_URL || "YOUR_SUPABASE_URL",
    supabaseAnonKey: process.env.SUPABASE_ANON_KEY || "YOUR_SUPABASE_ANON_KEY",
    supabasePublishableKey: process.env.SUPABASE_PUBLISHABLE_KEY || "YOUR_SUPABASE_PUBLISHABLE_KEY",

    // Cloudinary Configuration - Read from environment variables
    cloudinaryCloudName: process.env.CLOUDINARY_CLOUD_NAME || "YOUR_CLOUDINARY_CLOUD_NAME",
    
    // Language Settings
    defaultLanguage: "en", // "en" or "bn"
    
    // Feature Flags
    features: {
        enableRealtime: true,
        enableNotifications: true,
        enableGeofencing: true
    }
};