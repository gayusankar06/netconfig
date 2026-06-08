package com.enterprise.netconfigdiff.utils

object Constants {
    // For emulator: use 10.0.2.2 (maps to host machine localhost)
    // For real device on same WiFi: use your PC's LAN IP (e.g. 192.168.1.100)
    const val BASE_URL = "http://10.0.2.2:80/" // via Nginx proxy
    const val OLLAMA_URL = "http://10.0.2.2:11434/" // direct Ollama (for testing)
    const val WEBSOCKET_URL = "ws://10.0.2.2:8000/ws"
    const val DEFAULT_TIMEOUT_SECONDS = 60L
    const val UPLOAD_TIMEOUT_SECONDS = 300L
    const val ANALYSIS_POLL_INTERVAL_MS = 2000L
    const val MAX_FILE_SIZE_MB = 50
}
