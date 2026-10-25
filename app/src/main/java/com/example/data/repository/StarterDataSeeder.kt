package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.model.Chapter
import com.example.data.model.Novel
import com.example.data.model.UserProfile
import com.example.data.scraper.JsoupScraperEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object StarterDataSeeder {

    suspend fun seedDefaultsIfEmpty(database: AppDatabase) = withContext(Dispatchers.IO) {
        val novelDao = database.novelDao()
        val chapterDao = database.chapterDao()
        val extensionDao = database.extensionDao()
        val profileDao = database.userProfileDao()

        // 1. Extensions & Catalog
        val existingExt = extensionDao.getExtensionById("ext_royalroad")
        if (existingExt == null) {
            val catalog = com.example.data.scraper.CommunityRepoClient().getCatalogExtensions()
            extensionDao.insertExtensions(catalog)
        }

        val repositoryDao = database.repositoryDao()
        val defaultClient = com.example.data.scraper.CommunityRepoClient()
        val existingRepo = repositoryDao.getRepositoryById("repo_official")
        if (existingRepo == null) {
            repositoryDao.insertRepositories(defaultClient.getDefaultRepositories())
        }

        // 2. Profile
        val existingProfile = profileDao.getProfile()
        if (existingProfile == null) {
            profileDao.insertOrUpdateProfile(UserProfile())
        }

        // 3. Sample Novels in Library
        val existingNovel = novelDao.getNovelById("novel_arcane_sovereign")
        if (existingNovel == null) {
            val novels = listOf(
                Novel(
                    id = "novel_arcane_sovereign",
                    title = "The Arcane Sovereign: Rebirth of the Void",
                    author = "Aura Weaver",
                    coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&q=80",
                    description = "When the ancient Celestial seals shattered over the capital of Lumina, Lin Chen awakened with memories of an erased timeline. Armed with the forbidden Void Codex, he steps forward to cultivate anew and rewrite fate itself.",
                    genres = "Fantasy, Cultivation, Reincarnation, Action",
                    status = "Ongoing",
                    sourceId = "local_featured",
                    sourceUrl = "https://auranovel.internal/novels/1",
                    inLibrary = true,
                    totalChapters = 4,
                    userRating = 4.9f,
                    isFavorite = true,
                    totalWords = 5200L
                ),
                Novel(
                    id = "novel_midnight_alchemist",
                    title = "The Midnight Alchemist & The Clockwork Tower",
                    author = "Elian Thorne",
                    coverUrl = "https://images.unsplash.com/photo-1532012164546-f432f2e3777a?w=500&q=80",
                    description = "In the foggy cobblestone district of New Valen, young apprentice Julian accidentally synthesizes an elixir that distorts the flow of time. Pursued by the Inquisitors of the Iron Gear, his only refuge is the legendary submerged archives.",
                    genres = "Steampunk, Mystery, Magic, Adventure",
                    status = "Ongoing",
                    sourceId = "local_featured",
                    sourceUrl = "https://auranovel.internal/novels/2",
                    inLibrary = true,
                    totalChapters = 3,
                    userRating = 4.8f,
                    isFavorite = false,
                    totalWords = 3800L
                ),
                Novel(
                    id = "novel_alice_arcane",
                    title = "Alice in the Arcane Realm: Infinite Labyrinth",
                    author = "Lewis Carroll (Remastered Edition)",
                    coverUrl = "https://images.unsplash.com/photo-1476275466078-4007374efbbe?w=500&q=80",
                    description = "Tumbling through the roots of an ancient mana-infused rabbit hole, Alice discovers Wonderland is governed by strict dungeon mechanics, status windows, and perilous trial gates.",
                    genres = "Isekai, Fantasy, LitRPG, Classic",
                    status = "Completed",
                    sourceId = "local_featured",
                    sourceUrl = "https://auranovel.internal/novels/3",
                    inLibrary = true,
                    totalChapters = 3,
                    userRating = 4.7f,
                    isFavorite = false,
                    totalWords = 4100L
                )
            )
            novelDao.insertNovels(novels)

            // Chapters for Novel 1
            val ch1Novel1 = Chapter(
                id = "novel_arcane_sovereign_ch_1",
                novelId = "novel_arcane_sovereign",
                chapterNumber = 1f,
                title = "Chapter 1: The Shattered Starlight Codex",
                url = "https://auranovel.internal/novels/1/ch1",
                content = """The dawn over the floating spires of Mount Kunlun was not gold, but a bruised, tempestuous violet.

Lin Chen stood at the precipice of the Cloud Gazing Terrace, his silver-trimmed robes billowing violently against the howling high-altitude gales. In his palms rested fragments of the Starlight Codex, cold as glacial frost yet humming with residual thunder.

"Three hundred years," he murmured, his voice cutting cleanly through the wind. "The Heavenly Mandate thought they had erased every spark of the Nether Fire."

Below him, the nine outer courtyards were already stirring. Disciples in azure tunics were assembling for morning sword practice, their synchronized battle cries echoing across the ravines. None of them knew that within three cycles of the moon, the barrier sealing the Abyssal Rift would crumble to dust.

Lin Chen closed his eyes and sank his consciousness into his Dantian core. Where there should have been a fragile Qi condensation whorl, an ominous black singularity was quietly rotating. It was the Void Seed—the solitary prize he had dragged back across the threshold of reincarnation.

"If the heavens wish to bind me with destiny," Lin Chen said softly, clenching his fist as the code fragments dissolved into smoke, "then I will forge a spear that pierces the canopy of the stars."

He turned on his heel and stepped into the shadows of the courtyard, ready to begin his climb back to sovereignty.""",
                wordCount = 380,
                isDownloaded = true
            )

            val ch2Novel1 = Chapter(
                id = "novel_arcane_sovereign_ch_2",
                novelId = "novel_arcane_sovereign",
                chapterNumber = 2f,
                title = "Chapter 2: The Spirit Beast Pavilion",
                url = "https://auranovel.internal/novels/1/ch2",
                content = """The Spirit Beast Pavilion was always shrouded in pungent mist, smelling of crushed star-leaf herbs and feral musk.

As Lin Chen entered the courtyard, iron-reinforced cages hummed with suppression arrays. Within them, scaled pythons, thunder-winged falcons, and four-eyed wolves paced restlessly.

"Well, well. If it isn't our proud Lin Chen," a sneering voice called out from the registry platform.

Elder Song, in charge of allocating bonded beasts, looked down with disdain. "Three days late for selection. The third-tier and second-tier spirit beasts have all been claimed by the core disciples. What remains in the lower ditch are sick lizards and unhatched stone eggs."

Lin Chen did not respond to the provocation. Instead, his divine sense rippled invisibly outward.

Most of the beasts shrank back, instinctively sensing something ancient and predatory concealed beneath his mortal aura. But at the furthest corner of the ditch, beneath a damp iron grate, a faint golden resonance answered his pulse.

It was an unpolished, soot-stained tortoise egg—no, not a tortoise. A Draconic Qilin embryo that had been mislabeled as slag!

"I will take that one," Lin Chen pointed calmly.

Elder Song burst into mocking laughter. "A dead stone? Take it! Do not come crying when it fails to hatch before the spring solstice exam!"

Lin Chen scooped the warm stone into his sleeves, a faint smile gracing his lips. "When it hatches, Elder, you will bow before its roar." """,
                wordCount = 350,
                isDownloaded = true
            )

            val ch3Novel1 = Chapter(
                id = "novel_arcane_sovereign_ch_3",
                novelId = "novel_arcane_sovereign",
                chapterNumber = 3f,
                title = "Chapter 3: The Crimson Moon Trial",
                url = "https://auranovel.internal/novels/1/ch3",
                content = """Night settled over the Forbidden Canyon like an ink spill.

A blood-red crescent hung suspended in the northern sky. The Crimson Moon Trial was Mount Kunlun’s most unforgiving rite of passage: seven days in the monster-infested wilderness, with survival as the only passing grade.

Lin Chen vaulted across the jagged limestone ridges, his footsteps making less sound than a falling leaf. In his left hand, the newly awakened Qilin beast, now a tiny golden-furred cub with crystalline horns, rested comfortably upon his shoulder, sniffing the wind for hidden spiritual herbs.

"Squeak!" the cub chirped, batting its paw toward a sunken grotto beneath an overhanging banyan tree.

"Good eyes, Little Gold," Lin Chen whispered.

Deep within the grotto, a cluster of Nine-Leaf Frost Lotus flowers bloomed in the dark, bathed in luminescence. Guarding them, however, was a twin-headed Viper King, its crimson scales radiating waves of corrosive miasma.

Lin Chen's eyes narrowed into slits. He drew the rusted iron sword strapped to his back.

"A mid-stage Foundation Establishment beast. In my previous life, a flick of my finger would have atomized it. Today, it will serve as the whetstone for my Void Sword intent!"

With a resounding crack, Lin Chen surged forward, the air screeching as dark spatial fissures bloomed along the blade's edge.""",
                wordCount = 340,
                isDownloaded = true
            )

            val ch4Novel1 = Chapter(
                id = "novel_arcane_sovereign_ch_4",
                novelId = "novel_arcane_sovereign",
                chapterNumber = 4f,
                title = "Chapter 4: Sovereignty Reclaimed",
                url = "https://auranovel.internal/novels/1/ch4",
                content = """The twin heads of the Viper King thudded onto the limestone cavern floor, black venom hissing where it splashed upon the stones.

Lin Chen exhaled a breath of turbid white vapor. Around him, the spiritual energy of the cave swirled into a vortex, rushing into his meridian points like a tidal wave.

His bones chimed like bronze bells.

Crack!

The barrier of the Fourth Layer Qi condensation shattered effortlessly. The Void Seed inside his core expanded, sending waves of pure astral power coursing through his limbs.

Little Gold scampered over to the Frost Lotus, carefully plucking the central petal with its snout and offering it proudly to Lin Chen.

"With this lotus and the viper core, my Foundation Establishment will be flawless," Lin Chen smiled, patting the cub's head.

Far in the distance, a thunderous war horn echoed across the canyon. The elder overseers had noticed the anomaly. Lin Chen sheathed his blade and looked toward the eastern horizon where dawn was breaking.

"The hunt has only just begun." """,
                wordCount = 280,
                isDownloaded = true
            )

            chapterDao.insertChapters(listOf(ch1Novel1, ch2Novel1, ch3Novel1, ch4Novel1))

            // Chapters for Novel 2
            val ch1Novel2 = Chapter(
                id = "novel_midnight_alchemist_ch_1",
                novelId = "novel_midnight_alchemist",
                chapterNumber = 1f,
                title = "Chapter 1: The Silver Distillate",
                url = "https://auranovel.internal/novels/2/ch1",
                content = """Gas lamps flickered against the damp brickwork of Foundry Lane.

Julian adjusted his brass-rimmed goggles as amber vapor spiraled through the serpentine copper condenser. It was 3:17 in the morning, the exact hour when the atmospheric ether density of New Valen peaked.

"Add three drops of mercury sulfide," he muttered to himself, checking his leather-bound journal. "Followed by powdered chimera fang."

The cauldron chimed with a musical frequency. Suddenly, the amber vapor collapsed into an iridescent silver sphere that hovered motionless three inches above the burner.

Outside the garret window, a church bell began to toll. But as the chime reverberated, Julian noticed the water droplets dripping from his leaking roof had stopped dead in midair, suspended like polished glass beads.

He reached forward with a trembling gloved finger and touched the silver distillate.

Instantly, his perception dilated. He could see the dust motes frozen in space, the ticking clock whose second hand was locked halfway between seconds, and far down the alleyway, the sinister silhouettes of three Inquisitor trench coats frozen mid-stride.

He had created Chrono-Ether.""",
                wordCount = 310,
                isDownloaded = true
            )
            chapterDao.insertChapters(listOf(ch1Novel2))
        }
    }
}
