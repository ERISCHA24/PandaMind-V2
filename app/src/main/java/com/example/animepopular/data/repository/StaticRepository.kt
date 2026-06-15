package com.example.animepopular.data.repository

import com.example.animepopular.model.NewsItem
import com.example.animepopular.model.ScheduleItem

object NewsRepository {
    fun getNewsList() = listOf(
        NewsItem(
            id = "1",
            titleEn = "Attack on Titan: New Season Confirmed",
            titleId = "Season Baru Attack on Titan",
            contentEn = "MAPPA confirms the final season of Attack on Titan will release this autumn.",
            contentId = "Konfirmasi season final akan tayang musim gugur ini.",
            updateEn = "2 hours ago", updateId = "2 jam lalu"
        ),
        NewsItem(
            id = "2",
            titleEn = "Demon Slayer Movie Coming Soon",
            titleId = "Film Demon Slayer Diumumkan",
            contentEn = "ufotable announces a brand new Demon Slayer film releasing next year.",
            contentId = "ufotable mengumumkan film layar lebar baru yang akan tayang tahun depan.",
            updateEn = "5 hours ago", updateId = "5 jam lalu"
        ),
        NewsItem(
            id = "3",
            titleEn = "Spy x Family Season 3 Announced",
            titleId = "Spy x Family Season 3 akan tayang",
            contentEn = "Wit Studio confirms production of Spy x Family Season 3 has begun.",
            contentId = "Wit Studio mengonfirmasi produksi season 3 sudah dimulai.",
            updateEn = "1 day ago", updateId = "1 hari lalu"
        ),
        NewsItem(
            id = "4",
            titleEn = "Manga Awards 2024 Results",
            titleId = "Penghargaan Manga 2024",
            contentEn = "Berserk wins Manga of the Year for the third time in a row.",
            contentId = "Berserk memenangkan Manga of the Year untuk ketiga kalinya.",
            updateEn = "2 days ago", updateId = "2 hari lalu"
        ),
        NewsItem(
            id = "5",
            titleEn = "One Piece Live Action Season 2",
            titleId = "One Piece Live Action Season 2",
            contentEn = "Netflix officially announces Season 2 of One Piece live-action series.",
            contentId = "Netflix resmi mengumumkan Season 2 serial live-action One Piece.",
            updateEn = "3 days ago", updateId = "3 hari lalu"
        )
    )
}

object ScheduleRepository {
    fun getScheduleList() = listOf(
        ScheduleItem(
            dayEn = "Monday", dayId = "Senin",
            animeListEn = "• One Piece (22:00)\n• Boruto (20:00)",
            animeListId = "• One Piece (22:00)\n• Boruto (20:00)"
        ),
        ScheduleItem(
            dayEn = "Tuesday", dayId = "Selasa",
            animeListEn = "• Demon Slayer (22:30)\n• Hunter x Hunter (21:00)",
            animeListId = "• Pembasmi Iblis (22:30)\n• Hunter x Hunter (21:00)"
        ),
        ScheduleItem(
            dayEn = "Wednesday", dayId = "Rabu",
            animeListEn = "• Jujutsu Kaisen (21:00)\n• Chainsaw Man (22:00)",
            animeListId = "• Jujutsu Kaisen (21:00)\n• Chainsaw Man (22:00)"
        ),
        ScheduleItem(
            dayEn = "Thursday", dayId = "Kamis",
            animeListEn = "• Attack on Titan (22:00)\n• Blue Lock (21:00)",
            animeListId = "• Serangan Titan (22:00)\n• Blue Lock (21:00)"
        ),
        ScheduleItem(
            dayEn = "Friday", dayId = "Jumat",
            animeListEn = "• Spy x Family (20:00)\n• Tokyo Revengers (21:30)",
            animeListId = "• Spy x Family (20:00)\n• Tokyo Revengers (21:30)"
        ),
        ScheduleItem(
            dayEn = "Saturday", dayId = "Sabtu",
            animeListEn = "• My Hero Academia (21:00)\n• Black Clover (22:00)",
            animeListId = "• My Hero Academia (21:00)\n• Black Clover (22:00)"
        ),
        ScheduleItem(
            dayEn = "Sunday", dayId = "Minggu",
            animeListEn = "• Fairy Tail (20:00)\n• Vinland Saga (21:30)\n• Overlord (23:00)",
            animeListId = "• Fairy Tail (20:00)\n• Vinland Saga (21:30)\n• Overlord (23:00)"
        )
    )
}