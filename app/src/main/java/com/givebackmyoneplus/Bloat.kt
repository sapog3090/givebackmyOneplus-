package com.givebackmyoneplus

data class BloatApp(val pkg: String, val title: String, val note: String)

/** Безопасные для удаления (для пользователя) приложения HyperOS Global. */
object Bloat {
    val list = listOf(
        BloatApp("com.miui.msa.global", "Системные службы рекламы (MSA)", "Главный источник рекламы в системе"),
        BloatApp("com.miui.analytics.go", "Аналитика Xiaomi", "Сбор статистики"),
        BloatApp("com.miui.analytics", "Аналитика Xiaomi", "Сбор статистики"),
        BloatApp("com.miui.android.fashiongallery", "Карусель обоев", "Реклама на экране блокировки"),
        BloatApp("com.xiaomi.mipicks", "GetApps", "Магазин Xiaomi с рекламой и уведомлениями"),
        BloatApp("com.mi.globalbrowser", "Mi Браузер", "Браузер с рекламной лентой"),
        BloatApp("com.xiaomi.glgm", "Игры Xiaomi", "Каталог игр"),
        BloatApp("com.miui.videoplayer", "Mi Видео", "Видеоплеер с рекомендациями"),
        BloatApp("com.miui.player", "Mi Музыка", "Музыкальный плеер с рекомендациями"),
        BloatApp("com.miui.yellowpage", "Жёлтые страницы", "Справочник организаций"),
        BloatApp("com.miui.bugreport", "Отзывы", "Отправка отчётов в Xiaomi"),
        BloatApp("com.miui.hybrid", "Быстрые приложения", "Мини-приложения и реклама в них"),
        BloatApp("com.facebook.appmanager", "Facebook App Manager", "Предустановка Facebook"),
        BloatApp("com.facebook.services", "Facebook Services", "Предустановка Facebook"),
        BloatApp("com.facebook.system", "Facebook App Installer", "Предустановка Facebook"),
        BloatApp("com.netflix.partner.activation", "Netflix активация", "Партнёрская предустановка"),
        BloatApp("com.amazon.appmanager", "Amazon App Manager", "Партнёрская предустановка"),
    )
}
