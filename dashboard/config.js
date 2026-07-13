/**
 * VAULTIQ Dashboard Configuration
 * Centralized configuration file - update this to change app-wide settings
 */
const VAULTIQ_CONFIG = {
    // Application Name (Update this to change the app name everywhere)
    appName: "VAULTIQ",
    
    // Supabase Configuration
    supabaseUrl: "https://orrdsscvzaginlvmbyow.supabase.co",
    supabaseAnonKey: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im9ycmRzc2N2emFnaW5sdm1ieW93Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE4NDAyNDgsImV4cCI6MjA5NzQxNjI0OH0.DAxBhVIbTewGs_GiR8cmKpo-zT1eOHwG6W7HVeQzZVU",
    supabasePublishableKey: "sb_publishable_rCKateOLNeQ-SJekTUnjFA_glCHfolw",

    // Cloudinary Configuration
    cloudinaryCloudName: "dzd8hoo8e",
    
    // Language Settings
    defaultLanguage: "en", // "en" or "bn"
    
    // Feature Flags
    features: {
        enableRealtime: true,
        enableNotifications: true,
        enableGeofencing: true
    }
};